/*
 * Copyright 2015, Yahoo! Inc.
 * Licensed under the terms of the Apache License 2.0. See LICENSE file at the project root for terms.
 */
package com.yahoo.sketches.frequencies;

import java.util.Arrays;
import java.util.Collection;

/**
 * The frequent-items sketch is useful for keeping approximate counters for keys (map from key (long) to value (long)).  
 * The sketch is initialized with a maximal size parameter. The sketch will keep at most that number of positive counters at any given time.
 * When the sketch is updated with a key, the corresponding counter is incremented by 1 or, if there is no counter for that key, a new counter is created. 
 * If the sketch reaches its maximal allowed size, it removes some counter and decrements other.
 * The logic of the frequent-items sketch is such that the stored counts and real counts are never too different.
 * More explicitly 
 * 1) The estimate from the sketch is never larger than the real count
 * 2) The estimate from the sketch is never smaller than the real count minus the guaranteed error
 * 3) The guaranteed error is at most the number of updates to sketch divided by its maximal size.
 * 
 * Background:
 * The algorithm is most commonly known as the "Misra-Gries algorithm", "frequent items" or "space-saving". 
 * It was discovered and rediscovered and redesigned several times over the years.
 * "Finding repeated elements", Misra, Gries, 1982 
 * "Frequency estimation of internet packet streams with limited space" Demaine, Lopez-Ortiz, Munro, 2002
 * "A simple algorithm for finding frequent elements in streams and bags" Karp, Shenker, Papadimitriou, 2003
 * "Efficient Computation of Frequent and Top-k Elements in Data Streams" Metwally, Agrawal, Abbadi, 2006
 * 
 * @author edo
 */

public class FrequentItemsNaiveNative extends FrequencyEstimator {
  private PositiveCountersMap counters;
  private int maxSize;
  private int maxError;
  
  /**
   * @param maxSize (must be positive)
   * Gives the maximal number of positive counters the sketch is allowed to keep.
   * This should be thought of as the limit on its space usage. The size is dynamic.
   * If fewer than maxSize different keys are inserted the size will be smaller 
   * that maxSize and the counts will be exact.  
   */
  public FrequentItemsNaiveNative(double errorTolerance) {
    super(errorTolerance);
    this.maxSize = (int)(1.0/getErrorTolerance())+1;
    counters = new PositiveCountersMap();
    this.maxError = 0;
  }
  public FrequentItemsNaiveNative(){
    this(0.01);
  }
  
  /**
   * @return the number of positive counters in the sketch.
   */
  public long nnz() {
    return counters.nnz();
  }
  
  /**
   * @param key whose count estimate is returned.
   * @return the approximate count for the key.
   * It is guaranteed that
   * 1) get(key) <= real count
   * 2) get(key) >= real count - getMaxError() 
   */
  @Override
  public long getEstimate(long key) {
    return counters.get(key);
  }

  /**
   * @return the maximal error of the estimate one gets from get(key).
   * Note that the error is one sided. if the real count is realCount(key) then
   * get(key) <= realCount(key) <= get(key) + getMaxError() 
   */
  public int getMaxError() {
    return maxError;
  }
  
  /**
   * @param key 
   * A key (as long) to be added to the sketch. The key cannot be null.
   */
  @Override
  public void update(long key) {
    update(key,1);
  }
  

  @Override
  public long getEstimateLowerBound(long key) {
    return getEstimate(key);
  }

  @Override
  public long getEstimateUpperBound(long key) {
    return getEstimate(key) + maxError;
  }

  @Override
  public long[] getFrequentKeys() {
    Collection<Long> keysCollection = counters.keys();
    int n = keysCollection.size();
    long[] keys = new long[n];
    int i=0;
    for (long key : keysCollection){
      keys[i] = key;
      i++;
    }
    return keys;
  }

  @Override
  public void update(long key, long value) {
    counters.increment(key,value);  
    if (counters.nnz() > maxSize){ 
      counters.decerementAll();
      maxError++;
    }
  }
  
  /**
   * @param that
   * Another FrequentItems sketch. Potentially of different size. 
   * @return pointer to the sketch resulting in adding the approximate counts of another sketch. 
   * This method does not create a new sketch. The sketch whose function is executed is changed.
   */
  @Override
  public FrequencyEstimator merge(FrequencyEstimator other) {
    if (!(other instanceof FrequentItemsNaiveNative)) throw new IllegalArgumentException("FrequentItemsNaiveTrove can only merge with other FrequentItemsNaiveNative");
    FrequentItemsNaiveNative otherCasted = (FrequentItemsNaiveNative)other;
    // Summing up the counts
    counters.increment(otherCasted.counters);
    
    // The count for a specific key could have been
    // deleted the maximal number of times in both sketches
    this.maxError += otherCasted.maxError;
    
    long nnzNow = counters.nnz();
    if (nnzNow > maxSize){ 
      // Crude way to find mind the value of the 
      // maxSize'th largest element in the array
      long[] values = new long[(int)nnzNow];
      int i=0;
      for (long value : counters.values()) {
        values[i] = value;
        i++;
      }
      Arrays.sort(values);
      assert(nnzNow == values.length);
      long delta = values[(int)nnzNow - maxSize];
      counters.decerementAll(delta);
      maxError += delta;
    }
    return this;
  }

}

