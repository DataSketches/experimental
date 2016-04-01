/*
 * Copyright 2016, Yahoo! Inc. Licensed under the terms of the Apache License 2.0. See LICENSE file
 * at the project root for terms.
 */

package com.yahoo.sketches.frequencies;

import java.util.PriorityQueue;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * The Space Saving algorithm is useful for keeping approximate counters for keys (map from key
 * (long) to value (long)). The data structure is initialized with a maximal size parameter s. The
 * sketch will keep at most s counters at all times.
 * 
 * When the data structure is updated with a key k and a positive increment delta, the counter
 * assigned to k is incremented by delta, if there is no counter for k and fewer than s counters are
 * in use, a new counter is created and assigned to k. If s counters are already in use, then the
 * smallest counter is re-assigned to k, and this counter is incremented (so the data structure
 * "pretends" that the smallest counter was actually already tracking k).
 * 
 * The guarantee of the algorithm is that 1) The estimate from the sketch is never smaller than the
 * real count. 2) The estimate from the sketch is never larger than the real count plus the
 * guaranteed error bound. 3) The guaranteed error bound is at most F/s, where F is the sum of all
 * the increments.
 * 
 * Background: Space Saving was described in
 * "Efficient Computation of Frequent and Top-k Elements in Data Streams", by Metwally, Agrawal,
 * Abbadi, 2006.
 * 
 * "Space-optimal Heavy Hitters with Strong Error Bounds" by Berinde, Cormode, Indyk, and Strauss
 * proved the tighter error bounds for Space Saving than the F/s bound mentioned in 3) above and
 * used in this code. They proved error bounds in terms of F^{res(t)} i.e. the sum of the counts of
 * all stream items except the t largest.
 * 
 * "Methods for Finding Frequent Items in Data Streams" by Cormode and Hadjieleftheriou performed an
 * experimental comparison of frequent items algorithm, and found Space Saving to perform well.
 * 
 * "Mergeable Summaries" by Agarwal, Cormode, Huang, Phillips, Wei, and Yi showed that FrequentItems
 * aka Misra-Gries is equivalent to Space Saving in a formal sense.
 * 
 * @author Justin8712
 */


// @SuppressWarnings("cast")
public class SpaceSaving {

  // queue will store counters and their associated keys
  // for fast access to smallest counter.
  // counts will also store counters and their associated
  // keys to quickly check if a key is currently assigned a counter.

  private PriorityQueue<Pair> queue;
  private HashMap<Long, Long> counts;
  private int maxSize;
  private long mergeError;
  private long stream_length;

  /**
   * Constructs a min-heap-based SpaceSaving sketch. The sketch is guaranteed to (deterministically)
   * return frequency estimates with additive error bounded by errorTolerance*n, where n is the sum
   * of all item frequencies in the stream. Note that the space usage of the sketch is proportional
   * to the inverse of errorTolerance
   * 
   * @param errorTolerance (must be positive). The sketch is guaranteed to (deterministically)
   *        return frequency estimates with additive error bounded by errorTolerance*n, where n is
   *        the sum of all item frequencies.
   */
  public SpaceSaving(double errorTolerance) {
    this.maxSize = (int) (1.0 / errorTolerance) + 1;
    this.queue = new PriorityQueue<Pair>(maxSize);
    this.counts = new HashMap<Long, Long>(maxSize);
    this.mergeError = 0;
    this.stream_length = 0;

    // System.out.format("maxSize in SpaceSacing is: %d \n", maxSize);
  }

  /**
   * @param key Process a key (specified as a long) update and treat the increment as 1
   */

  public void update(long key) {
    update(key, 1);
  }

  /**
   * @param key A key (as long) whose frequency is to be incremented. The key cannot be null.
   * @param increment Amount to increment frequency by.
   * 
   */
  public void update(long key, long increment) {
    if (increment <= 0)
      throw new IllegalArgumentException("Received negative or zero value for increment.");

    this.stream_length += increment;

    // if key is already assigned a counter
    if (counts.containsKey(key)) {
      long old_count = counts.get(key);
      long new_count = old_count + increment;
      // update count of key in hash table
      counts.put(key, new_count);

      // update count of key in min-heap by
      // removing it and adding it back in with new count
      queue.remove(new Pair(key, old_count));
      queue.add(new Pair(key, new_count));
    } else {
      // if key not already assigned a counter,
      // and not all counters are used, assign it one
      if (counts.size() < maxSize) {
        counts.put(key, increment);
        queue.add(new Pair(key, increment));
      } else {
        // if all counters are used, assign the smallest counter to the key
        Pair lowest = queue.peek();
        long new_count = lowest.getvalue() + increment;
        counts.remove(lowest.getname());
        counts.put(key, new_count);
        boolean success = queue.remove(lowest);
        if (!success) {
          throw new IllegalArgumentException(
              "Failed to remove an element from priority queue " + "in SpaceSaving algorithm!");
        }
        queue.add(new Pair(key, new_count));
      }
    }
  }

  /**
   * @param key whose count estimate is returned.
   * @return the approximate count for the key. It is guaranteed that 1) get(key) + mergeError >=
   *         real count 2) get(key) <= real count + getMaxError() Note that in the absence of
   *         merging (i.e., if mergeError == 0) then getEstimate returns an upper bound on real
   *         count.
   */

  public long getEstimate(long key) {
    // the logic below returns the count of associated counter if key is tracked.
    // If the key is not tracked and fewer than maxSize counters are in use, 0 is returned.
    // Otherwise, the minimum counter value is returned.

    if (counts.containsKey(key))
      return counts.get(key);
    else
      return 0;
  }

  /**
   * @param key whose count estimate is returned.
   * @return an upper bound on the count for the key.
   */

  public long getEstimateUpperBound(long key) {
    if (counts.size() > 0)
      return (getEstimate(key) + mergeError + queue.peek().getvalue());
    return 0;
  }

  /**
   * @param key whose count estimate is returned.
   * @return a lower bound on the count for the key.
   */

  public long getEstimateLowerBound(long key) {
    if (getEstimate(key) == 0)
      return 0;

    if ((getEstimate(key) - queue.peek().getvalue() - mergeError) < 0)
      return 0;

    return (getEstimate(key) - queue.peek().getvalue() - mergeError);
  }

  /**
   * @return the maximal error of the estimate one gets from getEstimate(key). If the real count is
   *         realCount(key) then get(key) + getMaxError() >= realCount(key) >= get(key) -
   *         getMaxError().
   */

  public long getMaxError() {
    if (counts.size() < maxSize)
      return mergeError;
    else
      return queue.peek().getvalue() + mergeError;
  }

  /**
   * @return the number of positive counters in the sketch.
   */
  public long nnz() {
    return counts.size();
  }

  /**
   * Merge two SpaceSaving Sketches. This method does not create a new sketch. The sketch whose
   * function is executed is changed.
   * 
   * @param other Another SpaceSaving sketch. Potentially of different size.
   * @return pointer to the sketch resulting in adding the approximate counts of another sketch.
   */

  public SpaceSaving merge(SpaceSaving other) {
    this.stream_length += other.stream_length;
    this.mergeError += other.getMaxError();

    for (Map.Entry<Long, Long> entry : other.counts.entrySet()) {
      this.update(entry.getKey(), entry.getValue());
    }
    return this;
  }


  public long[] getFrequentKeys() {
    Collection<Long> keysCollection = counts.keySet();
    int count = 0;
    for (long key : keysCollection) {
      if (getEstimate(key) >= this.stream_length / this.maxSize) {
        count++;
      }
    }

    long[] keys = new long[count];
    int i = 0;
    for (long key : keysCollection) {
      if (getEstimate(key) >= this.stream_length / this.maxSize) {
        keys[i] = key;
        i++;
      }
    }
    return keys;
  }
}
