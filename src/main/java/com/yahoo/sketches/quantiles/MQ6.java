/*
 * Copyright 2015, Yahoo! Inc.
 * Licensed under the terms of the Apache License 2.0. See LICENSE file at the project root for terms.
 */
package com.yahoo.sketches.quantiles;

import static com.yahoo.sketches.quantiles.Util.*;
import java.util.Arrays;

/**
 * This is an implementation of the low-discrepancy mergeable quantile sketch, using double elements, 
 * described in section 3.2 of the journal version of the paper "Mergeable Summaries"
 * by Agarwal, Cormode, Huang, Phillips, Wei, and Yi.
 * 
 *  //TODO add comments on direction consistency, exact and approximate, etc.
 * 
 * @author Kevin Lang
 */
public class MQ6 {
  private static final int MIN_BASE_BUF_SIZE = 4; //This is somewhat arbitrary
  /**
   * Parameter that controls space usage of sketch and accuracy of estimates.
   */
  private int mqK;

  /**
   * Total number of data items in the stream so far. (Uniqueness plays no role in these sketches).
   */
  private long mqN;

  /**
   * Active levels expressed as a bit pattern.
   * 
   * Pattern = N / (2 * K)
   */
  private long mqBitPattern;

  /**
   * This single array contains the base buffer plus all levels some of which are empty.
   * A level is of size K and is either full and sorted, or empty. An "empty" buffer may have
   * garbage. Whether a level buffer is empty or not is indicated by the mqBitPattern.
   * The base buffer has length 2*K but might not be full and isn't necessarily sorted.
   * The base buffer precedes the level buffers. 
   * 
   * It requires quite a bit of explanation, which we defer until later.
   */
  private double[] mqCombinedBuffer; 

  /**
   * In the initial on-heap version, equals mqCombinedBuffer.length.
   * May differ in later versions that grow space more aggressively.
   * Also, in the off-heap version, mqCombinedBuffer won't even be a java array,
   * so it won't know its own length.
   */
  private int mqAllocatedBufferSpace;

  /**
   * Number of samples currently in base buffer. Can be computed from K and N: 
   * 
   * Count = N % (2*K)
   */
  private int mqBaseBufferCount; 
  
  /**
   * The smallest value ever seen in the stream.
   */
  private double mqMin;

  /**
   * The largest value ever seen in the stream.
   */
  private double mqMax;

  /**
   * Constructs a Mergeable Quantile Sketch of double elements.
   * @param k Parameter that controls space usage of sketch and accuracy of estimates
   */
  public MQ6(int k) {
    if (k <= 0) throw new IllegalArgumentException("K must be greater than zero");
    mqK = k;
    mqN = 0;
    mqAllocatedBufferSpace = Math.min(MIN_BASE_BUF_SIZE,2*k); //the min is important
    mqCombinedBuffer = new double[mqAllocatedBufferSpace];
    mqBaseBufferCount = 0;
    mqMin = java.lang.Double.POSITIVE_INFINITY;
    mqMax = java.lang.Double.NEGATIVE_INFINITY;
  }

  /** 
   * Updates this sketch with the given double data item
   * @param dataItem an item from a stream of items.  NaNs are ignored.
   */
  public void update(double dataItem) {
    // this method is only directly using the base buffer part of the combined buffer
    if (Double.isNaN(dataItem)) return;

    if (dataItem > mqMax) { mqMax = dataItem; }   // benchmarks faster than Math.max()
    if (dataItem < mqMin) { mqMin = dataItem; }

    if (mqBaseBufferCount+1 > mqAllocatedBufferSpace) {
      growBaseBuffer();
    } 
    mqCombinedBuffer[mqBaseBufferCount++] = dataItem;
    mqN++;
    if (mqBaseBufferCount == 2*mqK) {
      processFullBaseBuffer();
    }
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
  * Modified the target sketch by merging the source sketch into it.
  * @param mqTarget The target sketch
  * @param mqSource The source sketch
  */
  public static void mergeInto(MQ6 mqTarget, MQ6 mqSource) {  

    if ( mqTarget.mqK != mqSource.mqK) 
      throw new IllegalArgumentException("Given sketches must have the same value of k.");

    MQ6 mq1 = mqTarget;
    MQ6 mq2 = mqSource;
    double [] mq2Levels     = mq2.mqCombinedBuffer; // aliasing is a bit dangerous
    double [] mq2BaseBuffer = mq2.mqCombinedBuffer; // aliasing is a bit dangerous

    int k = mq1.mqK;
    long nFinal = mq1.mqN + mq2.mqN;
    
    for (int i = 0; i < mq2.mqBaseBufferCount; i++) {
      mq1.update (mq2BaseBuffer[i]);
    }

    mq1.maybeGrowLevels (nFinal); 

    double [] scratchBuf = new double [2*k];

    long bits2 = mq2.mqBitPattern;
    assert bits2 == mq2.mqN / (2L * mq2.mqK);
    for (int lvl2 = 0; bits2 != 0L; lvl2++, bits2 >>>= 1) {
      if ((bits2 & 1L) > 0L) {
        mq1.inPlacePropagateCarry (lvl2,
                                   mq2Levels, ((2+lvl2) * k),
                                   scratchBuf, 0,
                                   false);
        // won't updated mq1.mqN until the very end
      }
    }

    mq1.mqN = nFinal;
    
    assert mq1.mqN / (2*k) == mq1.mqBitPattern; // internal consistency check

    if (mq2.mqMax > mq1.mqMax) { mq1.mqMax = mq2.mqMax; }
    if (mq2.mqMin < mq1.mqMin) { mq1.mqMin = mq2.mqMin; }

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
    if      (fraction == 0.0) { return mqMin; }
    else if (fraction == 1.0) { return mqMax; }
    else {
      Auxiliary au = this.constructAuxiliary ();
      return (au.getQuantile (fraction));
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
  public double [] getQuantiles(double [] fractions) {
    Auxiliary au = null;
    double [] answers = new double [fractions.length];
    for (int i = 0; i < fractions.length; i++) {
      double fraction = fractions[i];
      if ((fraction < 0.0) || (fraction > 1.0)) {
        throw new IllegalArgumentException("Fraction cannot be less than zero or greater than 1.0");
      }
      if      (fraction == 0.0) { answers[i] = mqMin; }
      else if (fraction == 1.0) { answers[i] = mqMax; }
      else {
        if (au == null) au = this.constructAuxiliary ();
        answers[i] = au.getQuantile (fraction);
      }
    }
    return (answers);
  }

  /**
   * Returns an approximation to the Probability Density Function (PDF)[1] of the input stream 
   * given a set of splitPoints (values).  
   * 
   * The resulting approximations have a probabilistic guarantee that be obtained from the 
   * getNormalizedCountError() function. 
   * 
   * <p>
   * [1] Actually the name PMF (Probability Mass Function) might be a more precise name, 
   * but is less well known.
   * 
   * @param splitPoints an array of <i>m</i> unique, monotonically increasing doubles
   * that divide the real number line into <i>m+1</i> consecutive disjoint intervals.
   * 
   * @return an array of m+1 doubles each of whose entries is an approximation
   * to the fraction of the input stream values that fell into one of those intervals.
   * The definition of an "interval" is inclusive of the left splitPoint and exclusive of the right
   * splitPoint.
   */
  @SuppressWarnings("cast")
  public double[] getPDF(double[] splitPoints) {
    long [] counters = internalBuildHistogram (splitPoints);
    int numCounters = counters.length;
    double [] result = new double [numCounters];
    double denom = (double) this.mqN;
    long subtotal = 0;
    for (int j = 0; j < numCounters; j++) { 
      long count = counters[j];
      subtotal += count;
      result[j] = ((double) count) / denom;
    }
    assert (subtotal == this.mqN); //internal consistency check
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
  @SuppressWarnings("cast")
  public double[] getCDF(double [] splitPoints) {
    long [] counters = internalBuildHistogram (splitPoints);
    int numCounters = counters.length;
    double [] result = new double [numCounters];
    double denom = (double) this.mqN;
    long subtotal = 0;
    for (int j = 0; j < numCounters; j++) { 
      long count = counters[j];
      subtotal += count;
      result[j] = ((double) subtotal) / denom;
    }
    assert (subtotal == this.mqN); //internal consistency check
    return result;
  }
  


  // start of code for computing epsilon from K

  /*
         eps      eps from inverted
   K   empirical  adjusted formula
-------------------------------------
  16   0.121094   0.121454102233560
  32   0.063477   0.063586601346532
  64   0.033081   0.033169048393679
 128   0.017120   0.017248096847308
 256   0.008804   0.008944835012965
 512   0.004509   0.004627803568920
1024   0.002303   0.002389303789572
  */

  /*
these could be used in a unit test
2   0.821714930853465
16   0.12145410223356
1024   0.00238930378957284
1073741824   3.42875166500824e-09
  */

  // the following delta was used while crunching down the empirical results
  private static final double deltaForEps = 0.01;  

  // this is a heuristic fudge factor that causes the inverted formula to better match the empirical
  private static final double adjustKForEps = 4.0 / 3.0;  // fudge factor

  // ridiculously fine tolerance given the fudge factor; 1e-3 would probably suffice
  private static final double bracketedBinarySearchForEpsTol = 1e-15; 

  static double kOfEpsFormula (double eps) {
    return ((1.0 / eps) * (Math.sqrt (Math.log (1.0 / (eps * deltaForEps)))));
  }

  static boolean epsForKPredicate (double eps, double kf) {
    return (kOfEpsFormula(eps) >= kf);
  }

  static double bracketedBinarySearchForEps (double kf, double lo, double hi) {
    assert lo < hi;
    assert epsForKPredicate(lo, kf);
    assert !epsForKPredicate(hi, kf);
    if ((hi - lo) / lo < bracketedBinarySearchForEpsTol) {
      return lo;
    }
    double mid = (lo + hi) / 2.0;
    assert mid > lo;
    assert mid < hi;
    if (epsForKPredicate(mid, kf)) {
      return (bracketedBinarySearchForEps (kf, mid, hi));
    }
    else {
      return (bracketedBinarySearchForEps (kf, lo, mid));
    }
  }

  static double findEpsForK (double kf) {
    assert (kf >= 2.15); // ensures that the bracketing succeeds
    assert (kf < 1e12);  // ditto, but could actually be bigger
    double lo = 1e-16;
    double hi = 1.0 - 1e-16;
    assert epsForKPredicate(lo, kf);
    assert !epsForKPredicate(hi, kf);
    return (bracketedBinarySearchForEps (kf, lo, hi));
  }

  static double adjustedFindEpsForK (int k) {
    assert (k >= 2); // should actually raise invalid input
    // don't need to check in the other direction because an int is very small
    return (findEpsForK (adjustKForEps * k));
  }

  public double getNormalizedCountError() {
    return adjustedFindEpsForK (mqK);
  }

  // end of code for computing epsilon from K

  /**
   * Returns the length of the input stream so far.
   * @return the length of the input stream so far
   */
  public long getStreamLength() {
    return (mqN);
  }
  
  //Restricted methods
  
  Auxiliary constructAuxiliary() {
    Auxiliary au = Auxiliary.constructAuxiliaryFromMQ6 (mqK, mqN, 
                                                        mqBitPattern, mqCombinedBuffer, mqBaseBufferCount,
                                                        numSamplesInSketch());
    return au;
  }

  private void growBaseBuffer() {
    double [] mqBaseBuffer = mqCombinedBuffer;
    int oldSize = mqAllocatedBufferSpace;
    assert (oldSize < 2 * mqK);
    int newSize = Math.max (Math.min (2*mqK, 2*oldSize), 1);
    mqAllocatedBufferSpace = newSize;
    double [] newBuf = Arrays.copyOf (mqBaseBuffer, newSize);
    // just while debugging
    for (int i = oldSize; i < newSize; i++) {newBuf[i] = mqDummyValue;}
    mqCombinedBuffer = newBuf;
  }

  private void maybeGrowLevels (long newN) {     // important: newN might not equal mqN
    int numLevelsNeeded = computeNumLevelsNeeded (mqK, newN);
    if (numLevelsNeeded == 0) {
      return; // don't need any levels yet, and might have small base buffer; this can happen during a merge
    }
    // from here on we need a full-size base buffer and at least one level
    assert (newN >= 2L * mqK);
    assert (numLevelsNeeded > 0); 
    int spaceNeeded = (2 + numLevelsNeeded) * mqK;
    if (spaceNeeded <= mqAllocatedBufferSpace) {
      return;
    }
    double [] newCombinedBuffer = Arrays.copyOf (mqCombinedBuffer, spaceNeeded); // copies base buffer plus old levels
    //    just while debugging
    for (int i = mqAllocatedBufferSpace; i < spaceNeeded; i++) {newCombinedBuffer[i] = mqDummyValue;}

    mqAllocatedBufferSpace = spaceNeeded;
    mqCombinedBuffer = newCombinedBuffer;
  }

  private static void justZipSize2KBuffer (double [] bufA, int startA, // input
                                           double [] bufC, int startC, // output
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

  private static void justMergeTwoSizeKBuffers (double [] keySrc1, int arrStart1,
                                                double [] keySrc2, int arrStart2,
                                                double [] keyDst,  int arrStart3,
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

  private void inPlacePropagateCarry (int startingLevel,
                                      double [] sizeKBuf, int sizeKStart,
                                      double [] size2KBuf, int size2KStart,
                                      boolean doUpdateVersion) { // else doMergeIntoVersion

    double [] mqLevels = mqCombinedBuffer;

    int endingLevel = positionOfLowestZeroBitStartingAt (mqBitPattern, startingLevel);
    //    assert endingLevel < mqLevelsAllocated(); // was an internal consistency check

    if (doUpdateVersion) { // update version of computation
      // its is okay for sizeKbuf to be null in this case
      justZipSize2KBuffer (size2KBuf, size2KStart,
                           mqLevels, ((2+endingLevel) * mqK),
                           mqK);
    }
    else { // mergeInto version of computation
      System.arraycopy (sizeKBuf, sizeKStart,
                        mqLevels, ((2+endingLevel) * mqK),
                        mqK);
    }

    for (int lvl = startingLevel; lvl < endingLevel; lvl++) {

      assert (mqBitPattern & (((long) 1) << lvl)) > 0;  // internal consistency check

      justMergeTwoSizeKBuffers (mqLevels, ((2+lvl) * mqK),
                                mqLevels, ((2+endingLevel) * mqK),
                                size2KBuf, size2KStart,
                                mqK);

      justZipSize2KBuffer (size2KBuf, size2KStart,
                           mqLevels, ((2+endingLevel) * mqK),
                           mqK);
    // just while debugging
      Arrays.fill (mqLevels, ((2+lvl) * mqK), ((2+lvl+1) * mqK), mqDummyValue);

    } // end of loop over lower levels

    // update bit pattern with binary-arithmetic ripple carry
    mqBitPattern = mqBitPattern + (((long) 1) << startingLevel);

  }

  /**
   * Called when the base buffer has just acquired 2*k elements.
   */
  private void processFullBaseBuffer() {
    assert mqBaseBufferCount == 2 * mqK;  // internal consistency check
    // make sure there will be enough levels for the propagation

    maybeGrowLevels (mqN); // important: mqN was incremented by update before we got here

    // this aliasing is a bit dangerous; notice that we did it after the possible resizing
    double [] mqBaseBuffer = mqCombinedBuffer; 

    Arrays.sort (mqBaseBuffer, 0, mqBaseBufferCount);
    inPlacePropagateCarry (0,
                           null, 0,  // this null is okay
                           mqBaseBuffer, 0,
                           true);
    mqBaseBufferCount = 0;
    // just while debugging
    Arrays.fill (mqBaseBuffer, 0, 2*mqK, mqDummyValue);
    assert mqN / (2*mqK) == mqBitPattern;  // internal consistency check
  }
  
  /**
   * Shared algorithm for both PDF and CDF functions. The splitPoints must be unique, monotonically
   * increasing values.
   * @param splitPoints an array of <i>m</i> unique, monotonically increasing doubles
   * that divide the real number line into <i>m+1</i> consecutive disjoint intervals.
   * @return the unnormalized, accumulated counts of <i>m + 1</i> intervals.
   */
  private long[] internalBuildHistogram (double[] splitPoints) {
    double [] mqLevels     = mqCombinedBuffer; // aliasing is a bit dangerous
    double [] mqBaseBuffer = mqCombinedBuffer; // aliasing is a bit dangerous

    validateSplitPoints (splitPoints);

    int numSplitPoints = splitPoints.length;
    int numCounters = numSplitPoints + 1;
    long [] counters = new long [numCounters];

    for (int j = 0; j < numCounters; j++) { counters[j] = 0; } // already true, right?

    long weight = 1;
    if (numSplitPoints < 50) { // empirically determined crossover for K = 200
      // not worth it to sort when few split points
      Util.quadraticTimeIncrementHistogramCounters (mqBaseBuffer, 0, mqBaseBufferCount, weight, splitPoints, counters);
    }
    else {
      Arrays.sort (mqBaseBuffer, 0, mqBaseBufferCount); // sort is worth it when many split points
      Util.linearTimeIncrementHistogramCounters (mqBaseBuffer, 0, mqBaseBufferCount, weight, splitPoints, counters);
    }

    long bits = mqBitPattern;
    assert bits == mqN / (2L * mqK); // internal consistency check
    for (int lvl = 0; bits != 0L; lvl++, bits >>>= 1) {
      weight += weight; // *= 2
      if ((bits & 1L) > 0L) {
        // the levels are already sorted so we can use the fast version
        Util.linearTimeIncrementHistogramCounters (mqLevels, (2+lvl)*mqK, mqK, weight, splitPoints, counters);
                                                   //(mq.mqLevels[lvl], 0, mq.mqK, 
      }
    }
    return counters;
  }
  
  private static void validateSplitPoints(double[] splitPoints) {
    for (int j = 0; j < splitPoints.length - 1; j++) {
      if (splitPoints[j] < splitPoints[j+1]) { continue; }
      throw new IllegalArgumentException(
          "SplitPoints must be unique, monotonically increasing and not NaN.");
    }
  }
  
  private static int computeNumLevelsNeeded(int k, long n) {
    long long2k = ((long) 2 * k);
    long quo = n / long2k;
    if (quo == 0) return 0;
    else return (1 + (hiBitPos (quo)));
  }
  
  int getK() { 
    return mqK; 
  }
  
  long getN() { 
    return mqN; 
  }
  
  int numSamplesInSketch() {
    int count = mqBaseBufferCount;
    long bits = mqBitPattern;
    assert bits == mqN / (2L * mqK); // internal consistency check
    for (int lvl = 0; bits != 0L; lvl++, bits >>>= 1) {
      if ((bits & 1L) > 0L) {
        count += mqK;
      }
    }
    return count;
  }

  double sumOfSamplesInSketch() {
    double total = Util.sumOfDoublesInSubArray (mqCombinedBuffer, 0, mqBaseBufferCount);
    long bits = mqBitPattern;
    assert bits == mqN / (2L * mqK); // internal consistency check
    for (int lvl = 0; bits != 0L; lvl++, bits >>>= 1) {
      if ((bits & 1L) > 0L) {
        total += Util.sumOfDoublesInSubArray (mqCombinedBuffer, ((2+lvl) * mqK), mqK);
      }
    }
    return total;
  }

  void show () { // just for debugging
    double [] mqLevels     = mqCombinedBuffer;
    double [] mqBaseBuffer = mqCombinedBuffer;
    int numLevels = computeNumLevelsNeeded (mqK, mqN);
    System.out.printf ("showing: K=%d N=%d levels=%d combinedBufferLength=%d baseBufferCount=%d bitPattern=%d\n",
                       mqK, mqN, numLevels, mqAllocatedBufferSpace, mqBaseBufferCount, mqBitPattern);
    for (int i = 0; i < mqBaseBufferCount; i++) {
      System.out.printf (" %.1f", mqBaseBuffer[i]);
    }
    System.out.printf ("\n");
    System.out.printf ("Levels:\n");
    for (int j = 2*mqK; j < mqAllocatedBufferSpace; j++) {
      if (j % mqK == 0) {
        System.out.printf ("\n");
      }
      System.out.printf ("    %.1f", mqLevels[j]);
    }
    System.out.printf ("\n");
  }

  private static double mqDummyValue = -99.0;  // just for debugging

} // End of class MQ6

