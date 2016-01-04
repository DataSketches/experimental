package com.yahoo.sketches.hashmaps;

/**
 * @author edo
 *
 */

public abstract class HashMap {
  
  // The load factor is decided upon by the abstract class.
  // This cannot be modified by inheriting classes!
  final private double LOAD_FACTOR = 0.75; 
  
  protected int capacity;
  protected int length;
  protected int arrayMask;
  protected int size=0;
  protected long[] keys;
  protected long[] values;
  protected byte[] states;
  
  public HashMap () {}
  
  /**
   * @param capacity
   * Determines the number of (key, value) pairs the hashmap is expected to store.
   * Constructor will create arrays of sizecapacity/LOAD_FACTOR, rounded up to a power of 2.
   */
  public HashMap (int capacity) {
    if (capacity <= 0) throw new IllegalArgumentException("Received negative or zero value for as initial capacity.");
    this.capacity = capacity;
    length = Integer.highestOneBit(2*(int)(capacity/LOAD_FACTOR)-1);
    arrayMask = length-1; 
    keys = new long[length];
    values = new long[length];
    states = new byte[length];
  }
     
  /**
   * Adjusts the primitive value mapped to the key if the key is present in the map. 
   * Otherwise, the key is inserted with the value.
   * @param key the key of the value to increment
   * @param adjustAmount the amount to adjust the value by 
   * @param putAmount the value put into the map if the key is not initial present
   * @return the value present in the map after the adjustment or put operation
   */
  abstract public void adjustOrPutValue(long key, long adjustAmount, long putAmount);
  
  public void adjust(long key, long value){
    adjustOrPutValue(key, value, value);
  }

  /** 
   * @param key the key to look for
   * @return the positive value the key corresponds to or zero if
   * if the key is not found in the hash map. 
   */
  abstract public long get(long key);
    
  /**
   * @param value by which to shift all values.
   * Only keys corresponding to positive values are retained.  
   */
  public void adjustAllValuesBy(long adjustAmount) {
    for(int i=length;i-->0;)
      values[i] += adjustAmount;
  }
  
  /**
   * @param value by which to shift all values.
   * Only keys corresponding to positive values are retained.  
   */
  abstract public void keepOnlyLargerThan(long thresholdValue);

  /**
   * @param probe location in the hash table array
   * @return true if the cell in the array contains an active key
   */
  abstract public boolean isActive(int probe);

  /**
   * @return an array containing the active keys in the hash map.
   */
  public long[] getKeys() {
    if (size==0) return null;
    long [] retrunedKeys = new long[size];
    int j = 0;
    for (int i=0; i<length; i++)
      if (isActive(i)){
        retrunedKeys[j] = keys[i];
        j++;
      }
    assert(j == size);
    return retrunedKeys;
  }
   
  /**
   * @return an array containing the values corresponding.
   * to the active keys in the hash
   */
  public long[] getValues() {
    if (size==0) return null;
    long [] retrunedValues = new long[size];
    int j = 0;
    for (int i=0; i<length; i++)
      if (isActive(i)) {
        retrunedValues[j] = values[i];
        j++;
      }
    assert(j == size);
    return retrunedValues;
  }  
  
  /**
   * @return the raw array of keys. Do NOT modify this array!
   */
  public long[] ProtectedGetKey(){
    return keys; 
  }
  
  /**
   * @return the raw array of values. Do NOT modify this array!
   */
  public long[] ProtectedGetValues(){
    return values; 
  }
  
  /**
   * @return length of hash table internal arrays 
   */
  public int getLength(){
    return length;
  }
  
  /**
   * @return number of populated keys
   */
  public int getSize(){
    return size;
  }
    
  /**
   * prints the hash table
   */
  public void print(){
    for (int i=0; i<keys.length; i++){
      System.out.format("%3d: (%4d,%4d,%3d)\n",i, states[i],keys[i],values[i]);
    }
    System.out.format("=====================\n");
  }

  /**
   * @return the load factor of the hash table, the ratio
   * between the capacity and the array length
   */
  protected double getLoadFactor(){
    return LOAD_FACTOR;
  }
  
  /**
   * @param key to be hashed
   * @return an index into the hash table 
   */
  protected long hash(long key){
    key ^= key >>> 33;
    key *= 0xff51afd7ed558ccdL;
    key ^= key >>> 33;
    key *= 0xc4ceb9fe1a85ec53L;
    key ^= key >>> 33;
    return key;
  }
      
}
