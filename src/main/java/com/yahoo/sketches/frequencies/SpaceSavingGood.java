/*
 * Copyright 2016, Yahoo! Inc. Licensed under the terms of the Apache License 2.0. See LICENSE file
 * at the project root for terms.
 */

package com.yahoo.sketches.frequencies;

import gnu.trove.map.hash.TLongIntHashMap;

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
public class SpaceSavingGood {

  // queue will store counters and their associated keys
  // for fast access to smallest counter.
  // counts will also store counters and their associated
  // keys to quickly check if a key is currently assigned a counter.

  private TLongIntHashMap heap_indices;
  private long[] keys;
  private long[] counts;
  private double errorTolerance;
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
  public SpaceSavingGood(double errorTolerance) {
    this.errorTolerance = errorTolerance;
    // make sure maxSize is odd to ensure that all heap nodes either have
    // two children or no children
    this.maxSize = ((int) (1.0 / errorTolerance) + 1) | 1;
    this.heap_indices = new TLongIntHashMap(maxSize);
    // keys and counts will be indexed from 1, to maintain easy min-heap arithmetic
    // i.e., children of index i will be indices 2*i and 2*i+1
    this.keys = new long[maxSize + 1];
    this.counts = new long[maxSize + 1];
    this.mergeError = 0;
    this.stream_length = 0;
  }

  /**
   * @param index pre-condition: this.keys satisfies min-heap property (parent is always at most its
   *        children) except possibly at location index. post-condition: min-heap property is
   *        restored.
   */
  void Heapify(int index) { // restore the heap condition in case it has been violated
    long tmp_key;
    long tmp_val;
    int minchild_index;

    int moved = 0;
    while (true) {
      // if the current node has no children then we are done
      if ((index << 1) + 1 > maxSize) {
        if (moved == 1)
          heap_indices.put(keys[index], index);

        break;
      }

      // compute which child is the lesser of the two
      minchild_index = (index << 1);
      if ((minchild_index + 1) <= maxSize) {
        if (counts[minchild_index + 1] < counts[minchild_index])
          minchild_index++;
      } else {
        System.out.println("WTF!!!!\n");
        System.exit(1);
      }

      // if the parent is less than the smallest child, we can stop
      if (counts[index] <= counts[minchild_index]) {
        if (moved == 1)
          heap_indices.put(keys[index], index);
        break;
      }

      // else, swap the parent and child in the heap
      tmp_key = keys[index];
      tmp_val = counts[index];
      counts[index] = counts[minchild_index];
      keys[index] = keys[minchild_index];

      // update the heap index associated with the just-moved key
      heap_indices.put(keys[index], index);

      keys[minchild_index] = tmp_key;
      counts[minchild_index] = tmp_val;

      // continue on with the heapify from the child position
      index = minchild_index;
      moved = 1;
    }
  }

  /**
   * Process a key (specified as a long) update and treat the increment as 1
   * 
   * @param key key whose frequency should be incremented
   */

  public void update(long key) {
    update(key, 1);
  }

  /**
   * Process a key (specified as a long) and a non-negative increment.
   * 
   * @param key A key specified as a long, whose frequency should be incremented
   * @param increment The amount by which to increment the frequency of key
   */

  public void update(long key, long increment) {
    if (increment <= 0)
      throw new IllegalArgumentException("Received negative or zero value for increment.");

    this.stream_length += increment;

    int index;

    // if key is already assigned a counter
    if (heap_indices.containsKey(key)) {
      index = heap_indices.get(key);
      counts[index] += increment;
      Heapify(index);
      // update count of key in hash table
    } else {
      // if key not already assigned a counter,
      // assign the smallest counter to the key
      // note: if heap is not full, smallest counter will just be 0
      // (with no corresponding entry in heap_indices)

      if (counts[1] != 0)
        heap_indices.remove(keys[1]);

      keys[1] = key;
      heap_indices.put(key, 1);
      counts[1] += increment;
      Heapify(1);
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

    if (heap_indices.containsKey(key))
      return counts[heap_indices.get(key)];
    else
      return 0;
  }

  /**
   * @param key whose count estimate is returned.
   * @return an upper bound on the count for the key.
   */

  public long getEstimateUpperBound(long key) {
    return (getEstimate(key) + mergeError + counts[1]);
  }

  /**
   * @param key whose count estimate is returned.
   * @return a lower bound on the count for the key.
   */

  public long getEstimateLowerBound(long key) {

    if ((getEstimate(key) - counts[1] - mergeError) < 0)
      return 0;

    return (getEstimate(key) - counts[1] - mergeError);
  }

  /**
   * @return the maximal error of the estimate one gets from getEstimate(key). If the real count is
   *         realCount(key) then get(key) + getMaxError() >= realCount(key) >= get(key) -
   *         getMaxError().
   */

  public long getMaxError() {
    return counts[1] + mergeError;
  }

  /**
   * @return the number of positive counters in the sketch.
   */
  public long nnz() {
    return heap_indices.size();
  }

  /**
   * Merge two SpaceSavingGood Sketches. This method does not create a new sketch. The sketch whose
   * function is executed is changed.
   * 
   * @param other Another SpaceSavingGood sketch. Potentially of different size.
   * @return pointer to the sketch resulting in adding the approximate counts of another sketch.
   */

  public SpaceSavingGood merge(SpaceSavingGood other) {
    this.stream_length += other.stream_length;
    this.mergeError += other.getMaxError();

    for (int i = other.maxSize; i >= 1; i--) {
      if (other.counts[i] > 0)
        this.update(other.keys[i], other.counts[i]);
    }
    return this;
  }


  public long[] getFrequentKeys() {
    int count = 0;
    long threshold = (long) (this.stream_length * this.errorTolerance);

    for (int i = maxSize; i >= 1; i--) {
      if (counts[i] >= threshold)
        count++;
    }


    long[] freq_keys = new long[count];
    count = 0;
    for (int i = maxSize; i >= 1; i--) {
      if (counts[i] >= threshold) {
        freq_keys[count] = keys[i];
        count++;
      }
    }

    return freq_keys;
  }
}
