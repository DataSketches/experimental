/*
 * Copyright 2015, Yahoo! Inc.
 * Licensed under the terms of the Apache License 2.0. See LICENSE file at the project root for terms.
 */
package com.yahoo.sketches.quantiles;

import static com.yahoo.sketches.Util.ceilingPowerOf2;
import static com.yahoo.sketches.quantiles.Util.*;
import static com.yahoo.sketches.quantiles.PreambleUtil.*;

import com.yahoo.sketches.Family;
import com.yahoo.sketches.memory.Memory;
import com.yahoo.sketches.memory.NativeMemory;
import java.util.Arrays;

/**
 * This is an implementation of the low-discrepancy mergeable quantile sketch using double 
 * valued elements. This is an implementation of the Low Discrepancy Mergeable Quantiles Sketch 
 * described in section 3.2 of the journal version of the paper "Mergeable Summaries" 
 * by Agarwal, Cormode, Huang, Phillips, Wei, and Yi. 
 * <a href="http://dblp.org/rec/html/journals/tods/AgarwalCHPWY13"></a>
 * 
 * <p>This algorithm intentionally inserts randomness into the sampling process for values that
 * ultimately get retained in the sketch. The result is that this algorithm is not 
 * deterministic. I.e., if the same stream is inserted into two different instances of this sketch, 
 * the answers obtained from the two sketches may not be be identical.</p>
 * 
 * <p>Similarly, there may be directional inconsistencies. For example, the resulting array of values
 * obtained from getQuantiles(fractions[]) input into the reverse directional query 
 * getPDF(splitPoints[]) may not result in the original fractional values.</p>
 * 
 * @author Kevin Lang
 * @author Lee Rhodes
 */
class HeapQuantilesSketch extends QuantilesSketch {
  /**
   * Parameter that controls space usage of sketch and accuracy of estimates.
   */
  private final int k_; //could be a short (max 32K)

  /**
   * Total number of data items in the stream so far. (Uniqueness plays no role in these sketches).
   */
  private long n_;

  /**
   * The smallest value ever seen in the stream.
   */
  private double minValue_;

  /**
   * The largest value ever seen in the stream.
   */
  private double maxValue_;

  /**
   * Number of samples currently in base buffer.
   * 
   * Count = N % (2*K)
   */
  private int baseBufferCount_; 

  /**
   * Active levels expressed as a bit pattern.
   * 
   * Pattern = N / (2 * K)
   */
  private long bitPattern_;

  /**
   * In the initial on-heap version, equals combinedBuffer_.length.
   * May differ in later versions that grow space more aggressively.
   * Also, in the off-heap version, combinedBuffer_ won't even be a java array,
   * so it won't know its own length.
   */
  private int combinedBufferAllocatedCount_;

  /**
   * This single array contains the base buffer plus all levels some of which are not used.
   * A level is of size K and is either full and sorted, or not used. A "not used" buffer may have
   * garbage. Whether a level buffer used or not is indicated by the bitPattern_.
   * The base buffer has length 2*K but might not be full and isn't necessarily sorted.
   * The base buffer precedes the level buffers. 
   * 
   * It requires quite a bit of explanation, which we defer until later.
   */
  private double[] combinedBuffer_;

  //**CONSTRUCTORS**********************************************************
  
  private HeapQuantilesSketch(int k) {
    super();
    checkK(k);
    k_ = k;
  }
  
  /**
   * Obtains an instance of a QuantileSketch of double elements.
   * 
   * @param k Parameter that controls space usage of sketch and accuracy of estimates. 
   * Must be greater than one.
   */
  static HeapQuantilesSketch getInstance(int k) {
    
    HeapQuantilesSketch hqs = new HeapQuantilesSketch(k);
    int bufAlloc = Math.min(MIN_BASE_BUF_SIZE, 2*k); //the min is important
    hqs.n_ = 0;
    hqs.combinedBufferAllocatedCount_ = bufAlloc;
    hqs.combinedBuffer_ = new double[bufAlloc];
    hqs.baseBufferCount_ = 0;
    hqs.bitPattern_ = 0;
    hqs.minValue_ = java.lang.Double.POSITIVE_INFINITY;
    hqs.maxValue_ = java.lang.Double.NEGATIVE_INFINITY;
    return hqs;
  }
  
  /**
   * Heapifies the given srcMem, which must be a Memory image of a QuantilesSketch
   * @param srcMem
   * @return a QuantilesSketch on the Java heap.
   */
  static HeapQuantilesSketch getInstance(Memory srcMem) {
    long memCapBytes = srcMem.getCapacity();
    if (memCapBytes < 8) {
      throw new IllegalArgumentException("Memory too small: "+memCapBytes);
    }
    long pre0 = srcMem.getLong(0);
    int preambleLongs = extractPreLongs(pre0);
    int serVer = extractSerVer(pre0);
    int familyID = extractFamilyID(pre0);
    int flags = extractFlags(pre0);
    boolean empty = (flags & EMPTY_FLAG_MASK) > 0;
    int k = extractK(pre0);
    
    Family family = Family.idToFamily(familyID);
    if (!family.equals(Family.QUANTILES)) {
      throw new IllegalArgumentException(
          "Possible corruption: Invalid Family: " + family.toString());
    }
    boolean valid = ((preambleLongs == 1) && empty) || ((preambleLongs == 5) && !empty);
    if (!valid) {
      throw new IllegalArgumentException(
          "Possible corruption: PreambleLongs inconsistent with empty state: " +preambleLongs);
    }
    
    if (serVer != SER_VER) {
      throw new IllegalArgumentException(
          "Possible corruption: Invalid Serialization Version: "+serVer);
    }
    
    int flagsMask = ORDERED_FLAG_MASK | COMPACT_FLAG_MASK | READ_ONLY_FLAG_MASK | BIG_ENDIAN_FLAG_MASK;
    if ((flags & flagsMask) > 0) {
      throw new IllegalArgumentException(
          "Possible corruption: Input srcMem cannot be: big-endian, compact, ordered, or read-only");
    }
    
    if (empty) return new HeapQuantilesSketch(k);
    
    if (memCapBytes < 40) {
      throw new IllegalArgumentException("Memory too small: "+memCapBytes);
    }
    long[] preArr = new long[5];
    srcMem.getLongArray(0, preArr, 0, 5);
    long n = preArr[1];
    double minValue = Double.longBitsToDouble(preArr[2]);
    double maxValue = Double.longBitsToDouble(preArr[3]);
    int bufAlloc = (int) preArr[4];
    
    int bufBytes = bufferElementCapacity(k, n) << 3;
    if (memCapBytes < (40 + bufBytes)) {
      throw new IllegalArgumentException("Memory too small: "+memCapBytes);
    }
    
    HeapQuantilesSketch hqs = new HeapQuantilesSketch(k);
    hqs.n_ = n;
    hqs.minValue_ = minValue;
    hqs.maxValue_ = maxValue;
    hqs.baseBufferCount_ = (int)(n % (2*k));
    hqs.bitPattern_ = n / (2 * k);
    hqs.combinedBufferAllocatedCount_ = bufAlloc;
    hqs.combinedBuffer_ = new double[bufAlloc];
    srcMem.getDoubleArray(40, hqs.combinedBuffer_, 0, bufAlloc);
    return hqs;
  }
  
  /**
   * Returns a new builder
   *
   * @return a new builder
   */
  public static final QuantilesSketchBuilder builder() {
    return new QuantilesSketchBuilder();
  }



  @Override
  public void update(double dataItem) {
    // this method is only directly using the base buffer part of the combined buffer
    if (Double.isNaN(dataItem)) return;

    if (dataItem > maxValue_) { maxValue_ = dataItem; }   // benchmarks faster than Math.max()
    if (dataItem < minValue_) { minValue_ = dataItem; }

    if (baseBufferCount_+1 > combinedBufferAllocatedCount_) {
      growBaseBuffer();
    } 
    combinedBuffer_[baseBufferCount_++] = dataItem;
    n_++;
    if (baseBufferCount_ == 2*k_) {
      processFullBaseBuffer();
    }
  }

  @Override
  public double getQuantile(double fraction) {
    if ((fraction < 0.0) || (fraction > 1.0)) {
      throw new IllegalArgumentException("Fraction cannot be less than zero or greater than 1.0");
    }
    if      (fraction == 0.0) { return minValue_; }
    else if (fraction == 1.0) { return maxValue_; }
    else {
      Auxiliary aux = this.constructAuxiliary();
      return aux.getQuantile(fraction);
    }
  }


  @Override
  public double[] getQuantiles(double[] fractions) {
    Auxiliary aux = null; //
    double[] answers = new double[fractions.length];
    for (int i = 0; i < fractions.length; i++) {
      double fraction = fractions[i];
      if ((fraction < 0.0) || (fraction > 1.0)) {
        throw new IllegalArgumentException("Fraction cannot be less than zero or greater than 1.0");
      }
      if      (fraction == 0.0) { answers[i] = minValue_; }
      else if (fraction == 1.0) { answers[i] = maxValue_; }
      else {
        if (aux == null) aux = this.constructAuxiliary();
        answers[i] = aux.getQuantile(fraction);
      }
    }
    return answers;
  }

  @Override
  public double[] getPDF(double[] splitPoints) {
    long[] counters = internalBuildHistogram(splitPoints);
    int numCounters = counters.length;
    double[] result = new double[numCounters];
    double n = n_;
    long subtotal = 0;
    for (int j = 0; j < numCounters; j++) { 
      long count = counters[j];
      subtotal += count;
      result[j] = count / n; //normalize by n
    }
    assert subtotal == n; //internal consistency check
    return result;
  }

  @Override
  public double[] getCDF(double[] splitPoints) {
    long[] counters = internalBuildHistogram(splitPoints);
    int numCounters = counters.length;
    double[] result = new double[numCounters];
    double n = n_;
    long subtotal = 0;
    for (int j = 0; j < numCounters; j++) { 
      long count = counters[j];
      subtotal += count;
      result[j] = subtotal / n; //normalize by n
    }
    assert subtotal == n; //internal consistency check
    return result;
  }

  @Override
  public double getNormalizedRankError() {
    return Util.EpsilonFromK.getAdjustedEpsilon(k_);
  }

  @Override
  public long getStreamLength() {
    return n_;
  }

  @Override
  public int getK() { 
    return k_; 
  }

  @Override
  public double getMinValue() {
    return minValue_;
  }

  @Override
  public double getMaxValue() {
    return maxValue_;
  }

  @Override
  public boolean isDirect() {
    return false;
  }
  
  @Override
  public boolean isEmpty() {
    return (n_ == 0);
  }
  
  @Override
  public void reset() {
    n_ = 0;
    combinedBufferAllocatedCount_ = Math.min(MIN_BASE_BUF_SIZE,2*k_); //the min is important
    combinedBuffer_ = new double[combinedBufferAllocatedCount_];
    baseBufferCount_ = 0;
    bitPattern_ = 0;
    minValue_ = java.lang.Double.POSITIVE_INFINITY;
    maxValue_ = java.lang.Double.NEGATIVE_INFINITY;
  }
  
  @Override
  public byte[] toByteArray() {
    int preLongs, arrLongs, flags;
    boolean empty = isEmpty();
    
    if (empty) {
      preLongs = 1;
      arrLongs = 1;
      flags = EMPTY_FLAG_MASK;
    }
    else {
      preLongs = 5;
      arrLongs = preLongs + combinedBuffer_.length;
      flags = 0;
    }
    byte[] outArr = new byte[arrLongs << 3];
    NativeMemory mem = new NativeMemory(outArr);
    
    //build first prelong
    long pre0 = 0L;
    pre0 = insertPreLongs(preLongs, pre0);
    pre0 = insertSerVer(SER_VER, pre0);
    pre0 = insertFamilyID(Family.QUANTILES.getID(), pre0);
    //other flags: bigEndian = readOnly = compact = ordered = false
    pre0 = insertFlags(flags, pre0);
    pre0 = insertK(k_, pre0);
    
    if (empty) {
      mem.putLong(0, pre0);
      //return outArr
    }
    else {
      long[] preArr = new long[5];
      preArr[0] = pre0;
      preArr[1] = n_;
      preArr[2] = Double.doubleToLongBits(minValue_);
      preArr[3] = Double.doubleToLongBits(maxValue_);
      preArr[4] = combinedBufferAllocatedCount_;
      mem.putLongArray(0, preArr, 0, 5);
      mem.putDoubleArray(40, combinedBuffer_, 0, combinedBuffer_.length);
    }
    return outArr;
  }
  
  @Override
  public String toString() {
    return toString(true, false);
  }
  
  @Override
  public String toString(boolean sketchSummary, boolean dataDetail) {
    StringBuilder sb = new StringBuilder();
    String thisSimpleName = this.getClass().getSimpleName();
    
    if (dataDetail) {
      sb.append(LS).append("### ").append(thisSimpleName).append(" DATA DETAIL: ").append(LS);
      double[] levelsArr  = combinedBuffer_;
      double[] baseBuffer = combinedBuffer_;
      
      //output the base buffer
      sb.append("   BaseBuffer   : ");
      if (baseBufferCount_ > 0) {
        for (int i = 0; i < baseBufferCount_; i++) { 
          sb.append(String.format("%10.1f", baseBuffer[i]));
        }
      }
      sb.append(LS);
      //output all the levels
      
      int items = combinedBufferAllocatedCount_;
      if (items > 2*k_) {
        sb.append("   Valid | Level");
        for (int j = 2*k_; j < items; j++) { //output level data starting at 2K
          if (j % k_ == 0) { //start output of new level
            int levelNum = (j > 2*k_)? ((j-2*k_)/k_): 0;
            String validLvl = (((1L << levelNum) & bitPattern_) > 0L)? "    T  " : "    F  "; 
            String lvl = String.format("%5d",levelNum);
            sb.append(LS).append("   ").append(validLvl).append(" ").append(lvl).append(": ");
          }
          sb.append(String.format("%10.1f", levelsArr[j]));
        }
        sb.append(LS);
      }
      sb.append("### END DATA DETAIL").append(LS);
    }
    
    if (sketchSummary) {
      long n = getN();
      String nStr = String.format("%,d", n);
      int numLevels = computeNumLevelsNeeded(k_, n_);
      int bufBytes = combinedBufferAllocatedCount_ * 8;
      String bufCntStr = String.format("%,d", combinedBufferAllocatedCount_);
      //includes k, n, min, max, preamble of 8.
      int preBytes = 4 + 8 + 8 + 8 + 8;
      double eps = Util.EpsilonFromK.getAdjustedEpsilon(k_);
      String epsPct = String.format("%.3f%%", eps * 100.0);
      int numSamples = numValidSamples();
      String numSampStr = String.format("%,d", numSamples);
      sb.append(LS).append("### ").append(thisSimpleName).append(" SUMMARY: ").append(LS);
      sb.append("   K                            : ").append(getK()).append(LS);
      sb.append("   N                            : ").append(nStr).append(LS);
      sb.append("   BaseBufferCount              : ").append(getBaseBufferCount()).append(LS);
      sb.append("   CombinedBufferAllocatedCount : ").append(bufCntStr).append(LS);
      sb.append("   Total Levels                 : ").append(numLevels).append(LS);
      sb.append("   Valid Levels                 : ").append(Util.numValidLevels(bitPattern_)).append(LS);
      sb.append("   Level Bit Pattern            : ").append(Long.toBinaryString(bitPattern_)).append(LS);
      sb.append("   Valid Samples                : ").append(numSampStr).append(LS);
      sb.append("   Buffer Storage Bytes         : ").append(String.format("%,d", bufBytes)).append(LS);
      sb.append("   Preamble Bytes               : ").append(preBytes).append(LS);
      sb.append("   Normalized Rank Error        : ").append(epsPct).append(LS);
      sb.append("   Min Value                    : ").append(String.format("%,.3f", getMinValue())).append(LS);
      sb.append("   Max Value                    : ").append(String.format("%,.3f", getMaxValue())).append(LS);
      sb.append("### END SKETCH SUMMARY").append(LS);
    }
    return sb.toString();
  }

  /**
   * Merges the given sketch into this one
   * @param qsSource the given source sketch
   */
  public void merge(HeapQuantilesSketch qsSource) {
    mergeInto(qsSource, this);
  }

  // It is easy to prove that the following simplified code which launches 
  // multiple waves of carry propagation does exactly the same amount of merging work
  // (including the work of allocating fresh buffers) as the more complicated and 
  // seemingly more efficient approach that tracks a single carry propagation wave
  // through both sketches.

  // This simplified code probably does do slightly more "outer loop" work,
  // but I am pretty sure that even that is within a constant factor
  // of the more complicated code, plus the total amount of "outer loop"
  // work is at least a factor of K smaller than the total amount of 
  // merging work, which is identical in the two approaches.

  // Note: a two-way merge that doesn't modify either of its
  // two inputs could be implemented by making a deep copy of
  // the larger sketch and then merging the smaller one into it.
  // However, it was decided not to do this.

  
 /**
  * Modifies the source sketch into the target sketch.
  * @param qsSource The source sketch
  * @param qsTarget The target sketch
  */
  public static void mergeInto(HeapQuantilesSketch qsSource, HeapQuantilesSketch qsTarget) {
    if ( qsTarget.k_ != qsSource.k_) 
      throw new IllegalArgumentException("Given sketches must have the same value of k.");

    double[] srcLevels     = qsSource.combinedBuffer_; // aliasing is a bit dangerous
    double[] srcBaseBuffer = qsSource.combinedBuffer_; // aliasing is a bit dangerous

    int tgtK = qsTarget.getK();
    long nFinal = qsTarget.getN() + qsSource.getN();

    for (int i = 0; i < qsSource.baseBufferCount_; i++) {
      qsTarget.update(srcBaseBuffer[i]);
    }

    qsTarget.maybeGrowLevels(nFinal); 

    double[] scratchBuf = new double[2*tgtK];

    long srcBits = qsSource.bitPattern_;
    assert srcBits == qsSource.getN() / (2L * qsSource.getK());
    for (int srcLvl = 0; srcBits != 0L; srcLvl++, srcBits >>>= 1) {
      if ((srcBits & 1L) > 0L) {
        qsTarget.inPlacePropagateCarry(srcLvl,
                                   srcLevels, ((2+srcLvl) * tgtK),
                                   scratchBuf, 0,
                                   false);
        // won't update qsTarget.n_ until the very end
      }
    }

    qsTarget.n_ = nFinal;
    
    assert qsTarget.getN() / (2*tgtK) == qsTarget.bitPattern_; // internal consistency check

    if (qsSource.maxValue_ > qsTarget.maxValue_) { qsTarget.maxValue_ = qsSource.maxValue_; }
    if (qsSource.minValue_ < qsTarget.minValue_) { qsTarget.minValue_ = qsSource.minValue_; }
  }

  
  //Restricted

  /**
   * Returns the Auxiliary data structure which is only used for getQuantile() and getQuantiles() 
   * queries.
   * @return the Auxiliary data structure
   */
  Auxiliary constructAuxiliary() {
    return new Auxiliary( this );
        //k_, n_, bitPattern_, combinedBuffer_, baseBufferCount_, numSamplesInSketch());
  }

  /**
   * Returns the length of the input stream so far.
   * @return the length of the input stream so far
   */
  long getN() { 
    return n_; 
  }

  long getBitPattern() {
    return bitPattern_;
  }

  double[] getCombinedBuffer() {
    return combinedBuffer_;
  }

  int getBaseBufferCount() {
    return baseBufferCount_;
  }

  /**
   * Returns the current element capacity of the combined data buffer given <i>k</i> and <i>n</i>.
   * 
   * @param k sketch parameter. This determines the accuracy of the sketch and the 
   * size of the updatable data structure, which is a function of k.
   * 
   * @param n The number of elements in the input stream
   * @return the current element capacity of the combined data buffer
   */
  static int bufferElementCapacity(int k, long n) {
    int maxLevels = computeNumLevelsNeeded(k, n);
    int bbCnt = (maxLevels > 0)? 2*k : ceilingPowerOf2(computeBaseBufferCount(k, n));
    return bbCnt + maxLevels*k;
  }
  
  /**
   * Computes the number of samples in the sketch from the base buffer count and the bit pattern
   * @return the number of samples in the sketch
   */
  final int numValidSamples() {
    return baseBufferCount_ + numValidLevels(bitPattern_)*k_;
  }

  /**
   * Computes a checksum of all the samples in the sketch. Used in testing the Auxiliary
   * @return a checksum of all the samples in the sketch
   */ //Used by test
  final double sumOfSamplesInSketch() {
    double total = Util.sumOfDoublesInSubArray(combinedBuffer_, 0, baseBufferCount_);
    long bits = bitPattern_;
    assert bits == n_ / (2L * k_); // internal consistency check
    for (int lvl = 0; bits != 0L; lvl++, bits >>>= 1) {
      if ((bits & 1L) > 0L) {
        total += Util.sumOfDoublesInSubArray(combinedBuffer_, ((2+lvl) * k_), k_);
      }
    }
    return total;
  }

  private void growBaseBuffer() {
    double[] baseBuffer = combinedBuffer_;
    int oldSize = combinedBufferAllocatedCount_;
    assert oldSize < 2 * k_;
    int newSize = Math.max(Math.min(2*k_, 2*oldSize), 1);
    combinedBufferAllocatedCount_ = newSize;
    double[] newBuf = Arrays.copyOf(baseBuffer, newSize);
    // just while debugging
    //for (int i = oldSize; i < newSize; i++) {newBuf[i] = DUMMY_VALUE;}
    combinedBuffer_ = newBuf;
  }

  private void maybeGrowLevels(long newN) {     // important: newN might not equal n_
    int numLevelsNeeded = computeNumLevelsNeeded(k_, newN);
    if (numLevelsNeeded == 0) {
      return; // don't need any levels yet, and might have small base buffer; this can happen during a merge
    }
    // from here on we need a full-size base buffer and at least one level
    assert newN >= 2L * k_;
    assert numLevelsNeeded > 0; 
    int spaceNeeded = (2 + numLevelsNeeded) * k_;
    if (spaceNeeded <= combinedBufferAllocatedCount_) {
      return;
    }
    double[] newCombinedBuffer = Arrays.copyOf(combinedBuffer_, spaceNeeded); // copies base buffer plus old levels
    //    just while debugging
    //for (int i = combinedBufferAllocatedCount_; i < spaceNeeded; i++) {
    //  newCombinedBuffer[i] = DUMMY_VALUE;
    //}

    combinedBufferAllocatedCount_ = spaceNeeded;
    combinedBuffer_ = newCombinedBuffer;
  }

  private static void zipSize2KBuffer(double[] bufA, int startA, // input
                                      double[] bufC, int startC, // output
                                      int k) {
    //    assert bufA.length >= 2*k; // just for now    
    //    assert startA == 0; // just for now

    //    int randomOffset = (int) (2.0 * Math.random());
    int randomOffset = (Util.rand.nextBoolean())? 1 : 0;
    //    assert randomOffset == 0 || randomOffset == 1;

    //    int limA = startA + 2*k;
    int limC = startC + k;

    for (int a = startA + randomOffset, c = startC; c < limC; a += 2, c++) {
      bufC[c] = bufA[a];
    }
  }

  private static void mergeTwoSizeKBuffers(double[] keySrc1, int arrStart1,
                                           double[] keySrc2, int arrStart2,
                                           double[] keyDst,  int arrStart3,
                                           int k) {
    int arrStop1 = arrStart1 + k;
    int arrStop2 = arrStart2 + k;

    int i1 = arrStart1;
    int i2 = arrStart2;
    int i3 = arrStart3;

    while (i1 < arrStop1 && i2 < arrStop2) {
      if (keySrc2[i2] < keySrc1[i1]) { 
        keyDst[i3++] = keySrc2[i2++];
      }     
      else { 
        keyDst[i3++] = keySrc1[i1++];
      } 
    }

    if (i1 < arrStop1) {
      System.arraycopy(keySrc1, i1, keyDst, i3, arrStop1 - i1);
    }
    else {
      assert i2 < arrStop2;
      System.arraycopy(keySrc1, i2, keyDst, i3, arrStop2 - i2);
    }

  }

  private void inPlacePropagateCarry(int startingLevel,
                                     double[] sizeKBuf, int sizeKStart,
                                     double[] size2KBuf, int size2KStart,
                                     boolean doUpdateVersion) { // else doMergeIntoVersion

    double[] levelsArr = combinedBuffer_;

    int endingLevel = positionOfLowestZeroBitStartingAt(bitPattern_, startingLevel);
    //    assert endingLevel < levelsAllocated(); // was an internal consistency check

    if (doUpdateVersion) { // update version of computation
      // its is okay for sizeKbuf to be null in this case
      zipSize2KBuffer(size2KBuf, size2KStart,
                           levelsArr, ((2+endingLevel) * k_),
                           k_);
    }
    else { // mergeInto version of computation
      System.arraycopy(sizeKBuf, sizeKStart,
                       levelsArr, ((2+endingLevel) * k_),
                       k_);
    }

    for (int lvl = startingLevel; lvl < endingLevel; lvl++) {

      assert (bitPattern_ & (1L << lvl)) > 0;  // internal consistency check

      mergeTwoSizeKBuffers(levelsArr, ((2+lvl) * k_),
                               levelsArr, ((2+endingLevel) * k_),
                               size2KBuf, size2KStart,
                               k_);

      zipSize2KBuffer(size2KBuf, size2KStart,
                          levelsArr, ((2+endingLevel) * k_),
                          k_);
      // just while debugging
      //Arrays.fill(levelsArr, ((2+lvl) * k_), ((2+lvl+1) * k_), DUMMY_VALUE);

    } // end of loop over lower levels

    // update bit pattern with binary-arithmetic ripple carry
    bitPattern_ = bitPattern_ + (((long) 1) << startingLevel);

  }

  /**
   * Called when the base buffer has just acquired 2*k elements.
   */
  private void processFullBaseBuffer() {
    assert baseBufferCount_ == 2 * k_;  // internal consistency check

    // make sure there will be enough levels for the propagation
    maybeGrowLevels(n_); // important: n_ was incremented by update before we got here

    // this aliasing is a bit dangerous; notice that we did it after the possible resizing
    double[] baseBuffer = combinedBuffer_; 

    Arrays.sort(baseBuffer, 0, baseBufferCount_);
    inPlacePropagateCarry(0,
                          null, 0,  // this null is okay
                          baseBuffer, 0,
                          true);
    baseBufferCount_ = 0;
    // just while debugging
    //Arrays.fill(baseBuffer, 0, 2*k_, DUMMY_VALUE);
    assert n_ / (2*k_) == bitPattern_;  // internal consistency check
  }

  /**
   * Shared algorithm for both PDF and CDF functions. The splitPoints must be unique, monotonically
   * increasing values.
   * @param splitPoints an array of <i>m</i> unique, monotonically increasing doubles
   * that divide the real number line into <i>m+1</i> consecutive disjoint intervals.
   * @return the unnormalized, accumulated counts of <i>m + 1</i> intervals.
   */
  private long[] internalBuildHistogram(double[] splitPoints) {
    double[] levelsArr  = combinedBuffer_; // aliasing is a bit dangerous
    double[] baseBuffer = combinedBuffer_; // aliasing is a bit dangerous

    QuantilesSketch.validateSplitPoints(splitPoints);

    int numSplitPoints = splitPoints.length;
    int numCounters = numSplitPoints + 1;
    long[] counters = new long[numCounters];

    //may need this off-heap
    //for (int j = 0; j < numCounters; j++) { counters[j] = 0; } 

    long weight = 1;
    if (numSplitPoints < 50) { // empirically determined crossover
      // sort not worth it when few split points
      bilinearTimeIncrementHistogramCounters(
          baseBuffer, 0, baseBufferCount_, weight, splitPoints, counters);
    }
    else {
      Arrays.sort(baseBuffer, 0, baseBufferCount_); 
      // sort is worth it when many split points
      linearTimeIncrementHistogramCounters(
          baseBuffer, 0, baseBufferCount_, weight, splitPoints, counters);
    }

    long myBitPattern = bitPattern_;
    assert myBitPattern == n_ / (2L * k_); // internal consistency check
    for (int lvl = 0; myBitPattern != 0L; lvl++, myBitPattern >>>= 1) {
      weight += weight; // *= 2
      if ((myBitPattern & 1L) > 0L) { //valid level exists
        // the levels are already sorted so we can use the fast version
        linearTimeIncrementHistogramCounters(
            levelsArr, (2+lvl)*k_, k_, weight, splitPoints, counters);
      }
    }
    return counters;
  }

} // End of class HeapQuantilesSketch
