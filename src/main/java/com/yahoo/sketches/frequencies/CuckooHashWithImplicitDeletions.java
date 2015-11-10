package com.yahoo.sketches.frequencies;

import com.yahoo.sketches.hash.MurmurHash3;

public class CuckooHashWithImplicitDeletions {  
  final private double LOAD_FACTOR = 0.5;
  final private int LOCATIONS_PER_KEY = 10;
  private long offset;
  private int keyValueArrayLength;
  private long[] keys;
  private long[] values;
  int[] keyLocationInArray;
  long[] keyArr = new long[1];
  
  public CuckooHashWithImplicitDeletions(int maxSize) {
    if (maxSize <= 0) throw new IllegalArgumentException("Received negative or zero value for maxSize.");
    keyValueArrayLength = (int) (maxSize/LOAD_FACTOR);
    keys = new long[keyValueArrayLength];
    values = new long[keyValueArrayLength];
    offset = 0; 
  }

  public long get(long key) {
    for (int i=LOCATIONS_PER_KEY; i-->0;){
      int index = indexForKey(key,i);
      if (keys[index] == key) {
        long value = values[index];
        return (value > offset) ? value-offset : 0;
      }
    }
    return 0;
  }
      
  private int indexForKey(long key, int i){
    keyArr[0] = key;
    return (((int)(MurmurHash3.hash(keyArr,i)[0]))>>>1) % keyValueArrayLength;
  }
  
  public boolean increment(long key) {
    // In case the key is in the map already
    int availableIndex = -1;
    for (int i=LOCATIONS_PER_KEY; i-->0;) {
      int index = indexForKey(key,i);
      if (keys[index] == key) {
        long value = values[index];
        values[index] = (value > offset) ? value+1: offset+1;
        return true;
      }
      if (availableIndex < 0 && values[index] <= offset) {
        availableIndex = index;
      }
    }
    // The key is not in the map but there is a spot for it
    if (availableIndex >= 0) {
      keys[availableIndex] = key;
      values[availableIndex] = offset+1;
      return true;
    }
    // Need to add bump(key)
    return false;
  }

  public void decrement(){
    offset++;
  }
  
}
