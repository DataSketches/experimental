/*
 * Copyright 2015, Yahoo! Inc.
 * Licensed under the terms of the Apache License 2.0. See LICENSE file at the project root for terms.
 */
package com.yahoo.sketches.quantiles;

import static com.yahoo.sketches.quantiles.Util.*;
import java.util.Arrays;

/**
 * This is an implementation of the low-discrepancy mergeable quantile sketch, using double 
 * valued elements. The basic algorithm concepts are described in section 3.2 of the 
 * journal version of the paper "Mergeable Summaries" by Agarwal, Cormode, Huang, Phillips, Wei, 
 * and Yi.
 * 
 * This algorithm intentionally inserts randomness into the process that selects the values that
 * ultimately get retained in the sketch. The consequence of this is that this algorithm is not 
 * deterministic. I.e., if the same exact stream with the same ordering is inserted into two 
 * different instances of this sketch, the answers obtained from the two sketches may not be 
 * exactly the same but both will be within the error tolerances for the sketch.
 * 
 * Similarly, there may be directional inconsistencies. For example, the resulting array of values
 * obtained from getQuantiles(fractions[]) input into the reverse directional query 
 * getPDF(splitPoints[]) may not result in the same fractional values.
 * 
 * @author Kevin Lang
 * @author Lee Rhodes
 */
public class QuantilesSketch {
  private static final int MIN_BASE_BUF_SIZE = 4; //This is somewhat arbitrary

  @SuppressWarnings("unused")
  private final static double DUMMY_VALUE = -99.0;  // just for debugging
  
  /**
   * Parameter that controls space usage of sketch and accuracy of estimates.
   */
  private final int k_;

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
   * Number of samples currently in base buffer. Can be computed from K and N: 
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
  private int allocatedBufferSpace_;

  /**
   * This single array contains the base buffer plus all levels some of which are empty.
   * A level is of size K and is either full and sorted, or empty. An "empty" buffer may have
   * garbage. Whether a level buffer is empty or not is indicated by the bitPattern_.
   * The base buffer has length 2*K but might not be full and isn't necessarily sorted.
   * The base buffer precedes the level buffers. 
   * 
   * It requires quite a bit of explanation, which we defer until later.
   */
  private double[] combinedBuffer_;

  /**
   * Constructs a Mergeable Quantile Sketch of double elements.
   * @param k Parameter that controls space usage of sketch and accuracy of estimates
   */
  public QuantilesSketch(int k) {
    if (k <= 0) throw new IllegalArgumentException("K must be greater than zero");
    k_ = k;
    n_ = 0;
    allocatedBufferSpace_ = Math.min(MIN_BASE_BUF_SIZE,2*k); //the min is important
    combinedBuffer_ = new double[allocatedBufferSpace_];
    baseBufferCount_ = 0;
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

    if (baseBufferCount_+1 > allocatedBufferSpace_) {
      growBaseBuffer();
    } 
    combinedBuffer_[baseBufferCount_++] = dataItem;
    n_++;
    if (baseBufferCount_ == 2*k_) {
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
  * Modifies the target sketch by merging the source sketch into it.
  * @param qsTarget The target sketch
  * @param qsSource The source sketch
  */
  //TODO the source and target order need to be reversed.
  public static void mergeInto(QuantilesSketch qsTarget, QuantilesSketch qsSource) {
    if ( qsTarget.k_ != qsSource.k_) 
      throw new IllegalArgumentException("Given sketches must have the same value of k.");

    double[] srcLevels     = qsSource.combinedBuffer_; // aliasing is a bit dangerous
    double[] srcBaseBuffer = qsSource.combinedBuffer_; // aliasing is a bit dangerous

    int tgtK = qsTarget.getK();
    long nFinal = qsTarget.getN() + qsSource.getN();
    
    for (int i = 0; i < qsSource.baseBufferCount_; i++) {
      qsTarget.update (srcBaseBuffer[i]);
    }

    qsTarget.maybeGrowLevels (nFinal); 

    double[] scratchBuf = new double[2*tgtK];

    long srcBits = qsSource.bitPattern_;
    assert srcBits == qsSource.getN() / (2L * qsSource.getK());
    for (int srcLvl = 0; srcBits != 0L; srcLvl++, srcBits >>>= 1) {
      if ((srcBits & 1L) > 0L) {
        qsTarget.inPlacePropagateCarry (srcLvl,
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
      Auxiliary aux = this.constructAuxiliary ();
      return (aux.getQuantile (fraction));
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
  public double[] getQuantiles(double [] fractions) {
    Auxiliary aux = null;
    double [] answers = new double [fractions.length];
    for (int i = 0; i < fractions.length; i++) {
      double fraction = fractions[i];
      if ((fraction < 0.0) || (fraction > 1.0)) {
        throw new IllegalArgumentException("Fraction cannot be less than zero or greater than 1.0");
      }
      if      (fraction == 0.0) { answers[i] = minValue_; }
      else if (fraction == 1.0) { answers[i] = maxValue_; }
      else {
        if (aux == null) aux = this.constructAuxiliary ();
        answers[i] = aux.getQuantile (fraction);
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
  public double[] getPDF(double[] splitPoints) {
    long [] counters = internalBuildHistogram (splitPoints);
    int numCounters = counters.length;
    double [] result = new double [numCounters];
    double n = n_;
    long subtotal = 0;
    for (int j = 0; j < numCounters; j++) { 
      long count = counters[j];
      subtotal += count;
      result[j] = count / n;
    }
    assert (subtotal == n); //internal consistency check
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
    long [] counters = internalBuildHistogram (splitPoints);
    int numCounters = counters.length;
    double [] result = new double [numCounters];
    double n = n_;
    long subtotal = 0;
    for (int j = 0; j < numCounters; j++) { 
      long count = counters[j];
      subtotal += count;
      result[j] = subtotal / n;
    }
    assert (subtotal == n); //internal consistency check
    return result;
  }
  
  /**
   * Get the count (or rank) error normalized as a fraction between zero and one.
   * @return the count (or rank) error normalized as a fraction between zero and one.
   */
  public double getNormalizedCountError() {
    return EpsilonFromK.adjustedFindEpsForK(k_);
  }

  /**
   * Returns the length of the input stream so far.
   * @return the length of the input stream so far
   */
  public long getStreamLength() {
    return n_;
  }
  
  //Restricted
  
  long getBitPattern() {
    return bitPattern_;
  }
  
  double[] getCombinedBuffer() {
    return combinedBuffer_;
  }
  
  int getBaseBufferCount() {
    return baseBufferCount_;
  }
  
  int getK() { 
    return k_; 
  }
  
  long getN() { 
    return n_; 
  }
  
  void setN(long n) {
    n_ = n;
  }
  
  Auxiliary constructAuxiliary() {
    return new Auxiliary( this );
        //k_, n_, bitPattern_, combinedBuffer_, baseBufferCount_, numSamplesInSketch());
  }

  private void growBaseBuffer() {
    double [] mqBaseBuffer = combinedBuffer_;
    int oldSize = allocatedBufferSpace_;
    assert (oldSize < 2 * k_);
    int newSize = Math.max (Math.min (2*k_, 2*oldSize), 1);
    allocatedBufferSpace_ = newSize;
    double [] newBuf = Arrays.copyOf (mqBaseBuffer, newSize);
    // just while debugging   //TODO confirm w/ Kevin
    //for (int i = oldSize; i < newSize; i++) {newBuf[i] = DUMMY_VALUE;}
    combinedBuffer_ = newBuf;
  }

  private void maybeGrowLevels(long newN) {     // important: newN might not equal n_
    int numLevelsNeeded = computeNumLevelsNeeded (k_, newN);
    if (numLevelsNeeded == 0) {
      return; // don't need any levels yet, and might have small base buffer; this can happen during a merge
    }
    // from here on we need a full-size base buffer and at least one level
    assert (newN >= 2L * k_);
    assert (numLevelsNeeded > 0); 
    int spaceNeeded = (2 + numLevelsNeeded) * k_;
    if (spaceNeeded <= allocatedBufferSpace_) {
      return;
    }
    double[] newCombinedBuffer = Arrays.copyOf (combinedBuffer_, spaceNeeded); // copies base buffer plus old levels
    //    just while debugging   //TODO confirm w/ Kevin
    //for (int i = allocatedBufferSpace_; i < spaceNeeded; i++) {newCombinedBuffer[i] = DUMMY_VALUE;}

    allocatedBufferSpace_ = spaceNeeded;
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

    double[] mqLevels = combinedBuffer_;

    int endingLevel = positionOfLowestZeroBitStartingAt (bitPattern_, startingLevel);
    //    assert endingLevel < mqLevelsAllocated(); // was an internal consistency check

    if (doUpdateVersion) { // update version of computation
      // its is okay for sizeKbuf to be null in this case
      zipSize2KBuffer(size2KBuf, size2KStart,
                           mqLevels, ((2+endingLevel) * k_),
                           k_);
    }
    else { // mergeInto version of computation
      System.arraycopy(sizeKBuf, sizeKStart,
                       mqLevels, ((2+endingLevel) * k_),
                       k_);
    }

    for (int lvl = startingLevel; lvl < endingLevel; lvl++) {

      assert (bitPattern_ & (((long) 1) << lvl)) > 0;  // internal consistency check

      mergeTwoSizeKBuffers(mqLevels, ((2+lvl) * k_),
                               mqLevels, ((2+endingLevel) * k_),
                               size2KBuf, size2KStart,
                               k_);

      zipSize2KBuffer(size2KBuf, size2KStart,
                          mqLevels, ((2+endingLevel) * k_),
                          k_);
      // just while debugging    //TODO confirm w/ Kevin
      //Arrays.fill(mqLevels, ((2+lvl) * k_), ((2+lvl+1) * k_), DUMMY_VALUE);

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
    double[] mqBaseBuffer = combinedBuffer_; 

    Arrays.sort(mqBaseBuffer, 0, baseBufferCount_);
    inPlacePropagateCarry(0,
                          null, 0,  // this null is okay
                          mqBaseBuffer, 0,
                          true);
    baseBufferCount_ = 0;
    // just while debugging  //TODO confirm w/ Kevin
    //Arrays.fill(mqBaseBuffer, 0, 2*k_, DUMMY_VALUE);
    assert n_ / (2*k_) == bitPattern_;  // internal consistency check
  }
  
  /**
   * Shared algorithm for both PDF and CDF functions. The splitPoints must be unique, monotonically
   * increasing values.
   * @param splitPoints an array of <i>m</i> unique, monotonically increasing doubles
   * that divide the real number line into <i>m+1</i> consecutive disjoint intervals.
   * @return the unnormalized, accumulated counts of <i>m + 1</i> intervals.
   */
  private long[] internalBuildHistogram (double[] splitPoints) {
    double[] mqLevels     = combinedBuffer_; // aliasing is a bit dangerous
    double[] mqBaseBuffer = combinedBuffer_; // aliasing is a bit dangerous

    validateSplitPoints (splitPoints);

    int numSplitPoints = splitPoints.length;
    int numCounters = numSplitPoints + 1;
    long[] counters = new long [numCounters];

    for (int j = 0; j < numCounters; j++) { counters[j] = 0; } // already true, right?

    long weight = 1;
    if (numSplitPoints < 50) { // empirically determined crossover for K = 200
      // not worth it to sort when few split points
      Util.quadraticTimeIncrementHistogramCounters(mqBaseBuffer, 0, baseBufferCount_, weight, splitPoints, counters);
    }
    else {
      Arrays.sort(mqBaseBuffer, 0, baseBufferCount_); // sort is worth it when many split points
      Util.linearTimeIncrementHistogramCounters(mqBaseBuffer, 0, baseBufferCount_, weight, splitPoints, counters);
    }

    long bitPattern = bitPattern_;
    assert bitPattern == n_ / (2L * k_); // internal consistency check
    for (int lvl = 0; bitPattern != 0L; lvl++, bitPattern >>>= 1) {
      weight += weight; // *= 2
      if ((bitPattern & 1L) > 0L) {
        // the levels are already sorted so we can use the fast version
        Util.linearTimeIncrementHistogramCounters(mqLevels, (2+lvl)*k_, k_, weight, splitPoints, counters);
                                               //(mq.mqLevels[lvl], 0, mq.mqK, 
      }
    }
    return counters;
  }
  
  /**
   * Checks the validity of the split points. They must be unique, monotonically increasing and
   * not NaN.
   * @param splitPoints array
   */
  private static void validateSplitPoints(double[] splitPoints) {
    for (int j = 0; j < splitPoints.length - 1; j++) {
      if (splitPoints[j] < splitPoints[j+1]) { continue; }
      throw new IllegalArgumentException(
          "SplitPoints must be unique, monotonically increasing and not NaN.");
    }
  }
  
  /**
   * Computes the number of logarithmic levels needed given k and n.
   * @param k the configured size of the sketch
   * @param n the total values presented to the sketch.
   * @return the number of levels needed.
   */
  private static int computeNumLevelsNeeded(int k, long n) {
    long long2k = ((long) 2 * k);
    long quo = n / long2k;
    if (quo == 0) return 0;
    else return (1 + (hiBitPos (quo)));
  }
  
  /**
   * Computes the number of samples in the sketch given the base buffer count and the bit pattern.
   * @return the number of samples in the sketch
   */
  int numSamplesInSketch() {
    int count = baseBufferCount_;
    long bits = bitPattern_;
    assert bits == n_ / (2L * k_); // internal consistency check
    for ( ; bits != 0L; bits >>>= 1) {
      if ((bits & 1L) > 0L) {
        count += k_;
      }
    }
    return count;
  }

  /**
   * Computes a checksum of all the samples in the sketch. Used in testing the Auxiliary.
   * @return a checksum of all the samples in the sketch
   */
  double sumOfSamplesInSketch() {
    double total = Util.sumOfDoublesInSubArray (combinedBuffer_, 0, baseBufferCount_);
    long bits = bitPattern_;
    assert bits == n_ / (2L * k_); // internal consistency check
    for (int lvl = 0; bits != 0L; lvl++, bits >>>= 1) {
      if ((bits & 1L) > 0L) {
        total += Util.sumOfDoublesInSubArray (combinedBuffer_, ((2+lvl) * k_), k_);
      }
    }
    return total;
  }

  /**
   * Pretty prints summary information about the sketch. Used for debugging
   */
  void show() {
    double[] levelsArr     = combinedBuffer_;
    double[] baseBuffer = combinedBuffer_;
    int numLevels = computeNumLevelsNeeded (k_, n_);
    System.out.printf ("showing: K=%d N=%d levels=%d combinedBufferLength=%d baseBufferCount=%d bitPattern=%d\n",
                       k_, n_, numLevels, allocatedBufferSpace_, baseBufferCount_, bitPattern_);
    for (int i = 0; i < baseBufferCount_; i++) {
      System.out.printf (" %.1f", baseBuffer[i]);
    }
    System.out.printf ("\n");
    System.out.printf ("Levels:\n");
    for (int j = 2*k_; j < allocatedBufferSpace_; j++) {
      if (j % k_ == 0) {
        System.out.printf ("\n");
      }
      System.out.printf ("    %.1f", levelsArr[j]);
    }
    System.out.printf ("\n");
  }

  /**
   * Computes epsilon from K. The following table are examples.
   * <code><pre>
   *           eps      eps from inverted
   *     K   empirical  adjusted formula
   *  -------------------------------------
   *    16   0.121094   0.121454102233560
   *    32   0.063477   0.063586601346532
   *    64   0.033081   0.033169048393679
   *   128   0.017120   0.017248096847308
   *   256   0.008804   0.008944835012965
   *   512   0.004509   0.004627803568920
   *  1024   0.002303   0.002389303789572
   *
   *  these could be used in a unit test
   *  2   0.821714930853465
   *  16   0.12145410223356
   *  1024   0.00238930378957284
   *  1073741824   3.42875166500824e-09
   * </pre></code>
   */
  public static class EpsilonFromK {
    // the following delta was used while crunching down the empirical results
    private static final double deltaForEps = 0.01;  

    // this is a heuristic fudge factor that causes the inverted formula to better match the empirical
    private static final double adjustKForEps = 4.0 / 3.0;  // fudge factor

    // ridiculously fine tolerance given the fudge factor; 1e-3 would probably suffice
    private static final double bracketedBinarySearchForEpsTol = 1e-15; 

    private static double kOfEpsFormula (double eps) {
      return ((1.0 / eps) * (Math.sqrt (Math.log (1.0 / (eps * deltaForEps)))));
    }

    private static boolean epsForKPredicate (double eps, double kf) {
      return (kOfEpsFormula(eps) >= kf);
    }

    private static double bracketedBinarySearchForEps (double kf, double lo, double hi) {
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

    /**
     * Finds the theoretical epsilon given K and a fudge factor >= 1.0
     * @param k The given value of k
     * @param ff The given fudge factor that must be &ge; 1.0
     * @return the resulting epsilon
     */
    public static double findEpsForK (int k, double ff) {
      double kf = k*ff;
      assert (kf >= 2.15); // ensures that the bracketing succeeds
      assert (kf < 1e12);  // ditto, but could actually be bigger
      double lo = 1e-16;
      double hi = 1.0 - 1e-16;
      assert epsForKPredicate(lo, kf);
      assert !epsForKPredicate(hi, kf);
      return (bracketedBinarySearchForEps (kf, lo, hi));
    }

    //also used by test
    /**
     * From extensive empirical testing we recommend most users use this method for deriving 
     * epsilon.
     * @param k the given k
     * @return the resulting epsilon
     */
    public static double adjustedFindEpsForK (int k) {
      assert (k >= 2); // should actually raise invalid input
      // don't need to check in the other direction because an int is very small
      return findEpsForK (k, adjustKForEps);
    }
  } //End of EpsilonFromK
  
} // End of class QuantilesSketch
