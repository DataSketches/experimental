package com.yahoo.sketches.frequencies.hashmap;

import gnu.trove.iterator.TLongLongIterator;
import gnu.trove.map.hash.TLongLongHashMap;

public class HashMapTroveRebuilds extends HashMap {

  TLongLongHashMap hashmap;
  long threshold;
  @SuppressWarnings("hiding")
  int capacity;
  
  public HashMapTroveRebuilds(int capacity){
    super(1);
    this.capacity = capacity;
    hashmap = new TLongLongHashMap(capacity);
  }
  @Override
  public int getSize(){
    return hashmap.size();
  }
  
  @Override
  public void adjust(long key, long value) {
    hashmap.adjustOrPutValue(key, value, value);
  }

  @Override
  public long get(long key) {
    return hashmap.get(key);
  }

  @Override
  public void shift(long value) {
    TLongLongHashMap newHashmap = new TLongLongHashMap(capacity);
    TLongLongIterator iterator = hashmap.iterator();
    for ( int i = hashmap.size(); i-->0; ) {
      iterator.advance();
      if (iterator.value() > value){ 
        newHashmap.put(iterator.key(), iterator.value()-value);
      }
    }
    hashmap = newHashmap;
  }

  @Override
  public long[] getKeys(){
    return hashmap.keys();
  }
  
  @Override
  public long[] getValues(){
    return hashmap.values();
  }
  
  @Override
  protected boolean isActive(int probe) {
    return false;
  }

}
