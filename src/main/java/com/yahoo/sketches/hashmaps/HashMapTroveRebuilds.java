/*
 * Copyright 2016, Yahoo! Inc. Licensed under the terms of the Apache License 2.0. See LICENSE file
 * at the project root for terms.
 */

package com.yahoo.sketches.hashmaps;

import static com.yahoo.sketches.QuickSelect.select;

import gnu.trove.function.TLongFunction;
import gnu.trove.iterator.TLongLongIterator;
import gnu.trove.map.hash.TLongLongHashMap;

public class HashMapTroveRebuilds extends HashMap {

  TLongLongHashMap hashmap;
  long threshold;
  @SuppressWarnings("hiding")
  int capacity;

  /**
   * blah
   * @param capacity blah
   */
  public HashMapTroveRebuilds(final int capacity) {
    super(1);
    this.capacity = capacity;
    hashmap = new TLongLongHashMap(capacity);
  }

  @Override
  public int getSize() {
    return hashmap.size();
  }

  @Override
  public boolean isActive(final int probe) {
    return false;
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
    final TLongLongHashMap newHashmap = new TLongLongHashMap(capacity);
    final TLongLongIterator iterator = hashmap.iterator();
    for (int i = hashmap.size(); i-- > 0;) {
      iterator.advance();
      if (iterator.value() > thresholdValue) {
        newHashmap.put(iterator.key(), iterator.value());
      }
    }
    hashmap = newHashmap;
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
  public long quickSelect(final int rank) {
    assert rank > 1 && rank <= getSize();
    final long[] vals = getValues();
    final int sampleSize = vals.length;
    return select(vals,0,sampleSize - 1, sampleSize - rank);
  }

  @Override
  public long quickSelect(final double relativeRank, final int sampleSize) {
    final long[] vals = getValues();
    return select(vals,0,vals.length - 1, (int)(vals.length * relativeRank));
  }


  @Override
  public void adjustAllValuesBy(final long adjustAmount) {
    hashmap.transformValues(new AdjustAllValuesBy(adjustAmount));
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

}
