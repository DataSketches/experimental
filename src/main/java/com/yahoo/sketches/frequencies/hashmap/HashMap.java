package com.yahoo.sketches.frequencies.hashmap;

//import com.yahoo.sketches.QuickSelect;
import com.yahoo.sketches.hash.MurmurHash3;

/**
 * @author edo
 *
 */

public abstract class HashMap {
  
  // The load factor is decided upon by the abstract class.
  // This cannot be modified by inheriting classes!
  final private double LOAD_FACTOR = 0.7; 
  
  protected int capacity;
  protected int length;
  protected int arrayMask;
  protected int size=0;
  protected long[] keys;
  protected long[] values;
  protected byte[] states;
  
  private long[] keyArr = new long[1];
  
  public HashMap (int capacity) {
    if (capacity <= 0) throw new IllegalArgumentException("Received negative or zero value for as initial capacity.");
    this.capacity = capacity;
    // arraysLength is the smallest power of 2 greater than capacity/LOAD_FACTOR
    length = Integer.highestOneBit(2*(int)(capacity/LOAD_FACTOR)-1);
    arrayMask = length-1; 
    keys = new long[length];
    values = new long[length];
    states = new byte[length];
  }
     
  /**
   * @param key the key whose value should be set
   * @param value the value to set.
   * If the key exists, its value is increased by value.
   * Otherwise, the key is inserted with the value value.
   * Throws an exception if the number of active keys exceeds capacity.
   */
  //abstract public void set(long key, long value);
  
  /**
   * @param key the key whose value should be adjusted
   * @param value the value by which to adjust.
   * If the key exists, its value is increased by value.
   * Otherwise, the key is inserted with the value value.
   * Throws an exception if the number of active keys exceeds capacity.
   */  
  abstract public void adjust(long key, long value);
 
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
  abstract public void shift(long value);
  
  /**
   * @param key to be hashed
   * @return an index into the hash table 
   */
  protected long hash(long key){
    keyArr[0] = key;
    return MurmurHash3.hash(keyArr,0)[0];
  }
  
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
   * @param probe int location in array
   * @return true if the cell in the array contains an active key
   */
  abstract protected boolean isActive(int probe);

  
//  /**
//   * @return value that is 
//   * 1) no smaller than at least lower items
//   * 2) no larger than at least upper items
//   * 
//   * Assumes all values in the array are positive.
//   */ 
//  public long median() {
//    long[] tempValues = new long[length];
//    System.arraycopy(values, 0, tempValues, 0, length);
//    long value = QuickSelect.select(values, 0, length-1, length-size/2);
//    return value;
//  }
//
//  
//  public long smallValue() {
//    int n = 100;
//    if (n > size) n = size;
//    long [] tempValues = new long[n];
//    int j = 0;
//    for (int i=0; j<n; i++)
//      if (isActive(i)) 
//        tempValues[j++] = values[i];
//    assert(j == n);
//    long value = QuickSelect.select(tempValues, 0, n-1, (int) (n*0.4));
//    // TODO: add check for median
//    return value;
//  }
  
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

}
