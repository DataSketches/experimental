package com.yahoo.sketches.frequencies;

import java.util.Collection;
import org.testng.Assert;
import org.testng.annotations.Test;

public class MasterFETester{

  public static void main(String[] args) {
    test2();
    StressTest();
  }
  
  /**
   * @param prob the probability of success for the geometric distribution. 
   * @return a random number generated from the geometric distribution.
   */
  static private long randomGeometricDist(double prob) {
    assert(prob > 0.0 && prob < 1.0);
    return (long) (Math.log(Math.random()) / Math.log(1.0 - prob));
  }
  
  @Test
  public void testRandomGeometricDist() {
    long maxKey = 0L;
    double prob = .1;
    for (int i=0; i<100; i++) {
      long key = randomGeometricDist(prob) ;
      if (key > maxKey) maxKey = key;
      // If you succeed with probability p the probability 
      // of failing 20/p times is smaller than 1/2^20.
      Assert.assertTrue(maxKey < 20.0/prob);
    }
  }
  
  private static void StressTest(){
      int n =   2000000;
      int size = 100000; 
      double error_tolerance = 1.0/size;
      int trials = 100;
      
      int numEstimators = 2;
      for (int h=0; h<numEstimators; h++){
        double total_updates_per_s = 0;
        FrequencyEstimator estimator = newFrequencyEstimator(error_tolerance, .1, h);
        for(int trial = 0; trial < trials; trial++)
        {
          estimator = newFrequencyEstimator(error_tolerance, .1, h);
          int key=0;
          double startTime = System.nanoTime();
          for (int i=0; i<n; i++) {
            //long key = randomGeometricDist(prob);
            estimator.update(key++);
          }
          double endTime = System.nanoTime();
          double timePerUpdate = (endTime-startTime)/(1000000.0*n);
          double updatesPerSecond = 1000.0/timePerUpdate;
          total_updates_per_s +=updatesPerSecond;
        }
        System.out.format("%s Performes %.2f million updates per second.\n",
            estimator.getClass().getSimpleName(),
            (total_updates_per_s/trials));
      }
    }
  
  private static void test2(){
    int numEstimators = 2; 
    int n = 138222;
    double error_tolerance = 1.0/100000;
    
    FrequencyEstimator[] estimators = new FrequencyEstimator[numEstimators];
    for (int h=0; h<numEstimators; h++){
      estimators[h] = newFrequencyEstimator(error_tolerance, .1, h);
    }
    
    PositiveCountersMap realCounts = new PositiveCountersMap();
    long key;
    double prob = .001;
    for (int i=0; i<n; i++) {   
      key = randomGeometricDist(prob);
      realCounts.increment(key);
      for(int h=0; h<numEstimators; h++)
        estimators[h].update(key); 
    }
    
    for(int h=0; h<numEstimators; h++) {
      long[] freq = estimators[h].getFrequentKeys();
      for(int i = 0; i < freq.length; i++) {
        if(estimators[h].getEstimate(freq[i]) < (long)(error_tolerance * n)) {
          System.out.format("length is: %d, i is %d, freq[i] is: %d, Estimate is %d, threshold is %f", freq.length, i, freq[i], estimators[h].getEstimate(freq[i]), error_tolerance*n);
        }
        if(estimators[h].getEstimate(freq[i]) < (long)(error_tolerance * n))
        {
          System.out.format("Error 1. h is: %d and estimate is: %d and threshold is: %d \n", h, estimators[h].getEstimate(freq[i]), (long)(error_tolerance * n));
        } 
      } 
      Collection<Long> keysCollection = realCounts.keys();

      int found;
      for (long the_key : keysCollection) {
        if(realCounts.get(the_key) > (long)(error_tolerance*n)) {
          found = 0;
          for(int i = 0; i < freq.length; i++) {
            if(freq[i] == the_key) {
              found = 1;
            }
          }  
          if(found != 1)
            System.out.println("Error 2");
        }  
      }
    }
    System.out.println("Completed tests of returned lists of potentially frequent items.");
  }
  
  static private FrequencyEstimator newFrequencyEstimator(double error_parameter, double failure_prob, int i){
    switch (i){
      case 0: return new SpaceSaving(error_parameter);
      case 1: return new CountMinFastFE(error_parameter, failure_prob);
    }
    return null;
  }
 }