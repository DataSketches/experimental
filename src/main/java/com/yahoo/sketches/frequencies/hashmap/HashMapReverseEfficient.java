package com.yahoo.sketches.frequencies.hashmap;

public class HashMapReverseEfficient extends HashMap {

  public HashMapReverseEfficient(int capacity) {
    super(capacity);
  }

  @Override
  public long get(long key) {
    int probe = hashProbe(key);
    if (states[probe] > 0){
      assert(keys[probe] == key);
      return values[probe];
    }
    return 0;
  }

  @Override
  public void adjust(long key, long value) {
    int probe = (int) hash(key) & arrayMask;
    byte drift = 1;
    while (states[probe] != 0 && keys[probe]!=key) {
      probe = (probe+1)&arrayMask;
      drift++;
    }
    
    if (states[probe] == 0) {
      // adding the key to the table the value
      assert(size < capacity);
      keys[probe] = key;
      values[probe] = value;
      states[probe] = drift;
      size++;
    } else {
      // adjusting the value of an existing key
      assert(keys[probe] == key);
      values[probe] += value;
    }
  }
  
  public void del(long key){
    int probe = hashProbe(key);
    if (states[probe]>0){
      assert(keys[probe] == key);
      hashDelete(probe);
      size--;
    }
  }
  
  @Override
  public void shift(long value){
    int firstProbe=length-1;
    while(states[firstProbe] > 0) firstProbe--;
      
    for (int probe = firstProbe;probe-->0;){
      if (states[probe] > 0){
        if(values[probe] > value){
          values[probe] -= value;
        } else {
          hashDelete(probe);
          size--;
        }
      }
    }
    for (int probe = length; probe-->firstProbe;){
      if (states[probe] > 0){
        if(values[probe] > value){
          values[probe] -= value;
        } else {
          hashDelete(probe);
          size--;
        }
      }
    }
  }
  
  public void shiftNotReversed(long value){
    for (int probe=0; probe<length; probe++) {
      if (states[probe] > 0){
        if (values[probe] > value)
          values[probe] -= value;
        else {
          hashDelete(probe);
          probe--;
          size--;
        }
      }
    }
  }
  
  private int hashProbe(long key) {
    int probe = (int)hash(key) &arrayMask;
    while (states[probe] > 0 && keys[probe]!=key) probe = (probe+1)&arrayMask;
    return probe;
  }

  private void hashDelete(int deleteProbe){
    // Looks ahead in the table to search for another 
    // item to move to this location 
    // if none are found, the status is changed
    states[deleteProbe] = 0;
    byte drift = 1;
    int probe = (deleteProbe+drift)&arrayMask;
    // advance until you find a free location replacing locations as needed
    while(states[probe] != 0){
      if (states[probe] > drift) {
        // move current element
        keys[deleteProbe] = keys[probe];
        values[deleteProbe] = values[probe];
        states[deleteProbe] = (byte) (states[probe] - drift);
        // marking this location as deleted
        states[probe] = 0;
        drift = 0;
        deleteProbe = probe;
      }
      probe=(probe+1)&arrayMask; 
      drift++;
    }
  }

  @Override
  protected boolean isActive(int probe) {
    return (states[probe] > 0);
  }
}
