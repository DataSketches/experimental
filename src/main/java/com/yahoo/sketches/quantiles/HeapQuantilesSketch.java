/*
 * Copyright 2015, Yahoo! Inc.
 * Licensed under the terms of the Apache License 2.0. See LICENSE file at the project root for terms.
 */
package com.yahoo.sketches.quantiles;

import static com.yahoo.sketches.Util.ceilingPowerOf2;
import static com.yahoo.sketches.quantiles.Util.*;
import java.util.Arrays;

/**
 * This is an implementation of the low-discrepancy mergeable quantile sketch using double 
 * valued elements. This is an implementation of the Low Discrepancy Mergeable Quantiles Sketch 
 * described in section 3.2 of the journal version of the paper "Mergeable Summaries" 
 * by Agarwal, Cormode, Huang, Phillips, Wei, and Yi.
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
public class HeapQuantilesSketch extends QuantilesSketch {
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

  /**
   * Constructs a Mergeable Quantile Sketch of double elements.
   * @param k Parameter that controls space usage of sketch and accuracy of estimates. 
   * Must be greater than one.
   */
  public HeapQuantilesSketch(int k) {
    checkK(k);
    k_ = k;
    n_ = 0;
    combinedBufferAllocatedCount_ = Math.min(MIN_BASE_BUF_SIZE,2*k); //the min is important
    combinedBuffer_ = new double[combinedBufferAllocatedCount_];
    baseBufferCount_ = 0;
    bitPattern_ = 0;
    minValue_ = java.lang.Double.POSITIVE_INFINITY;
    maxValue_ = java.lang.Double.NEGATIVE_INFINITY;
  }

  /**
   * Resets this sketch to a virgin state, but retains the original value of k.
   */
  public void reset() {
    n_ = 0;
    combinedBufferAllocatedCount_ = Math.min(MIN_BASE_BUF_SIZE,2*k_); //the min is important
    combinedBuffer_ = new double[combinedBufferAllocatedCount_];
    baseBufferCount_ = 0;
    bitPattern_ = 0;
    minValue_ = java.lang.Double.POSITIVE_INFINITY;
    maxValue_ = java.lang.Double.NEGATIVE_INFINITY;
  }

  /** 
   * Updates this sketch with the given double data item
   * @param dataItem an item from a stream of items.  NaNs are ignored.
   */
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

  /*
    Note: a two-way merge that doesn't modify either of its
    two inputs could be implemented by making a deep copy of
    the larger sketch and then merging the smaller one into it.
    However, it was decided not to do this.
  */

  /**
   * This returns an approximation to the value of the data item
   * that would be preceded by the given fraction of a hypothetical sorted
   * version of the input stream so far.
   * 
   * <p>
   * We note that this method has a fairly large overhead (microseconds instead of nanoseconds)
   * so it should not be called multiple times to get different quantiles from the same
   * sketch. Instead use getQuantiles(). which pays the overhead only once.
   * 
   * @param fraction the specified fractional position in the hypothetical sorted stream.
   * If fraction = 0.0, the true minimum value of the stream is returned. 
   * If fraction = 1.0, the true maximum value of the stream is returned. 
   * 
   * @return the approximation to the value at the above fraction
   */
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

  /**
   * This is a more efficent multiple-query version of getQuantile().
   * <p>
   * This returns an array that could have been generated by
   * mapping getQuantile() over the given array of fractions.
   * However, the computational overhead of getQuantile() is shared
   * amongst the multiple queries. Therefore, we strongly recommend this method
   * instead of multiple calls to getQuantile().
   * 
   * @param fractions given array of fractional positions in the hypothetical sorted stream. 
   * It is recommended that these be in increasing order.
   * 
   * @return array of approximations to the given fractions in the same order as given fractions array. 
   */
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

  /**
   * Returns an approximation to the Probability Density Function (PDF)[1] of the input stream 
   * given a set of splitPoints (values).  
   * 
   * The resulting approximations have a probabilistic guarantee that be obtained from the 
   * getNormalizedRankError() function.
   * 
   * <p>
   * [1] Actually the name PMF (Probability Mass Function) might be a more precise name, 
   * but is less well known.
   * 
   * @param splitPoints an array of <i>m</i> unique, monotonically increasing doubles
   * that divide the real number line into <i>m+1</i> consecutive disjoint intervals.
   * 
   * @return an array of m+1 doubles each of which is an approximation
   * to the fraction of the input stream values that fell into one of those intervals.
   * The definition of an "interval" is inclusive of the left splitPoint and exclusive of the right
   * splitPoint.
   */
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

  /**
   * Returns an approximation to the Cumulative Distribution Function (CDF), which is the 
   * cumulative analog of the PDF, of the input stream given a set of splitPoint (values).
   * <p>
   * More specifically, the value at array position j of the CDF is the
   * sum of the values in positions 0 through j of the PDF.
   * 
   * @param splitPoints an array of <i>m</i> unique, monotonically increasing doubles
   * that divide the real number line into <i>m+1</i> consecutive disjoint intervals.
   * 
   * @return an approximation to the CDF of the input stream given the splitPoints.
   */
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

  /**
   * Get the rank error normalized as a fraction between zero and one. 
   * The error of this sketch is specified as a fraction of the normalized rank of the hypothetical 
   * sorted stream of items presented to the sketch. 
   * 
   * <p>Suppose the sketch is presented with N values. The raw rank (0 to N-1) of an item 
   * would be its index position in the sorted version of the input stream. If we divide the 
   * raw rank by N, it becomes the normalized rank, which is between 0 and 1.0.
   * 
   * <p>For example, choosing a K of 227 yields a normalized rank error of about 1%. 
   * The upper bound on the median value obtained by getQuantile(0.5) would be the value in the 
   * hypothetical ordered stream of values at the normalized rank of 0.51. 
   * The lower bound would be the value in the hypothetical ordered stream of values at the 
   * normalized rank of 0.49.
   * 
   * <p>The error of this sketch cannot be translated into an error (relative or absolute) of the 
   * returned quantile values. 
   * 
   * @return the rank error normalized as a fraction between zero and one.
   */
  public double getNormalizedRankError() {
    return Util.EpsilonFromK.getAdjustedEpsilon(k_);
  }

  /**
   * Returns the length of the input stream so far.
   * @return the length of the input stream so far
   */
  public long getStreamLength() {
    return n_;
  }

  /**
   * Returns the configured value of K
   * @return the configured value of K
   */
  public int getK() { 
    return k_; 
  }

  /**
   * Returns the min value of the stream
   * @return the min value of the stream
   */
  public double getMinValue() {
    return minValue_;
  }

  /**
   * Returns the max value of the stream
   * @return the max value of the stream
   */
  public double getMaxValue() {
    return maxValue_;
  }

  @Override
  public String toString() {
    return toString(true, false);
  }

  /**
   * Returns summary information about the sketch. Used for debugging
   * @param sketchSummary if true includes sketch summary
   * @param dataDetail if true includes data detail
   * @return summary information about the sketch.
   */
  public String toString(boolean sketchSummary, boolean dataDetail ) {
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
      sb.append("   Valid Levels                 : ").append(numValidLevels()).append(LS);
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

  //Restricted

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
   * Computes the number of logarithmic levels needed given k and n.
   * This is equivalent to max(floor(lg(n/k), 0).
   * @param k the configured size of the sketch
   * @param n the total values presented to the sketch.
   * @return the number of levels needed.
   */
  static final int computeNumLevelsNeeded(int k, long n) {
    return 1 + hiBitPos(n / (2L * k));
  }

  /**
   * Computes the number of valid levels above the base buffer
   * @return the number of valid levels above the base buffer
   */
  final int numValidLevels() {
    return Long.bitCount(bitPattern_);
  }
  
  static int bufferElementCapacity(int k, long n) {
    int maxLevels = computeNumLevelsNeeded(k, n);
    int bbCnt = (maxLevels > 0)? 2*k : 
      ceilingPowerOf2(computeBaseBufferCount(k, n));
    return bbCnt + maxLevels*k;
  }
  
  /**
   * Computes the levels bit pattern given k, n.
   * This is computed as <i>n / (2*k)</i>.
   * @param k the configured size of the sketch
   * @param n the total values presented to the sketch.
   * @return the levels bit pattern
   */
  static final long computeBitPattern(int k, long n) {
    return n / (2L * k);
  }

  /**
   * Computes the base buffer count given k, n
   * @param k the configured size of the sketch
   * @param n the total values presented to the sketch
   * @return the base buffer count
   */
  static final int computeBaseBufferCount(int k, long n) {
    return (int) (n % (2L * k));
  }

  /**
   * Computes the number of samples in the sketch from the base buffer count and the bit pattern
   * @return the number of samples in the sketch
   */
  final int numValidSamples() {
    return baseBufferCount_ + Long.bitCount(bitPattern_)*k_;
  }

  /**
   * Computes a checksum of all the samples in the sketch. Used in testing the Auxiliary
   * @return a checksum of all the samples in the sketch
   */
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
