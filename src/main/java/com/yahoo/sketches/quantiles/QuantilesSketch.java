/*
 * Copyright 2015, Yahoo! Inc.
 * Licensed under the terms of the Apache License 2.0. See LICENSE file at the project root for terms.
 */
package com.yahoo.sketches.quantiles;

import static com.yahoo.sketches.quantiles.PreambleUtil.BIG_ENDIAN_FLAG_MASK;
import static com.yahoo.sketches.quantiles.PreambleUtil.COMPACT_FLAG_MASK;
import static com.yahoo.sketches.quantiles.PreambleUtil.EMPTY_FLAG_MASK;
import static com.yahoo.sketches.quantiles.PreambleUtil.ORDERED_FLAG_MASK;
import static com.yahoo.sketches.quantiles.PreambleUtil.READ_ONLY_FLAG_MASK;
import static com.yahoo.sketches.quantiles.PreambleUtil.SER_VER;
import static com.yahoo.sketches.quantiles.Util.bufferElementCapacity;

import com.yahoo.sketches.Family;
import com.yahoo.sketches.memory.Memory;

public abstract class QuantilesSketch {
  static final int MIN_BASE_BUF_SIZE = 4; //This is somewhat arbitrary
  
  /**
   * Returns a new builder
   * @return a new builder
   */
  public static final QuantilesSketchBuilder builder() {
    return new QuantilesSketchBuilder();
  }
  
  /** 
   * Updates this sketch with the given double data item
   * @param dataItem an item from a stream of items.  NaNs are ignored.
   */
  public abstract void update(double dataItem);
  
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
  public abstract double getQuantile(double fraction);
  
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
  public abstract double[] getQuantiles(double[] fractions);
  
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
  public abstract double getNormalizedRankError();
  
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
  public abstract double[] getPDF(double[] splitPoints);
  
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
  public abstract double[] getCDF(double[] splitPoints);
  
  //Internal parameters
  
  /**
   * Returns the configured value of K
   * @return the configured value of K
   */
  public abstract int getK();
  
  /**
   * Returns the length of the input stream so far.
   * @return the length of the input stream so far
   */
  public abstract long getN();
  
  /**
   * Returns the min value of the stream
   * @return the min value of the stream
   */
  public abstract double getMinValue();

  /**
   * Returns the max value of the stream
   * @return the max value of the stream
   */
  public abstract double getMaxValue();
  
  /**
   * Returns true if this sketch is empty
   * @return true if this sketch is empty
   */
  public boolean isEmpty() {
   return getN() == 0; 
  }
  
  /**
   * Returns true if this sketch accesses its internal data using the Memory package
   * @return true if this sektch accesses its internal data using the Memory package
   */
  public abstract boolean isDirect();
  
  /**
   * Resets this sketch to a virgin state, but retains the original value of k.
   */
  public abstract void reset();
  
  
  /**
   * Serialize this sketch to a byte array form. 
   * @return byte array of this sketch
   */
  public abstract byte[] toByteArray();

  /**
   * Returns summary information about this sketch.
   */
  @Override
  public String toString() {
    return toString(true, false);
  }
  
  /**
   * Returns summary information about this sketch. Used for debugging.
   * @param sketchSummary if true includes sketch summary
   * @param dataDetail if true includes data detail
   * @return summary information about the sketch.
   */
  public abstract String toString(boolean sketchSummary, boolean dataDetail);
  
  /**
   * Merges the given sketch into this one
   * @param qsSource the given source sketch
   */
  public abstract void merge(QuantilesSketch qsSource);
  
  /**
   * Modifies the source sketch into the target sketch
   * @param qsSource The source sketch
   * @param qsTarget The target sketch
   */
   public abstract void mergeInto(QuantilesSketch qsSource, QuantilesSketch qsTarget);
   
   /**
    * Heapify takes the sketch image in Memory and instantiates an on-heap Sketch, 
    * The resulting sketch will not retain any link to the source Memory.
    * @param srcMem an image of a Sketch.
    * <a href="{@docRoot}/resources/dictionary.html#mem">See Memory</a>
    * @return a heap-based Sketch based on the given Memory
    */
   public static QuantilesSketch heapify(Memory srcMem) {
     return HeapQuantilesSketch.getInstance(srcMem);
   }
   
   /**
    * Computes the number of retained entries (samples) in the sketch
    * @return the number of retained entries (samples) in the sketch
    */
   public int getRetainedEntries() {
     int k =  getK();
     long n = getN();
     int bbCnt = Util.computeBaseBufferCount(k, n);
     long bitPattern = Util.computeBitPattern(k, n);
     int validLevels = Long.bitCount(bitPattern);
     return bbCnt + validLevels*k; 
   }
   
   public int getStorageBytes() {
     if (isEmpty()) return 8;
     return 40 + 8*Util.bufferElementCapacity(getK(), getN());
   }
   
   //restricted
   
   /**
    * Returns the combined buffer reference
    * @return the commbined buffer reference
    */
   abstract double[] getCombinedBuffer();
   
   /**
    * Returns the base buffer count
    * @return the base buffer count
    */
   abstract int getBaseBufferCount();
   
   /**
    * Returns the bit pattern for valid log levels
    * @return the bit pattern for valid log levels
    */
   abstract long getBitPattern();
   
  /**
   * Checks the validity of the given value k
   * @param k must be greater than or equal to 2.
   */
  static void checkK(int k) {
    if (k < MIN_BASE_BUF_SIZE/2) {
      throw new IllegalArgumentException("K must be >= "+(MIN_BASE_BUF_SIZE/2));
    }
  }

  /**
   * Check the validity of the given serialization version
   * @param serVer the given serialization version
   */
  static void checkSerVer(int serVer) {
    if (serVer != SER_VER) {
      throw new IllegalArgumentException(
          "Possible corruption: Invalid Serialization Version: "+serVer);
    }
  }
  
  /**
   * Checks the validity of the given family ID
   * @param familyID the given family ID
   */
  static void checkFamilyID(int familyID) {
    Family family = Family.idToFamily(familyID);
    if (!family.equals(Family.QUANTILES)) {
      throw new IllegalArgumentException(
          "Possible corruption: Invalid Family: " + family.toString());
    }
  }
  
  /**
   * Checks the validity of the memory buffer allocation and the memory capacity assuming
   * n and k.
   * @param k the given value of k
   * @param n the given value of n
   * @param memBufAlloc the memory buffer allocation
   * @param memCapBytes the memory capacity
   */
  static void checkBufAllocAndCap(int k, long n, int memBufAlloc, long memCapBytes) {
    int computedBufAlloc = bufferElementCapacity(k, n);
    if (memBufAlloc != computedBufAlloc) {
      throw new IllegalArgumentException("Possible corruption: Invalid Buffer Allocated Count: "
          + memBufAlloc +" != " +computedBufAlloc);
    }
    int maxPre = Family.QUANTILES.getMaxPreLongs();
    int reqBufBytes = (maxPre + memBufAlloc) << 3;
    if (memCapBytes < reqBufBytes) {
      throw new IllegalArgumentException("Possible corruption: Memory capacity too small: "+ 
          memCapBytes + " < "+ reqBufBytes);
    }
  }
  
  /**
   * Checks the consistency of the flag bits and the state of preambleLong and the memory
   * capacity and returns the empty state.
   * @param preambleLongs the size of preamble in longs 
   * @param flags the flags field
   * @param memCapBytes the memory capacity
   * @return the value of the empty state
   */
  static boolean checkPreLongsFlagsCap(int preambleLongs, int flags, long memCapBytes) {
    boolean empty = (flags & EMPTY_FLAG_MASK) > 0;
    int minPre = Family.QUANTILES.getMinPreLongs();
    int maxPre = Family.QUANTILES.getMaxPreLongs();
    boolean valid = ((preambleLongs == minPre) && empty) || ((preambleLongs == maxPre) && !empty);
    if (!valid) {
      throw new IllegalArgumentException(
          "Possible corruption: PreambleLongs inconsistent with empty state: " +preambleLongs);
    }
    checkFlags(flags);
    if (!empty && (memCapBytes < (maxPre<<3))) {
      throw new IllegalArgumentException(
          "Possible corruption: Insufficient capacity for preamble: " +memCapBytes);
    }
    return empty;
  }
  
  /**
   * Checks just the flags field of the preamble
   * @param flags the flags field
   */
  static void checkFlags(int flags) {
    int flagsMask = ORDERED_FLAG_MASK | COMPACT_FLAG_MASK | READ_ONLY_FLAG_MASK | BIG_ENDIAN_FLAG_MASK;
    if ((flags & flagsMask) > 0) {
      throw new IllegalArgumentException(
          "Possible corruption: Input srcMem cannot be: big-endian, compact, ordered, or read-only");
    }
  }
  
  /**
   * Checks the validity of the split points. They must be unique, monotonically increasing and
   * not NaN.
   * @param splitPoints array
   */
  static final void validateSplitPoints(double[] splitPoints) {
    for (int j = 0; j < splitPoints.length - 1; j++) {
      if (splitPoints[j] < splitPoints[j+1]) { continue; }
      throw new IllegalArgumentException(
          "SplitPoints must be unique, monotonically increasing and not NaN.");
    }
  }

}
