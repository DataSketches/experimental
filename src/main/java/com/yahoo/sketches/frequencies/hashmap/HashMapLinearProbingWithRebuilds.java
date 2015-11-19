package com.yahoo.sketches.frequencies.hashmap;


public class HashMapLinearProbingWithRebuilds extends HashMap {
    
  public HashMapLinearProbingWithRebuilds (int capacity) {
    super(capacity);
  }
  
  @Override
  protected boolean isActive(int probe) {
    return (states[probe]>0);
  }
  
  @Override
  public long get(long key) {
    int probe = hashProbe(key);
    return (states[probe] > 0) ? values[probe] : 0;
  }
      
  @Override
  public void adjust(long key, long value) {
    if (value < 0) throw new IllegalArgumentException("adjust received negative value.");
    int probe = hashProbe(key);
    if (states[probe] == 0) {
      keys[probe] = key;
      values[probe] = value;
      states[probe] = 1;
      size++;
    } else {
      assert(keys[probe] == key);
      values[probe] += value;
    }
  }

  @Override
  public void shift(long value) {
    HashMapLinearProbingWithRebuilds rebuiltHashMap = new HashMapLinearProbingWithRebuilds(capacity);
    for(int i=0;i<length;i++)
      if (states[i] > 0 && values[i] > value)
        rebuiltHashMap.adjust(keys[i], values[i]-value);
    System.arraycopy(rebuiltHashMap.keys, 0, keys, 0, length);
    System.arraycopy(rebuiltHashMap.values, 0, values, 0, length);
    System.arraycopy(rebuiltHashMap.states, 0, states, 0, length);
    size = rebuiltHashMap.getSize();
  }

  /**
   * @param key to search for in the array
   * @return returns the location of the key in the array or the first 
   * possible place to insert it. 
   */
  private int hashProbe(long key) {
    long hash = hash(key);
    //int arrayMask = (1 << lgArrLongs) - 1; // current Size -1
    // make odd and independent of curProbe:
    int stride = 1;//(2 * (int) ((hash >> (lgArrLongs)) & STRIDE_MASK)) + 1;
    int probe = (int) (hash & arrayMask);
    // search for duplicate or zero
    while (keys[probe] != key && states[probe] != 0) probe = (probe + stride) & arrayMask;
    return probe;
  }
  
}
