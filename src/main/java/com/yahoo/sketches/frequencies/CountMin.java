/*
 * Copyright 2015, Yahoo! Inc. Licensed under the terms of the Apache License 2.0. See LICENSE file
 * at the project root for terms.
 */
package com.yahoo.sketches.frequencies;

import com.yahoo.sketches.hash.MurmurHash3;

/**
 * The Count-Min sketch of Cormode and Muthukrishnan is useful for approximately answering point
 * queries, i.e., queries of the form "what is the frequency of key i"? It can also answer other
 * queries as well (range queries, inner product queries, heavy hitters, quantiles, etc.), though it
 * incurs significant overheads for some of these other queries.
 * 
 * 
 * @author Justin8712
 */


// @SuppressWarnings("cast")
public class CountMin {

  private int rows;
  private int columns;
  private long update_sum;
  private long[] counts;
  private long[] keyArr = new long[1];
  double eps;


  /**
   * Constructs and initializes a CountMin sketch. The guarantee of the sketch is that the answer
   * returned to any individual point query will, with probability at least 1-delta, be accurate to
   * error plus or minus eps*F, where F is the sum of all the increments the sketch has processed.
   * 
   * @param eps Estimates are guaranteed to have error eps*n with probability at least 1-delta,
   *        where n is sum of item frequencies
   * @param delta Estimates are guaranteed to have error eps*n with probability at least 1-delta,
   *        where n is sum of item frequencies
   */
  public CountMin(double eps, double delta) {
    if (eps <= 0 || delta <= 0) {
      throw new IllegalArgumentException("Received negative or zero value for eps or delta.");
    }
    this.eps = eps;
    this.rows = (int) (Math.ceil(Math.log(1 / delta) / Math.log(2.0)));
    this.columns = (int) (2 * Math.ceil(1 / eps));
    counts = new long[rows * columns];
    for (int i = 0; i < rows * columns; i++) {
      counts[i] = 0;
    }
    this.update_sum = 0;
  }

  /**
   * Process a key (specified as a long) update and treat the increment as 1
   * 
   * @param key A key specified as a long.
   */
  public void update(long key) {
    update(key, 1);
  }

  /**
   * Process a key (specified as a long) update and treat the increment as 1
   * 
   * @param key A key specified as a long.
   */

  public void conservative_update(long key) {
    conservative_update(key, 1);
  }


  /**
   * Process a key (specified as a long) and an increment (can be negative).
   * 
   * @param key A key specified as a long.
   * @param increment amount by which to increment frequency of key
   */
  public void update(long key, long increment) {
    this.update_sum += increment;
    for (int i = 0; i < this.rows; i++) {
      int index = indexForKey(key, i);
      counts[index] += increment;
    }
  }

  /**
   * Process a key (specified as a long) and an increment (can be negative).
   * 
   * @param key A key specified as a long
   * @param increment amount by which to increment frequency of key.
   */
  public void conservative_update(long key, long increment) {
    this.update_sum += increment;
    long min_count = Long.MAX_VALUE;
    for (int i = 0; i < this.rows; i++) {
      int index = indexForKey(key, i);
      if (counts[index] < min_count) {
        min_count = counts[index];
      }
    }
    for (int i = 0; i < this.rows; i++) {
      int index = indexForKey(key, i);
      if (counts[index] < min_count + increment) {
        counts[index] = min_count + increment;
      }
    }
  }

  /**
   * Returns the index of the i'th cell in the sketch that key maps to
   */
  private int indexForKey(long key, int i) {
    keyArr[0] = key;
    return columns * i + (((int) (MurmurHash3.hash(keyArr, i)[0])) >>> 1) % columns;
  }

  /**
   * Determine an estimate for the frequency of key, specified as a long. It is guaranteed that with
   * probability at least 1-delta 1) getEstimate(key) >= real count 2) getEstimate(key) <= real
   * count + getMaxError()
   * 
   * @param key whose count estimate is returned.
   * @return the approximate count for the key.
   */
  public long getEstimate(long key) {
    keyArr[0] = key;
    long min_count = Long.MAX_VALUE;
    for (int i = 0; i < this.rows; i++) {
      int index = indexForKey(key, i);
      if (counts[index] < min_count) {
        min_count = counts[index];
      }
    }
    return min_count;
  }

  /**
   * Determine an estimate for the frequency of key, specified as a long. It is guaranteed that with
   * probability at least 1-delta 1) getEstimateUpperBound(key) >= real count 2)
   * getEstimateUpperBound(key) <= real count + getMaxError()
   * 
   * @param key whose count estimate is returned.
   * @return the approximate count for the key.
   */
  public long getEstimateUpperBound(long key) {
    return getEstimate(key);
  }

  /**
   * Determine an estimate for the frequency of key, specified as a long. It is guaranteed that with
   * probability at least 1-delta 1) getEstimateLowerBound(key) >= real count - getMaxError() 2)
   * getEstimateLowerBound(key) >= real count
   * 
   * @param key whose count estimate is returned.
   * @return an approximate count for the key.
   * 
   */
  public long getEstimateLowerBound(long key) {
    return getEstimate(key) - getMaxError();
  }

  /**
   * Returns a bound on the error of any estimate returned by the sketch (the bound holds for each
   * estimate with probability at least 1-delta).
   * 
   * Note that the error is one sided. If the real count is realCount(key) then getEstimate(key) >=
   * realCount(key). The guarantee of the sketch is that, for any fixed key, with probability at
   * least 1-delta, realCount(key) is also at most get(key) + getMaxError()
   * 
   * @return a bound on the error of the estimate one gets from getEstimate(key).
   */
  public long getMaxError() {
    return (long) (Math.ceil(this.eps * this.update_sum));
  }


  /**
   * Merges two CountMin sketches, returning pointer to the resulting sketch.
   * 
   * @param other Another CountMin sketch. Must have been created using the same hash functions
   *        (i.e., same seed to MurmurHash) and have the same parameter values eps, delta.
   * @return pointer to the sketch resulting in adding the approximate counts of another sketch.
   *         This method does not create a new sketch. The sketch whose function is executed is
   *         changed.
   */
  public CountMin merge(CountMin other) {
    if (this.rows != other.rows || this.columns != other.columns) {
      throw new IllegalArgumentException(
          "Trying to merge two CountMin data structures of different sizes.");
    }
    for (int i = 0; i < rows * columns; i++) {
      this.counts[i] += other.counts[i];
    }
    this.update_sum += other.update_sum;
    return this;
  }
}
