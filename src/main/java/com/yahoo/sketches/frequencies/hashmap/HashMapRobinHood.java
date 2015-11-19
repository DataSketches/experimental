package com.yahoo.sketches.frequencies.hashmap;

public class HashMapRobinHood extends HashMap{

  public HashMapRobinHood (int capacity) {
    super(capacity);
  }
  
  @Override
  public long get(long key) {
    // TODO add implementation 
    return 0;
  }

  public void adjustHash(long key, long value, long hash) {
    int probe = hashProbe(key, hash);
    values[probe] += value;
  }
    
  @Override
  public void adjust(long key, long value) {
    int probe = hashProbe(key, hash(key));
    values[probe] += value;
  }
  
//  public void del(long key){
//    int probe = hashProbe(key);
//    if (states[probe]>0){
//      assert(keys[probe] == key);
//      hashDelete(probe);
//      size--;
//    }
//  }
  
  @Override
  public void shift(long value){
    shiftFancy(value);
  }
  
  public void shiftFancy(long value){
    
    int firstProbe = 0;
    
    // first probe is the last vacant cell before an occupied one
    while(states[firstProbe]>0) firstProbe=(firstProbe-1)&arrayMask;
    
    // loop around the array once
    int deletes = 0;
    int newProbe;
    for (int probe = (firstProbe+1)&arrayMask; probe!=firstProbe; probe = (probe+1)&arrayMask){
      if (states[probe]==0){
        deletes = 0;
      } else {
        // It needs to be deleted
        if (values[probe] <= value){
          states[probe]=0;
          deletes ++;
          size--;
        } else {
          if (deletes > 0){
            if (deletes >= states[probe]) deletes = states[probe]-1; 
            newProbe = (probe - deletes)&arrayMask;
          
            keys[newProbe] = keys[probe];
            values[newProbe] = values[probe] - value;
            states[newProbe] = (byte) (states[probe] - deletes);
            states[probe] = 0;
          }
        }
      }
    }
  }
  
  public void shiftNaive(long value){
    int startProbe = 0;
    while(states[startProbe] > 0) startProbe = (startProbe+1) & arrayMask;
    
    for (int probe=(startProbe+1)&arrayMask; probe!=startProbe; probe=(probe+1)&arrayMask) {
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
  
  private int hashProbe(long key, long hash) {
    int probe = (int) hash & arrayMask;
    byte state = 1;
    while (states[probe] >= state && keys[probe]!=key) {
      state++;
      probe = (probe+1)&arrayMask;
    }
    
    // found the key
    if (keys[probe]==key && states[probe] > 0) return probe;
    
    // found a vacant spot
    if (states[probe] == 0){
      keys[probe] = key;
      values[probe] = 0;
      states[probe] = state;
      size++;
      return probe;
    }
    
    int rightProbe =(probe+1)&arrayMask;
    while (states[rightProbe]>0) rightProbe=(rightProbe+1)&arrayMask; 
    
    while(rightProbe != probe){
      int leftOfRightProbe = (rightProbe-1)&arrayMask;
      keys[rightProbe] = keys[leftOfRightProbe];
      values[rightProbe] = values[leftOfRightProbe];
      states[rightProbe] = (byte) (states[leftOfRightProbe]+1);
      rightProbe = leftOfRightProbe;
    }
    keys[probe] = key;
    values[probe] = 0;
    states[probe] = state;
    size++;
    return probe;
  }

  private void hashDelete(int deleteProbe){
    int nextProbe = (deleteProbe+1)&arrayMask;
    while(states[nextProbe]>1){
      keys[deleteProbe] = keys[nextProbe];
      values[deleteProbe] = values[nextProbe];
      //states[deleteProbe] = (byte) (states[nextProbe]-1);
      deleteProbe = nextProbe;
      nextProbe = (deleteProbe+1)&arrayMask;
    }
    states[deleteProbe] = 0;
  }

  @Override
  protected boolean isActive(int probe) {
    return (states[probe]>0);
  }
}
