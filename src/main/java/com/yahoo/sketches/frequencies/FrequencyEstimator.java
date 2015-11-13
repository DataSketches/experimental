/*
 * Copyright 2015, Yahoo! Inc.
 * Licensed under the terms of the Apache License 2.0. See LICENSE file at the project root for terms.
 */
package com.yahoo.sketches.frequencies;

/**
 * @author Edo Liberty
 */
public abstract class FrequencyEstimator {
 
  private double errorTolerance;
  private boolean errorToleranceAlreadyAssigned = false;
  protected final double SMALLEST_ERROR_TOLERANCE_ALLOWED = 1E-7; // epsilon
  protected final double ACCEPTABLE_FAILURE_PROBABILITY = 1E-10; // delta 
  
  /**
   * @param errorTolerance the acceptable relative error in the estimates of 
   * the sketch. The maximal error in the frequency estimate should not 
   * by more than the error tolerance times the number of updates.
   * **Warning**: the memory footprint of this class is inversely proportional
   * to the error tolerance!
   */
  public FrequencyEstimator(double errorTolerance){
    setErrorTolerance(errorTolerance);
  }
  
  /**
   * Default constructor uses error tolerance of 1%. 
   */
  public FrequencyEstimator(){
    this(0.01);
  }
 
  /**
   * @param key for which the frequency should be increased.  
   * The frequency of a key is equal to the number of times
   * the function increment(key) was called.
   */
  abstract public void update(long key);
  
  /**
   * @param key for which the frequency should be increased.
   * @param value by which the frequency of the key should be increased.
   * The value must by non-negative. 
   */
  abstract public void update(long key, long value);
  
  /**
   * @param key for which an estimate of the frequency is required.
   * The exact frequency of a key is the number of times the function increment(key)
   * was executed. 
   * @return an estimate of the frequency 
   */
  abstract public long getEstimate(long key);
  
  /**
   * @param key the key for which the frequency lower bound is needed. 
   * @return a lower bound on the frequency. That is, a number which is
   * guaranteed to be no larger than the real frequency.
   */
  abstract public long getEstimateLowerBound(long key);
  
  /**
   * @param key the key for which the frequency upper bound is needed. 
   * @return an upper bound on the frequency. That is, a number which is
   * guaranteed to be no smaller than the real frequency.
   */
  abstract public long getEstimateUpperBound(long key);
  
  /**
   * @return an array of keys containing all keys whose frequencies are
   * are least the error tolerance.   
   */
  abstract public long[] getFrequentKeys();
  
  /**
   * @param other another FrequenciesEstimator of the same class  
   * @return a pointer to a FrequencyEstimator whose estimates 
   * are within the guarantees of the largest error tolerance of
   * the two merged sketches.  
   */
  abstract public FrequencyEstimator merge(FrequencyEstimator other);

  
  /**
   * @return the error tolerance
   */
  public double getErrorTolerance() {
    return errorTolerance;
  }
  
  /**
   * @param errorTolerance sets the error tolerance if smaller than 1.0 and larger than the minimal allowed value.  
   */
  protected void setErrorTolerance(double errorTolerance){
    if (errorTolerance > 1.0) throw new IllegalArgumentException("Received error tolerance larger than 1.0");
    if (errorTolerance < SMALLEST_ERROR_TOLERANCE_ALLOWED) throw new 
       IllegalArgumentException("Received error tolerance smaller than minimla allowed value of " + SMALLEST_ERROR_TOLERANCE_ALLOWED);
    if (errorToleranceAlreadyAssigned) throw new IllegalStateException("The error tolerance of a sketch can only be set once per sketch object.");
    this.errorToleranceAlreadyAssigned = true;
    this.errorTolerance = errorTolerance;
  }
 
}
