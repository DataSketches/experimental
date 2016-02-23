/*
 * Copyright 2015, Yahoo! Inc.
 * Licensed under the terms of the Apache License 2.0. See LICENSE file at the project root for terms.
 */

package com.yahoo.sketches.frequencies;

import java.util.Arrays;

import com.yahoo.sketches.hashmaps.HashMapReverseEfficient;

/**
 * Implements frequent items sketch on the Java heap.
 * 
 * The frequent-items sketch is useful for keeping approximate counters for keys 
 * (map from key (long) to value (long)).  The sketch is initialized with a value 
 * k. The sketch will keep roughly k counters when it is full size. More specifically,
 * when k is a power of 2, a HashMap will be created with 2*k cells, and the number 
 * of counters will oscillate between roughly k and 1.5*k. The space usage of the sketch
 * is therefore proportional to k when it reaches full size.
 * 
 * When the sketch is updated with a key and increment, the corresponding counter is incremented or, 
 * if there is no counter for that key, a new counter is created. If the sketch 
 * reaches its maximal allowed size, it decrements all of the counters
 * (by an approximately computed median), and removes any non-positive counters.
 * 
 * The logic of the frequent-items sketch is such that the stored counts and real 
 * counts are never too different. More specifically, for any key KEY, the sketch 
 * can return an estimate of the true frequency of KEY, along with upper and lower 
 * bounds on the frequency (that hold deterministically). For our implementation,
 * it is guaranteed that, with high probability over the randomness of the 
 * implementation, the difference between the upper bound and the estimate is 
 * at most n/k, where n denotes the stream length (i.e, sum of all the frequencies), 
 * and similarly for the lower bound and the estimate. In practice, the difference 
 * is usually smaller.
 * 
 * Background:
 * This code implements a variant of what is commonly known as the "Misra-Gries 
 * algorithm" or "Frequent Items". Variants of it were discovered and rediscovered 
 * and redesigned several times over the years.
 * a) "Finding repeated elements", Misra, Gries, 1982 
 * b) "Frequency estimation of internet packet streams with limited space" 
 *    Demaine, Lopez-Ortiz, Munro, 2002
 * c) "A simple algorithm for finding frequent elements in streams and bags" 
 *    Karp, Shenker, Papadimitriou, 2003
 * d)  "Efficient Computation of Frequent and Top-k Elements in Data Streams" 
 *    Metwally, Agrawal, Abbadi, 2006
 * 
 * @author Justin Thaler
 */
public class FrequentItems extends FrequencyEstimator{

  /**
   *  We start by allocating a small data structure capable of explicitly
   *  storing very small streams in full, and growing it as the stream grows.
   *  The following constant controls the size of the initial data structure
   */
  static final int MIN_FREQUENT_ITEMS_SIZE = 4; //This is somewhat arbitrary
  
  /**
   *  The current number of counters that the data structure can support
   */
  private int K;

  /**
   * Hash map mapping stored keys to approximate counts
   */
  private HashMapReverseEfficient counters;
  
  /**
   *  The number of counters to be stored when sketch is full size
   */
  private int maxK;
  
  /**
   *  The value of K passed to the constructor
   */
  private int k;
  
  /**
   *  Tracks the total number of decrements performed on sketch.
   */
  private int offset;
  
  /**
   *  An upper bound on the error in any estimated count due to merging with 
   *  other FrequentItems sketches.
   */
  private int mergeError;
  
  /**
   *  The sum of all frequencies of the stream so far.
   */
  private int streamLength=0;
  
  /**
   *  The maximum number of samples used to compute approximate median of counters when doing decrement
   */
  private int sample_size;
  
  
  //**CONSTRUCTOR********************************************************** 
  /**
   * @param k
   * Determines the accuracy of the estimates returned by the sketch.
   * The guarantee of the sketch is that any returned estimate will have error
   * at most eps*N, where N is the true sum of frequencies in the stream,
   * and eps:=1/k.
   * The space usage of the sketch is proportional to k.
   * If fewer than ~k different keys are inserted then the counts will be exact. 
   * More precisely, if k is a power of 2,then when the sketch reaches full size,
   * the data structure's HashMap will contain 2*k cells. 
   * Assuming that the LOAD_FACTOR of the HashMap is set to 0.75, the number of cells
   * of the hash table that are actually filled should oscillate between 
   * k and 1.5 * k. The guarantee of the sketch is that (with high probability)
   * the error of any estimate will be at most (1/k) * n, where n is the stream length.
   * In practice, the error is usually much smaller.
   */
  public FrequentItems(int k) {
    if (k <= 0) throw new IllegalArgumentException("Received negative or zero value for k.");
    
    //set initial size of counters data structure so it can exactly store a stream with 
    //MIN_FREQUENT_ITEMS_SIZE distinct elements
    this.K = MIN_FREQUENT_ITEMS_SIZE;
    counters = new HashMapReverseEfficient(this.K);
    
    this.k=k;
    
    //set maxK to be the maximum number of counters that can be supported
    //by a HashMap with the appropriate number of cells (specifically, 
    //2*k cells if k is a power of 2) and a load that does not exceed 
    //the designated load factor
    int maxHashMapLength = Integer.highestOneBit(4*k-1);
    this.maxK = (int) (maxHashMapLength * counters.LOAD_FACTOR);
    
    this.offset = 0;
    if (this.maxK < 250) 
      this.sample_size = this.maxK;
    else
      this.sample_size = 250;
  }
  
  /**
   * @return the number of positive counters in the sketch.
   */
  public long nnz() {
    return counters.getSize();
  }
  
  /**
   * @param key whose count estimate is returned.
   * @return an estimate of the count for the key.
   */
  @Override
  public long getEstimate(long key) { 
    //the logic below returns the count of associated counter if key is tracked.
    //If the key is not tracked and fewer than maxK counters are in use, 0 is returned.
    //Otherwise, the minimum counter value is returned.
    if(counters.get(key) > 0) 
      return counters.get(key) + offset;
    else
      return 0;
  }
  
   /**
    * @param key whose count estimate is returned.
    * @return an upper bound on the count for the key.
    */
   @Override
   public long getEstimateUpperBound(long key)
   {
     long estimate = getEstimate(key);
     if(estimate > 0)
       return estimate + mergeError;

     return mergeError + offset;

   }
   
   /**
    * @param key whose count estimate is returned.
    * @return a lower bound on the count for the key.
    */
   @Override
   public long getEstimateLowerBound(long key)
   {
     long estimate = getEstimate(key);
            
     if((estimate-offset-mergeError) <= 0)
       return 0;
       
     return (estimate-offset-mergeError);
   }

  /**
   * @return the maximal error of the estimate one gets from get(key).
   * 
   */
  @Override
  public long getMaxError() {
      return offset + mergeError;
  }
  
  /**
   * @param key 
   * Process a key (specified as a long) update and treat the increment as 1
   */
  @Override  
  public void update(long key) {
    update(key, 1);
  }


  
  /**
   * @param key 
   * A key (as long) to be added to the sketch. The key cannot be null.
   */
  @Override
  public void update(long key, long increment) {
    this.streamLength += increment;
    counters.adjust(key, increment);
    int size = counters.getSize();  
    
    //if the data structure needs to be grown
    if ((size >= this.K) && (this.K < this.maxK)) {
      //grow the size of the data structure
      int newSize = Math.max(Math.min(this.maxK, 2*this.K), 1);
      this.K = newSize;
      HashMapReverseEfficient newTable = new HashMapReverseEfficient(newSize);
      long[] keys = this.counters.getKeys();
      long[] values = this.counters.getValues();
      for(int i = 0; i < size; i++) {
        newTable.adjust(keys[i], values[i]);
      }
      this.counters = newTable;
    } 
    
    if(size > this.maxK) {
      purge();
      assert(size <= this.maxK);
    }
  }

  /**
   * This function is called when a key is processed that is not currently assigned
   * a counter, and all the counters are in use. This function estimates the median
   * of the counters in the sketch via sampling, decrements all counts by this estimate,
   * throws out all counters that are no longer positive, and increments offset accordingly.
   */
  private void purge()
  {
    int limit = Math.min(this.sample_size, counters.getSize());
    
    long[] values = counters.ProtectedGetValues();
    int num_samples = 0;
    int i = 0;
    long[] samples = new long[limit];

    while(num_samples < limit){
      if(counters.isActive(i)){
        samples[num_samples] = values[i];
        num_samples++;
      }
      i++;
    }
    
    Arrays.sort(samples, 0, num_samples);
    long val = samples[limit/2];
    counters.adjustAllValuesBy(-1 * val);
    counters.keepOnlyLargerThan(0);
    this.offset += val; 
  }

  
   /**
    * This function merges two FrequentItems sketches
    * @param other
    * Another FrequentItems sketch. Potentially of different size. 
    * @return pointer to the sketch resulting in adding the approximate counts of another sketch. 
    * This method does not create a new sketch. The sketch whose function is executed is changed
    * and a reference to it is returned.
    */
    @Override
   public FrequencyEstimator merge(FrequencyEstimator other) {
     if (!(other instanceof FrequentItems)) throw new IllegalArgumentException("FrequentItems can only merge with other FrequentItems");
       FrequentItems otherCasted = (FrequentItems) other;
       
     this.streamLength += otherCasted.streamLength;
     this.mergeError += otherCasted.getMaxError();
     
     long[] other_keys = otherCasted.counters.getKeys();
     long[] other_values = otherCasted.counters.getValues();
      
     for (int i=other_keys.length; i-->0;) { 
       this.update(other_keys[i], other_values[i]);
     }   
     return this;
   }
  
   /**
    * @return an array containing all keys exceed the frequency threshold of roughly 1/eps+1
    */
   @Override
   public long[] getFrequentKeys() {
     int count = 0;
     long[] keys = counters.ProtectedGetKey();
     
     //first, count the number of candidate frequent keys
     for(int i = counters.getLength(); i-->0;) {
       if(counters.isActive(i) && (getEstimate(keys[i]) >= (this.streamLength / this.maxK))) {
         count++;
       }
     }
     
     //allocate an array to store the candidate frequent keys, and then compute them
     long[] freq_keys = new long[count];
     count = 0;
     for(int i = counters.getLength(); i-->0;) {
       if(counters.isActive(i) && (getEstimate(keys[i]) >= (this.streamLength / this.maxK))) {
         freq_keys[count] = keys[i];
         count++;
       }
     }
     return freq_keys;
   }
   
   /**
    * Turns the FrequentItems object into a string
    * @return a string specifying the FrequentItems object
    */
   public String FrequentItemsToString() {
     StringBuilder sb = new StringBuilder();
     sb.append(String.format("%d,%d,%d,", k, mergeError, offset));

     sb.append(counters.hashMapReverseEfficientToString());
     return sb.toString();
   }
   
   /**
    * Turns a string specifying a FrequentItems object 
    * into a FrequentItems object.
    * @param string String specifying a FrequentItems object
    * @return a FrequentItems object corresponding to the string
    */
   public static FrequentItems StringToFrequentItems(String string) {
     String[] tokens = string.split(",");
     if(tokens.length < 3) {
       throw new IllegalArgumentException("Tried to make FrequentItems out of string not long enough to specify relevant parameters.");
     }
     
     int k = Integer.parseInt(tokens[0]);
     int mergeError = Integer.parseInt(tokens[1]);
     int offset = Integer.parseInt(tokens[2]);
     
     FrequentItems sketch = new FrequentItems(k);
     sketch.mergeError=mergeError;
     sketch.offset = offset;
     
     sketch.counters = HashMapReverseEfficient.StringArrayToHashMapReverseEfficient(tokens, 3);
     return sketch;
   }
   
   
   /**
    * Returns the current number of counters the sketch is configured to store.
    * @return the current number of counters the sketch is configured to store.
    */
    @Override
   public int getK() {
     return this.K;
   }
   
    /**
     * Returns the sum of the frequencies in the stream seen so far by the sketch
     * @return the sum of the frequencies in the stream seen so far by the sketch
     */
     @Override
    public int getStreamLength() {
      return this.streamLength;
    }
   
   /**
    * Returns the maximum number of counters the sketch will ever be configured to store.
    * @return the maximum number of counters the sketch will ever be configured to store.
    */
    @Override
   public int getMaxK() {
     return this.maxK;
   }
   
    /**
     * Returns true if this sketch is empty
     * @return true if this sketch is empty
     */
     @Override
    public boolean isEmpty() {
     return this.counters.getSize() == 0; 
    }
    
     /**
      * Resets this sketch to a virgin state, but retains the original value of the error parameter
      */
      @Override
     public void reset() {
       this.K = MIN_FREQUENT_ITEMS_SIZE;
       counters = new HashMapReverseEfficient(this.K);
       this.offset = 0;
     }
     
      /**
       * Returns the number of bytes required to store this sketch as an array of bytes.
       * @return the number of bytes required to store this sketch as an array of bytes.
       */
      public int getStorageBytes() {
        if (isEmpty()) return 20;
        return 28 + 16 * this.counters.getSize();
      }
}
