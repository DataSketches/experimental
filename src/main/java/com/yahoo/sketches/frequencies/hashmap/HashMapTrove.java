package com.yahoo.sketches.frequencies.hashmap;

import gnu.trove.procedure.TLongLongProcedure;
import gnu.trove.function.TLongFunction;
import gnu.trove.map.hash.TLongLongHashMap;

public class HashMapTrove extends HashMap {

  TLongLongHashMap hashmap;
  
  public HashMapTrove(int capacity){
    super(1);
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
    hashmap.retainEntries(new GreaterThenThreshold(value));
    hashmap.transformValues(new decreaseByThreshold(value));
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
  
  private class GreaterThenThreshold implements TLongLongProcedure {
    long threshold;
    public GreaterThenThreshold(long threshold){
      this.threshold = threshold;
    }
    
    @Override
    public boolean execute(long key, long value) {
      return (value > threshold);
    }
  }
  
  private class decreaseByThreshold implements TLongFunction {
    long threshold;
    public decreaseByThreshold(long threshold){
      this.threshold = threshold;
    }
    
    @Override
    public long execute(long value) {
      // TODO Auto-generated method stub
      return value - threshold;
    }    
  }
}
