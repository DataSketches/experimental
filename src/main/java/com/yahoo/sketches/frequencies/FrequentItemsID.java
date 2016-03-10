/*
 * Copyright 2015, Yahoo! Inc.
 * Licensed under the terms of the Apache License 2.0. See LICENSE file at the project root for terms.
 */

package com.yahoo.sketches.frequencies;

import java.util.Arrays;

import com.yahoo.sketches.hashmaps.HashMapWithImplicitDeletes;

/**
 * The frequent-items sketch is useful for keeping approximate counters for keys (map from key (long) to value (long)).  
 * The sketch is initialized with an error parameter eps. The sketch will keep roughly 1/eps counters.
 * When the sketch is updated with a key and increment, the corresponding counter is incremented or, 
 * if there is no counter for that key, a new counter is created. 
 * If the sketch reaches its maximal allowed size, it removes some counter and decrements others.
 * The logic of the frequent-items sketch is such that the stored counts and real counts are never too different.
 * More specifically, for any key k, the sketch can return an estimate of the true frequency of k, along with
 * upper and lower bounds on the frequency (that hold deterministically). The sketch is guaranteed that, with high
 * probability over the randomness of the implementation, the difference between the upper bound and the estimate is at most 2*eps*n,
 * where n denotes the stream length (i.e, sum of all the frequencies),
 * and similarly for the lower bound and the estimate.
 * In practice, the difference is usually smaller.
 * 
 * Background:
 * This code implements a variant of what is commonly known as the "Misra-Gries algorithm" or "Frequent Items". 
 * Variants of it were discovered and rediscovered and redesigned several times over the years.
 * "Finding repeated elements", Misra, Gries, 1982 
 * "Frequency estimation of internet packet streams with limited space" Demaine, Lopez-Ortiz, Munro, 2002
 * "A simple algorithm for finding frequent elements in streams and bags" Karp, Shenker, Papadimitriou, 2003
 * "Efficient Computation of Frequent and Top-k Elements in Data Streams" Metwally, Agrawal, Abbadi, 2006
 * 
 * @author Justin8712
 */
public class FrequentItemsID {

  private HashMapWithImplicitDeletes counters;
  //maxSize is maximum number of counters stored
  private int maxSize;
  //offset will track total number of decrements performed on sketch
  private int offset;
  private int mergeError;
  private int streamLength=0;
  //sample_Size is maximum number of samples used to compute approximate median of counters when doing decrement
  private int sample_size;
  private long[] samples;
  
  /**
   * @param errorParameter
   * Determines the accuracy of the estimates returned by the sketch.
   * The space usage of the sketch is proportional to the inverse of errorParameter. 
   * If fewer than ~1/errorParameter different keys are inserted then the counts will be exact.  
   */
  public FrequentItemsID(double errorParameter) {
    if (errorParameter <= 0) throw new IllegalArgumentException("Received negative or zero value for maxSize.");
    this.maxSize = (int) (1/errorParameter)+1;
    counters = new HashMapWithImplicitDeletes(this.maxSize);
    this.offset = 0;
    if (this.maxSize < 100) 
      this.sample_size = this.maxSize;
    else
      this.sample_size = 100;
    samples = new long[sample_size];
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
  
  public long getEstimate(long key) { 
    //the logic below returns the count of associated counter if key is tracked.
    //If the key is not tracked and fewer than maxSize counters are in use, 0 is returned.
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
   
   public long getEstimateLowerBound(long key)
   {
     if(getEstimate(key) == 0)
       return 0;
       
     if((getEstimate(key)-offset-mergeError) < 0)
       return 0;
       
     return (getEstimate(key)-offset-mergeError);
   }

  /**
   * @return the maximal error of the estimate one gets from get(key).
   * 
   */
  
  public long getMaxError() {
      return offset + mergeError;
  }
  
  /**
   * @param key 
   * Process a key (specified as a long) update and treat the increment as 1
   */
    
  public void update(long key) {
    update(key, 1);
  }


  
  /**
   * @param key A key (as long) whose frequency is to be incremented. The key cannot be null.
   * @param increment Amount to increment frequency by.
   * 
   */
  public void update(long key, long increment) {
    this.streamLength += increment;
    counters.adjust(key, increment);  
    if(counters.getSize() > this.maxSize) {
      purge();
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
    int limit = this.sample_size;
    if (limit > counters.getSize())
      limit = counters.getSize();
    
    long[] values = counters.ProtectedGetValues();
    int num_samples = 0;
    int i = 0;

    while(num_samples < limit){
      if(counters.isActive(i)){
        samples[num_samples] = values[i];
        num_samples++;
      }
      i++;
    }
    
    Arrays.sort(samples, 0, limit);
    long val = samples[limit/2];
    counters.adjustAllValuesBy(-1 * val);
    counters.keepOnlyLargerThan(0);
    this.offset += val; 
  }

  
   /**
    * @param other
    * Another FrequentItemsID sketch. Potentially of different size. 
    * @return pointer to the sketch resulting in adding the approximate counts of another sketch. 
    * This method does not create a new sketch. The sketch whose function is executed is changed.
    */
    
   public FrequentItemsID merge(FrequentItemsID other) {       
     this.streamLength += other.streamLength;
     this.mergeError += other.getMaxError();
     
     long[] other_keys = other.counters.getKeys();
     long[] other_values = other.counters.getValues();
      
     for (int i=other_keys.length; i-->0;) { 
       this.update(other_keys[i], other_values[i]);
     }
       
     return this;
   }
  
   /**
    * @return an array containing all keys exceed the frequency threshold of roughly 1/errorParameter+1
    * 
    */
   
   public long[] getFrequentKeys() {
     int count = 0;
     long[] keys = counters.ProtectedGetKey();
     
     for(int i = 0; i < counters.getLength(); i++) {
       if(counters.isActive(i) && (getEstimate(keys[i]) >= (this.streamLength / this.maxSize))) {
         count++;
       }
     }
     
     long[] freq_keys = new long[count];
     count = 0;
     for(int i = counters.getLength(); i-->0;) {
       if(counters.isActive(i) && (getEstimate(keys[i]) >= (this.streamLength / this.maxSize))) {
         freq_keys[count] = keys[i];
         count++;
       }
     }

     return freq_keys;
   }

}
