/*
 * Copyright 2016, Yahoo! Inc. Licensed under the terms of the Apache License 2.0. See LICENSE file
 * at the project root for terms.
 */

package com.yahoo.sketches.hashmaps;


public class HashMapWithImplicitDeletes extends HashMap {

  private final short AVAILABLE_STATE = 0;
  private final short OCCUPIED_STATE = 1;
  private final short DELETED_STATE = 2;
  private int maximalDrift = 0;

  public HashMapWithImplicitDeletes(final int capacity) {
    super(capacity);
  }

  @Override
  public boolean isActive(final int probe) {
    return (states[probe] == 1);
  }

  @Override
  public long get(final long key) {
    final int probe = hashProbe(key);
    return (keys[probe] == key && states[probe] == OCCUPIED_STATE) ? values[probe] : 0;
  }

  @Override
  public void adjustOrPutValue(final long key, final long adjustAmount, final long putAmount) {
    final int probe = hashProbe(key);
    if (states[probe] != OCCUPIED_STATE) {
      assert (size < capacity);
      keys[probe] = key;
      values[probe] = putAmount;
      states[probe] = OCCUPIED_STATE;
      size++;
    } else {
      assert (keys[probe] == key);
      values[probe] += adjustAmount;
    }
  }

  @Override
  public void keepOnlyLargerThan(final long thresholdValue) {
    for (int i = 0; i < length; i++) {
      if (states[i] == OCCUPIED_STATE && values[i] <= thresholdValue) {
        states[i] = DELETED_STATE;
        size--;
      }
    }
  }

  private int hashProbe(final long key) {
    int hash = (int) hash(key) & arrayMask;
    int drift = 0;
    while (states[hash] == OCCUPIED_STATE && keys[hash] != key) {
      hash = (hash + 1) & arrayMask;
      drift++;
    }

    // found either the key or a free spot, return that.
    if (keys[hash] == key || states[hash] == AVAILABLE_STATE) {
      if (drift > maximalDrift) {
        maximalDrift = drift;
      }
      return hash;
    }

    // found a deleted spot, need to return this if key is not in the map
    assert (states[hash] == DELETED_STATE);
    final int firstDeletedHash = hash;

    // looking for the key
    while (states[hash] != AVAILABLE_STATE && keys[hash] != key && drift++ <= maximalDrift) {
      hash = (hash + 1) & arrayMask;
    }
    // if the key is found, return the key,
    // otherwise, return the first deleted position for insertion.
    return (keys[hash] == key && states[hash] == OCCUPIED_STATE) ? hash : firstDeletedHash;
  }

}
