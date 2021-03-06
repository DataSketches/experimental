/*
 * Copyright 2016, Yahoo! Inc. Licensed under the terms of the Apache License 2.0. See LICENSE file
 * at the project root for terms.
 */

package com.yahoo.sketches.hashmaps;

/**
 * Classic Kunth Open Address Double Hash
 *
 * @author Edo Liberty
 */
public class HashMapDoubleHashingWithRebuilds extends HashMap {

  private static final int STRIDE_HASH_BITS = 7;
  static final int STRIDE_MASK = (1 << STRIDE_HASH_BITS) - 1;
  int logLength;

  public HashMapDoubleHashingWithRebuilds(final int capacity) {
    super(capacity);
    logLength = Integer.numberOfTrailingZeros(length);
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
    // if (value < 0) throw new IllegalArgumentException("adjust received negative value.");
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

  /**
   * @param key to search for in the array
   * @return returns the location of the key in the array or the first possible place to insert it.
   */
  private int hashProbe(final long key) {
    final long hash = hash(key);
    // make odd and independent of the probe:
    final int stride = (2 * (int) ((hash >> logLength) & STRIDE_MASK)) + 1;
    int probe = (int) (hash & arrayMask);
    // search for duplicate or zero
    while (keys[probe] != key && states[probe] != 0) {
      probe = (probe + stride) & arrayMask;
    }
    return probe;
  }

  @Override
  public void keepOnlyLargerThan(final long thresholdValue) {
    final HashMapDoubleHashingWithRebuilds rebuiltHashMap =
        new HashMapDoubleHashingWithRebuilds(capacity);
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
}
