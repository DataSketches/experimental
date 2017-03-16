/*
 * Copyright 2016, Yahoo! Inc. Licensed under the terms of the Apache License 2.0. See LICENSE file
 * at the project root for terms.
 */

package com.yahoo.sketches.hashmaps;

import static com.yahoo.sketches.QuickSelect.select;

import gnu.trove.function.TLongFunction;
import gnu.trove.map.hash.TLongLongHashMap;
import gnu.trove.procedure.TLongLongProcedure;

public class HashMapTrove extends HashMap {

  TLongLongHashMap hashmap;

  /**
   * blah
   * @param capacity blah
   */
  public HashMapTrove(final int capacity) {
    if (capacity <= 0) {
      throw new IllegalArgumentException(
          "Received negative or zero value for as initial capacity.");
    }
    this.capacity = capacity;
    hashmap = new TLongLongHashMap(capacity);
  }

  @Override
  public int getSize() {
    return hashmap.size();
  }

  @Override
  public void adjustOrPutValue(final long key, final long adjustAmount, final long putAmount) {
    hashmap.adjustOrPutValue(key, adjustAmount, putAmount);
  }

  @Override
  public long get(final long key) {
    return hashmap.get(key);
  }

  @Override
  public void keepOnlyLargerThan(final long thresholdValue) {
    hashmap.retainEntries(new GreaterThenThreshold(thresholdValue));
  }

  @Override
  public void adjustAllValuesBy(final long adjustAmount) {
    hashmap.transformValues(new AdjustAllValuesBy(adjustAmount));
  }

  @Override
  public long[] getKeys() {
    return hashmap.keys();
  }

  @Override
  public long[] getValues() {
    return hashmap.values();
  }

  @Override
  public boolean isActive(final int probe) {
    return false;
  }

  private class GreaterThenThreshold implements TLongLongProcedure {
    long threshold;

    public GreaterThenThreshold(final long threshold) {
      this.threshold = threshold;
    }

    @Override
    public boolean execute(final long key, final long value) {
      return (value > threshold);
    }
  }

  private class AdjustAllValuesBy implements TLongFunction {
    long adjustAmount;

    public AdjustAllValuesBy(final long adjustAmount) {
      this.adjustAmount = adjustAmount;
    }

    @Override
    public long execute(final long value) {
      return value + adjustAmount;
    }
  }

  @Override
  public long quickSelect(final double relativeRank,final int sampleSize) {
    final long[] vals = getValues();
    return select(vals,0,vals.length - 1,(int)(vals.length * relativeRank));
  }

}
