/*
 * Copyright 2016, Yahoo! Inc. Licensed under the terms of the Apache License 2.0. See LICENSE file
 * at the project root for terms.
 */

package com.yahoo.sketches.hashmaps;

public class HashMapWithEfficientDeletes extends HashMap {

  public HashMapWithEfficientDeletes(final int capacity) {
    super(capacity);
  }

  @Override
  public long get(final long key) {
    final int probe = hashProbe(key);
    if (states[probe] > 0) {
      assert (keys[probe] == key);
      return values[probe];
    }
    return 0;
  }

  @Override
  public boolean isActive(final int probe) {
    return (states[probe] > 0);
  }

  @Override
  public void adjustOrPutValue(final long key, final long adjustAmount, final long putAmount) {
    int probe = (int) hash(key) & arrayMask;
    short drift = 1;
    while (states[probe] != 0 && keys[probe] != key) {
      probe = (probe + 1) & arrayMask;
      drift++;
    }

    if (states[probe] == 0) {
      // adding the key to the table the value
      assert (size < capacity);
      keys[probe] = key;
      values[probe] = putAmount;
      states[probe] = drift;
      size++;
    } else {
      // adjusting the value of an existing key
      assert (keys[probe] == key);
      values[probe] += adjustAmount;
    }
  }

  @Override
  public void keepOnlyLargerThan(final long thresholdValue) {
    for (int probe = 0; probe < length; probe++) {
      if (states[probe] > 0 && values[probe] <= thresholdValue) {
        hashDelete(probe);
        probe--;
        size--;
      }
    }
  }

  private int hashProbe(final long key) {
    int probe = (int) hash(key) & arrayMask;
    while (states[probe] > 0 && keys[probe] != key) {
      probe = (probe + 1) & arrayMask;
    }
    return probe;
  }

  private void hashDelete(int deleteProbe) {
    // Looks ahead in the table to search for another
    // item to move to this location
    // if none are found, the status is changed
    states[deleteProbe] = 0;
    short drift = 1;
    int probe = (deleteProbe + drift) & arrayMask;
    // advance until you find a free location replacing locations as needed
    while (states[probe] != 0) {
      if (states[probe] > drift) {
        // move current element
        keys[deleteProbe] = keys[probe];
        values[deleteProbe] = values[probe];
        states[deleteProbe] = (short) (states[probe] - drift);
        // marking this location as deleted
        states[probe] = 0;
        drift = 0;
        deleteProbe = probe;
      }
      probe = (probe + 1) & arrayMask;
      drift++;
    }
  }
}
