package com.yahoo.sketches.frequencies.hashmap;

public class HashMapReverseEfficientOneArray extends HashMap {
  
  private final int KEY_OFFSET = 0;
  private final int VALUE_OFFSET = 1;
  private final int STATE_OFFSET = 2;
  private final int KVS_SIZE = 3;
  
  long[] kvsArray;
  private int kvsLength;
  
  public HashMapReverseEfficientOneArray(int capacity) {
    super(capacity);
    kvsLength = 3*getLength();
    this.kvsArray = new long[kvsLength];
    
  }

  @Override
  public long[] getKeys() {
    if (size==0) return null;
    long [] retrunedKeys = new long[size];
    int j = 0;
    for (int i=0; i<length; i++)
      if (isActive(i)){
        retrunedKeys[j] = kvsArray[i*KVS_SIZE + KEY_OFFSET];
        j++;
      }
    assert(j == size);
    return retrunedKeys;
  }
   
  @Override
  public long[] getValues() {
    if (size==0) return null;
    long [] retrunedValues = new long[size];
    int j = 0;
    for (int i=0; i<length; i++)
      if (isActive(i)) {
        retrunedValues[j] = kvsArray[i*KVS_SIZE + VALUE_OFFSET];
        j++;
      }
    assert(j == size);
    return retrunedValues;
  }
  
  
  @Override
  public long get(long key) {
//    int probe = hashProbe(key);
//    if (kvsArray[probe*KVS_SIZE + STATE_OFFSET] > 0){
//      assert(kvsArray[probe*KVS_SIZE + KEY_OFFSET] == key);
//      return kvsArray[probe*KVS_SIZE + VALUE_OFFSET];
//    }
    return 0;
  }

  @Override
  public void adjust(long key, long value) {
    int probe = (int) hash(key) & arrayMask;
    byte drift = 1;
    while (kvsArray[probe*KVS_SIZE + STATE_OFFSET] != 0 && kvsArray[probe*KVS_SIZE + KEY_OFFSET]!=key) {
      probe = (probe+1)&arrayMask;
      drift++;
    }
    int kvsProbe = probe*KVS_SIZE;
    if (kvsArray[probe*KVS_SIZE + STATE_OFFSET] == 0) {
      // adding the key to the table the value
      assert(size < capacity);
      //kvsProbe = probe*KVS_SIZE;
      kvsArray[kvsProbe + KEY_OFFSET] = key;
      kvsArray[kvsProbe + VALUE_OFFSET] = value;
      kvsArray[kvsProbe + STATE_OFFSET] = drift;
      size++;
    } else {
      // adjusting the value of an existing key
      assert(kvsArray[kvsProbe + KEY_OFFSET] == key);
      kvsArray[kvsProbe + VALUE_OFFSET] += value;
    }
  }
  
//  public void del(long key){
//    int probe = hashProbe(key);
//    if (kvsArray[probe*KVS_SIZE + STATE_OFFSET]>0){
//      assert(kvsArray[probe*KVS_SIZE + KEY_OFFSET] == key);
//      hashDelete(probe);
//      size--;
//    }
//  }
  
  @Override
  public void shift(long value){
    int firstProbe=length-1;
    while(kvsArray[firstProbe*KVS_SIZE + STATE_OFFSET] > 0) firstProbe--;
      
    for (int probe = firstProbe;probe-->0;){
      int kvsProbe = probe*KVS_SIZE;
      if (kvsArray[kvsProbe + STATE_OFFSET] > 0){
        if(kvsArray[kvsProbe + VALUE_OFFSET] > value){
          kvsArray[kvsProbe + VALUE_OFFSET] -= value;
        } else {
          hashDelete(probe);
          size--;
        }
      }
    }
    for (int probe = length; probe-->firstProbe;){
      int kvsProbe = probe*KVS_SIZE;
      if (kvsArray[kvsProbe + STATE_OFFSET] > 0){
        if(kvsArray[kvsProbe + VALUE_OFFSET] > value){
          kvsArray[kvsProbe + VALUE_OFFSET] -= value;
        } else {
          hashDelete(probe);
          size--;
        }
      }
    }
  }
//  
//  public void shiftNotReversed(long value){
//    for (int probe=0; probe<length; probe++) {
//      if (kvsArray[probe*KVS_SIZE + STATE_OFFSET] > 0){
//        if (kvsArray[probe*KVS_SIZE + VALUE_OFFSET] > value)
//          kvsArray[probe*KVS_SIZE + VALUE_OFFSET] -= value;
//        else {
//          hashDelete(probe);
//          probe--;
//          size--;
//        }
//      }
//    }
//  }
//  
  
//  private int hashProbe(long key) {
//    int probe = (int)hash(key) &arrayMask;
//    while (kvsArray[probe*KVS_SIZE + STATE_OFFSET] > 0 && kvsArray[probe*KVS_SIZE + KEY_OFFSET]!=key) probe = (probe+1)&arrayMask;
//    return probe;
//  }

  private void hashDelete(int deleteProbe){
    // Looks ahead in the table to search for another 
    // item to move to this location 
    // if none are found, the status is changed
    int kvsDeleteProbe = deleteProbe*KVS_SIZE;
    kvsArray[kvsDeleteProbe + STATE_OFFSET] = 0;
    byte drift = 1;
    int probe = (deleteProbe+drift)&arrayMask;
    // advance until you find a free location replacing locations as needed
    int kvsProbe = probe*KVS_SIZE;
    while(kvsArray[kvsProbe + STATE_OFFSET] != 0){
      if (kvsArray[kvsProbe + STATE_OFFSET] > drift) {
        // move current element
        kvsArray[kvsDeleteProbe + KEY_OFFSET] = kvsArray[kvsProbe + KEY_OFFSET];
        kvsArray[kvsDeleteProbe + VALUE_OFFSET] = kvsArray[kvsProbe + VALUE_OFFSET];
        kvsArray[kvsDeleteProbe + STATE_OFFSET] = (byte) (kvsArray[kvsProbe + STATE_OFFSET] - drift);
        // marking this location as deleted
        kvsArray[kvsProbe + STATE_OFFSET] = 0;
        drift = 0;
        deleteProbe = probe;
      }
      probe=(probe+1)&arrayMask; 
      kvsProbe = probe*KVS_SIZE;
      kvsDeleteProbe = deleteProbe*KVS_SIZE;
      drift++;
    }
  }

  @Override
  protected boolean isActive(int probe) {
    return (kvsArray[probe*KVS_SIZE + STATE_OFFSET] > 0);
  }

  @Override
  public void print(){
    for (int probe=0; probe<keys.length; probe++){
      System.out.format("%3d: (%4d,%4d,%3d)\n",
                        probe, 
                        kvsArray[probe*KVS_SIZE + STATE_OFFSET],
                        kvsArray[probe*KVS_SIZE + KEY_OFFSET],
                        kvsArray[probe*KVS_SIZE + VALUE_OFFSET]);
    }
    System.out.format("=====================\n");
  }
}
