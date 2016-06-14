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

  public HashMapTroveRebuilds(int capacity) {
    super(1);
    this.capacity = capacity;
    hashmap = new TLongLongHashMap(capacity);
  }

  @Override
  public int getSize() {
    return hashmap.size();
  }

  @Override
  public boolean isActive(int probe) {
    return false;
  }

  @Override
  public void adjustOrPutValue(long key, long adjustAmount, long putAmount) {
    hashmap.adjustOrPutValue(key, adjustAmount, putAmount);
  }

  @Override
  public long get(long key) {
    return hashmap.get(key);
  }

  @Override
  public void keepOnlyLargerThan(long thresholdValue) {
    TLongLongHashMap newHashmap = new TLongLongHashMap(capacity);
    TLongLongIterator iterator = hashmap.iterator();
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
  public long quickSelect(int rank){
   	assert(rank > 1 && rank <= getSize());
    long[] vals = getValues();
    int sampleSize = vals.length;
    return select(vals,0,sampleSize-1,sampleSize-rank);   
  }
  
  @Override
  public long quickSelect(double relativeRank,int sampleSize){
  	long[] vals = getValues();
    return select(vals,0,vals.length-1,(int)(vals.length*relativeRank));
  }
  
  
  @Override
  public void adjustAllValuesBy(long adjustAmount) {
    hashmap.transformValues(new AdjustAllValuesBy(adjustAmount));
  }

  private class AdjustAllValuesBy implements TLongFunction {
    long adjustAmount;

    public AdjustAllValuesBy(long adjustAmount) {
      this.adjustAmount = adjustAmount;
    }

    @Override
    public long execute(long value) {
      return value + adjustAmount;
    }
  }
  
}
