/*
 * Copyright 2015, Yahoo! Inc.
 * Licensed under the terms of the Apache License 2.0. See LICENSE file at the project root for terms.
 */
package com.yahoo.sketches.frequencies;

/**
 * @author Edo Liberty, Justin Thaler
 */

/**
 * Abstract class for a FrequencyEstimator algorithm. Supports
 * the ability to process a data stream of (item, increment) pairs,
 * where item is an identifier, specified as a long, and increment is a 
 * non-negative integer that is also specified as a long. The frequency
 * of an identifier is defined to be the sum of associated the increments.
 * Any FrequencyEstimator algorithm must be able to:
 *  1) estimate the frequency of an identifier, 
 *  2) return upper and lower bounds on the frequency (depending on the 
 *     implementation, these bounds may hold deterministically, or with 
 *     high probability), 
 *  3) return an upper bound on the maximum error in any estimate 
 *     (which also might hold deterministically or with high probability, 
 *     depending on implementation)
 *   4) Return an array of keys whose frequencies might be above a certain 
 *      threshold (specifically, the threshold is 1/errorTolerance + 1)
 *   5) merge itself with another FrequencyEstimator algorithm from the same 
 *      instantiation of the abstract class.
 * 
 */
public abstract class FrequencyEstimator {
 
  private double errorTolerance;
  private boolean errorToleranceAlreadyAssigned = false;
  protected final double SMALLEST_ERROR_TOLERANCE_ALLOWED = 1E-7; // epsilon
  protected final double ACCEPTABLE_FAILURE_PROBABILITY = 1E-10; // delta 
  
  /**
   * Constructs a FrequencyEstimator sketch
   * 
   * @param errorTolerance the acceptable relative error in the estimates of 
   * the sketch. The maximal error in the frequency estimate should not 
   * by more than the error tolerance times the number of updates.
   * @param failureProb the acceptable failure probability for any point query.
   * For some instantiations of the abstract class, the algorithm will be deterministic
   * and hence the failure probability will be 0.
   * **Warning**: the memory footprint of this class is inversely proportional
   * to the error tolerance (and possibly the failure probability).
   */
  public FrequencyEstimator(double errorTolerance, double failureProb){
    setErrorTolerance(errorTolerance, failureProb);
  }
  
  
  /**
   * Constructs a FrequencyEstimator sketch
   * 
   * @param errorTolerance the acceptable relative error in the estimates of 
   * the sketch. The maximal error in the frequency estimate should not 
   * by more than the error tolerance times the number of updates.
   * Sets failure probability to default value of .1.
   * **Warning**: the memory footprint of this class is inversely proportional
   * to the error tolerance!
   */
  public FrequencyEstimator(double errorTolerance){
    setErrorTolerance(errorTolerance, .1);
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
   * @return An upper bound on the maximum error of getEstimate(key) for any key.
   * This upper bound may only hold for each key with probability failure_prob.
   */
  abstract public long getMaxError();
  
  
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
  protected void setErrorTolerance(double errorTolerance, double failureProb){
    if (errorTolerance > 1.0) throw new IllegalArgumentException("Received error tolerance larger than 1.0");
    if (failureProb > 1.0) throw new IllegalArgumentException("Received failure probability larger than 1.0");
    if (errorTolerance < SMALLEST_ERROR_TOLERANCE_ALLOWED) throw new 
       IllegalArgumentException("Received error tolerance smaller than minimla allowed value of " + SMALLEST_ERROR_TOLERANCE_ALLOWED);
    if (failureProb < ACCEPTABLE_FAILURE_PROBABILITY) throw new 
    IllegalArgumentException("Received failure probability smaller than minimla allowed value of " + ACCEPTABLE_FAILURE_PROBABILITY);
    if (errorToleranceAlreadyAssigned) throw new IllegalStateException("The error tolerance of a sketch can only be set once per sketch object.");
    this.errorToleranceAlreadyAssigned = true;
    this.errorTolerance = errorTolerance;
  }
 
}
