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
public class MergeableQuantileSketch {

  /**
   * Parameter that controls space usage of sketch and accuracy of estimates.
   */
  private int mqK;

  /**
   * Total number of data items in the stream so far. (Uniqueness plays no role in these sketches).
   */
  private long mqN;

  /**
   * Each level is either null or a buffer of length K that is completely full and sorted.
   * Note: in the README file these length K buffers are called "mini-sketches".
   */
  private double[][] mqLevels; 

  /**
   * The base buffer has length 2*K but might not be full and isn't necessarily sorted.
   */
  private double[] mqBaseBuffer; 

  /**
   * Number of samples currently in base buffer
   */
  private int mqBaseBufferCount; 

  /**
   * Constructs a Mergeable Quantile Sketch of double elements.
   * @param k Parameter that controls space usage of sketch and accuracy of estimates
   */
  public MergeableQuantileSketch(int k) {
    assert k >= 1;
    mqK = k;
    mqN = 0;
    mqLevels = new double[0][];
    mqBaseBuffer = new double[Math.min(4,2*k)]; //The 4 is somewhat arbitrary; the min is important
    mqBaseBufferCount = 0;
  }

  /** 
   * Updates this sketch with the given double data item
   * @param dataItem an item from a stream of items
   */
  public void update(double dataItem) {

    if (Double.isNaN(dataItem)) return;

    if (mqBaseBufferCount+1 > mqBaseBuffer.length) {
      this.growBaseBuffer();
    } 

    mqBaseBuffer[mqBaseBufferCount++] = dataItem;
    mqN++;

    if (mqBaseBufferCount == 2*mqK) {
      this.processFullBaseBuffer();
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
  public static void mergeInto(MergeableQuantileSketch mqTarget, MergeableQuantileSketch mqSource) {  

    if ( mqTarget.mqK != mqSource.mqK) throw new IllegalArgumentException(
        "Given sketches must have the same value of k.");

    int k = mqTarget.mqK;

    long nFinal = mqTarget.mqN + mqSource.mqN; 

    double [] sourceBaseBuffer = mqSource.mqBaseBuffer;
    for (int i = 0; i < mqSource.mqBaseBufferCount; i++) {
      mqTarget.update(sourceBaseBuffer[i]);
    }
    // note: the above updates have already changed mqTarget in many ways, 
    //   but it might still need an additional buffer level

    int numLevelsNeeded = computeNumLevelsNeeded(k, nFinal);
    if (numLevelsNeeded > mqTarget.mqLevels.length) {
      mqTarget.growLevels(numLevelsNeeded);
    }

    for (int lvl = 0; lvl < mqSource.mqLevels.length; lvl++) {
      double [] buf = mqSource.mqLevels[lvl]; 
      if (buf != null) {
        mqTarget.propagateCarry(buf, lvl);
      }
    }

    mqTarget.mqN = nFinal;
  }

  /*
    Note: a two-way merge that doesn't modify either of its
    two inputs could be implemented by making a deep copy of
    the larger sketch and then merging the smaller one into it.
    However, it was decided not to do this.
  */

  /**
   * <p>
   * This returns an approximation to the value of the data item
   * that would be preceded by the given fraction of a hypothetical sorted
   * version of the input stream so far.
   * <p>
   * We note that this method has a fairly large overhead (microseconds instead of nanoseconds)
   * so it should not be called multiple times to get different quantiles from the same
   * sketch. Instead use getQuantiles(). which pays the overhead only once.
   * 
   * @param fraction the specified fractional position in the hypothetical sorted stream.
   * 
   * @return the approximation to the value at the above fraction
   */
  public double getQuantile(double fraction) {
    Auxiliary au = this.constructAuxiliary ();
    return (au.getQuantile (fraction));
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
    Auxiliary au = this.constructAuxiliary ();
    double [] answers = new double [fractions.length];
    for (int i = 0; i < fractions.length; i++) {
      answers[i] = au.getQuantile (fractions[i]);
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
  public double [] getPDF(double [] splitPoints) {
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
    assert (subtotal == this.mqN);
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
    assert (subtotal == this.mqN);
    return result;
  }
  
  public double getNormalizedCountError() {
    return 0;  //a function of k table lookup.
  }

  /**
   * Returns the length of the input stream so far.
   * @return the length of the input stream so far
   */
  public long getStreamLength() {
    return (mqN);
  }
  
  //Restricted methods
  
  Auxiliary constructAuxiliary() {
    Auxiliary au = Auxiliary.constructAuxiliary(
        this.mqK, this.mqN, this.mqLevels, this.mqBaseBuffer, this.mqBaseBufferCount);
    return au;
  }

  private void growBaseBuffer() {
    int oldSize = mqBaseBuffer.length;
    int newSize = Math.max (Math.min (2*mqK, 2*oldSize), 1);
    double[] newBuf = new double[newSize];
    for (int i = 0; i < oldSize; i++) {
      newBuf[i] = mqBaseBuffer[i];
    }
    mqBaseBuffer = newBuf;
  }

  private void growLevels(int newSize) {
    int oldSize = mqLevels.length;
    assert (newSize > oldSize);
    double[][] newLevels = new double[newSize][];
    for (int i = 0; i < oldSize; i++) {
      newLevels[i] = mqLevels[i];
    }
    for (int i = oldSize; i < newSize; i++) {
      newLevels[i] = null;
    } 
    mqLevels = newLevels;
  }


  /**
   * Propogate the given buffer of k elements, which must be full and sorted, into the given level.
   * This buffer must be the output of the overall algorithm up to this given level.
   * It is the caller's responsibility to ensure that the levels array is big enough so that 
   * this provably cannot fail. 
   * Also, while this tail-recursive procedure might cause logarithmic stack growth in java, 
   * that in itself shouldn't be a problem, nor would recoding it provide a significant speed-up, 
   * since it is not the inner loop of the algorithm
   * @param carryIn the buffer that is being propogated into the specified level
   * @param curLvl the specified level
   */
  private void propagateCarry(double[] carryIn, int curLvl) {
    if (mqLevels[curLvl] == null) {// propagation stops here
      mqLevels[curLvl] = carryIn;
    }
    else {
      double [] oldBuf = mqLevels[curLvl];
      mqLevels[curLvl] = null;
      double [] carryOut = allocatingMergeTwoSizeKBuffers (carryIn, oldBuf, mqK);
      propagateCarry(carryOut, curLvl+1);
    }
  }

  /**
   * Called when the base buffer has just acquired 2*k elements.
   */
  private void processFullBaseBuffer() {
    assert mqBaseBufferCount == 2 * mqK;

    // make sure there will be enough levels for the propagation 
    int numLevelsNeeded = computeNumLevelsNeeded (mqK, mqN);
    if (numLevelsNeeded > mqLevels.length) {
      this.growLevels(numLevelsNeeded);
    }

    // now we construct a new length-k "carry" buffer, and propagate it 
    Arrays.sort(mqBaseBuffer, 0, mqBaseBufferCount);
    double[] carry = allocatingCarryOfOneSize2KBuffer(mqBaseBuffer, mqK);
    mqBaseBufferCount = 0;
    propagateCarry(carry, 0);
  }
  
  /**
   * Shared algorithm for both PDF and CDF functions. The splitPoints must be unique, monotonically
   * increasing values.
   * @param splitPoints an array of <i>m</i> unique, monotonically increasing doubles
   * that divide the real number line into <i>m+1</i> consecutive disjoint intervals.
   * @return the unnormalized, accumulated counts of <i>m + 1</i> intervals.
   */
  private long[] internalBuildHistogram (double[] splitPoints) {
    MergeableQuantileSketch mq = this;
    validateSplitPoints(splitPoints);
    int numSplitPoints = splitPoints.length;
    int numCounters = numSplitPoints + 1;
    long[] counters = new long[numCounters];
    for (int j = 0; j < numCounters; j++) { counters[j] = 0; } // already true, right?
    long weight = 1;
    if (numSplitPoints < 50) { // empirically determined crossover for K = 200
      // not worth it to sort when few split points
      Util.quadraticTimeIncrementHistogramCounters(mq.mqBaseBuffer, mq.mqBaseBufferCount, weight,
          splitPoints, counters);
    }
    else {
      Arrays.sort(mq.mqBaseBuffer, 0, mq.mqBaseBufferCount); // sort is worth it when many split points
      Util.linearTimeIncrementHistogramCounters (mq.mqBaseBuffer, mq.mqBaseBufferCount, weight,
          splitPoints, counters);
    }
    for (int lvl = 0; lvl < mq.mqLevels.length; lvl++) { 
      weight += weight; // *= 2
      if (mq.mqLevels[lvl] != null) { 
        // the levels are already sorted so we can use the fast version
        Util.linearTimeIncrementHistogramCounters(mq.mqLevels[lvl], mq.mqK, weight, 
            splitPoints, counters);
      }
    }
    return counters;
  }
  
  // 
  /**
   * The zip of the merge / zip alternating algorithm.
   * @param inbuf must be sorted and of length 2*k. 
   * @param k the k value of the sketch
   * @return a new sorted buffer of length k
   */
  private static double[] allocatingCarryOfOneSize2KBuffer(double [] inbuf, int k) {
    int randomOffset = (int) (2.0 * Math.random());
    assert randomOffset == 0 || randomOffset == 1;
    double[] outbuf = new double [k];
    for (int i = 0; i < k; i++) {
      outbuf[i] = inbuf[2*i + randomOffset];
    }
    return (outbuf);
  }
  /**
   * Performs both a merge and a zip of the given buffers
   * @param bufA One side of the merge. Must be sorted and of length k.
   * @param bufB The second side of the merge. Must be sorted and of length k.
   * @param k the k of the sketch
   * @return a new sorted buffer of length k
   */
  private static double[] allocatingMergeTwoSizeKBuffers(double[] bufA, double[] bufB, int k) {
    assert bufA.length == k;
    assert bufB.length == k;
    int tmpLen = 2 * k;
    double[] tmpBuf = new double [tmpLen];

    int a = 0;
    int b = 0;
    int j = 0;

    while (a < k && b < k) {
      if (bufB[b] < bufA[a]) {
        tmpBuf[j++] = bufB[b++];
      }
      else {
        tmpBuf[j++] = bufA[a++];
      }
    }
    if (a < k) {
      System.arraycopy(bufA, a, tmpBuf, j, k - a); 
    }
    else {
      assert b < k;
      System.arraycopy(bufB, b, tmpBuf, j, k - b);
    }

    return allocatingCarryOfOneSize2KBuffer(tmpBuf,k);
  }
  
  // Note: there is a comment in the increment histogram counters code that says that the 
  // splitpoints must be unique. However, the end to end test could generate duplicate splitpoints.
  // Need to resolve this apparent conflict.
  private static void validateSplitPoints(double[] splitPoints) {
    for (int j = 0; j < splitPoints.length - 1; j++) {
      if (splitPoints[j] >= splitPoints[j+1]) { //checks for uniqueness and order
        throw new IllegalArgumentException(
            "SplitPoints must be unique and monotonically increasing.");
      }
    }
  }
  

  private static int computeNumLevelsNeeded(int k, long n) {
    long long2k = ((long) 2 * k);
    long quo = n / long2k;
    if (quo == 0) return 0;
    else return (1 + (hiBitPos (quo)));
  }
  
  // Code leveraged during testing 
  
  int getK() { 
    return mqK; 
  }
  
  long getN() { 
    return mqN; 
  }
  
  int numSamplesInSketch() {
    int count = this.mqBaseBufferCount;
    for (int lvl = 0; lvl < this.mqLevels.length; lvl++) {
      if (this.mqLevels[lvl] != null) { count += this.mqK; }
    }
    return count;
  }

  double sumOfSamplesInSketch() {
    double total = Util.sumOfDoublesInArrayPrefix(this.mqBaseBuffer, this.mqBaseBufferCount);
    for (int lvl = 0; lvl < this.mqLevels.length; lvl++) {
      if (this.mqLevels[lvl] != null) { 
        total += Util.sumOfDoublesInArrayPrefix(this.mqLevels[lvl], this.mqK);
      }
    }
    return total;
  }

  static void validateMergeableQuantileSketchStructure(MergeableQuantileSketch mq, int k, long n) {
    long long2k = 2L * k;
    long quotient = n / long2k;
    int remainder = (int) (n % long2k);
    assert mq.mqBaseBufferCount == remainder;
    int numLevels = 0;
    if (quotient > 0) { numLevels = (1 + (hiBitPos(quotient))); }
    assert mq.mqLevels.length == numLevels;
    int level;
    long bitPattern;
    for (level = 0, bitPattern = quotient; level < numLevels; level++, bitPattern >>>= 1) {
      if ((bitPattern & 1) == 0) {
        assert mq.mqLevels[level] == null;
      }
      else if ((bitPattern & 1) == 1) {
        assert mq.mqLevels[level] != null; 
        double [] thisBuf = mq.mqLevels[level];
        for (int i = 0; i < thisBuf.length - 1; i++) {
          assert thisBuf[i] <= thisBuf[i+1];
        }
      }
      else {
        assert false;
      }
    }
  }
} // End of class MergeableQuantileSketch


//  Discussion of structure sharing and modification of buffer contents.

// We are currently using a scheme whose "zip" operation always creates a 
// fresh level buffer, whose contents are never modified afterwards.

// This means that nothing bad can happen even though the current mergeInto code can cause
// the source and target sketches to both contain pointers to the exact same level buffer.

// There is a different scheme, which we are current NOT using because it is slightly more
// complicated and dangerous, that re-uses and therefore modifies level buffers as each
// carry wave propagates. If we ever switched to that scheme, different sketches could 
// not be allowed to share level buffers. 

// The fix (which actually isn't all that complicated) would be the following:
// (1) Keep track of which sketch owns each level buffer during a multi-sketch carry propagation.
// (2) Never modify a level buffer that belongs to a different sketch; 
//     instead modify the "other" buffer that is involved in the buffer merge.
// (3) Never store a pointer to a level buffer that belongs to a different sketch;
//     instead copy the level buffer.
