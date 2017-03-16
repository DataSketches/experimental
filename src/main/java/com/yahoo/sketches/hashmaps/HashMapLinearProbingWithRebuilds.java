/*
 * Copyright 2016, Yahoo! Inc. Licensed under the terms of the Apache License 2.0. See LICENSE file
 * at the project root for terms.
 */

package com.yahoo.sketches.hashmaps;

/**
 * Classic Knuth Open Address Single Hash (linear probing)
 */
public class HashMapLinearProbingWithRebuilds extends HashMap {

  public HashMapLinearProbingWithRebuilds(final int capacity) {
    super(capacity);
  }

  @Override
  public boolean isActive(final int probe) {
    return (states[probe] > 0);
  }

  @Override
  public long get(final long key) {
    final int probe = hashProbe(key);
    return (states[probe] > 0) ? values[probe] : 0;
  }

  @Override
  public void adjustOrPutValue(final long key, final long adjustAmount, final long putAmount) {
    final int probe = hashProbe(key);
    if (states[probe] == 0) {
      keys[probe] = key;
      values[probe] = putAmount;
      states[probe] = 1;
      size++;
    } else {
      assert (keys[probe] == key);
      values[probe] += adjustAmount;
    }
  }

  @Override
  public void keepOnlyLargerThan(final long thresholdValue) {
    final HashMapLinearProbingWithRebuilds rebuiltHashMap =
        new HashMapLinearProbingWithRebuilds(capacity);
    for (int i = 0; i < length; i++) {
      if (states[i] > 0 && values[i] > thresholdValue) {
        rebuiltHashMap.adjustOrPutValue(keys[i], values[i], values[i]);
      }
    }
    System.arraycopy(rebuiltHashMap.keys, 0, keys, 0, length);
    System.arraycopy(rebuiltHashMap.values, 0, values, 0, length);
    System.arraycopy(rebuiltHashMap.states, 0, states, 0, length);
    size = rebuiltHashMap.getSize();
  }

  /**
   * @param key to search for in the array
   * @return returns the location of the key in the array or the first possible place to insert it.
   */
  private int hashProbe(final long key) {
    final long hash = hash(key);
    int probe = (int) (hash) & arrayMask;
    while (keys[probe] != key && states[probe] != 0) {
      probe = (probe + 1) & arrayMask;
    }
    return probe;
  }

}
