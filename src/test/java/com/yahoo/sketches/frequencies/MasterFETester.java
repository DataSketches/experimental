package com.yahoo.sketches.frequencies;

import java.util.Collection;
import org.testng.Assert;
import org.testng.annotations.Test;

public class MasterFETester{

  public static void main(String[] args) {
    FETest();
    StressTest();
    realCountsInBoundsAfterMerge();
    strongMergeTest();
    updateOneTime();
  }
  
  static private void updateOneTime() {
    int size = 100;
    double error_tolerance = 1.0/size;
    double delta = .01;
    int numEstimators = 2;
    for (int h=0; h<numEstimators; h++){
      FrequencyEstimator estimator = newFrequencyEstimator(error_tolerance, delta, h);
      Assert.assertEquals(estimator.getEstimateUpperBound(13L), 0);
      Assert.assertEquals(estimator.getEstimateLowerBound(13L), 0);
      //Assert.assertEquals(estimator.getMaxError(), 0);
      Assert.assertEquals(estimator.getEstimate(13L), 0);
      estimator.update(13L);
      Assert.assertEquals(estimator.getEstimate(13L), 1);
    }
    System.out.println("completed one update test");
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
      double delta = .1;
      double error_tolerance = 1.0/size;
      int trials = 1;
      
      int numEstimators = 2;
      for (int h=0; h<numEstimators; h++){
        double total_updates_per_s = 0;
        FrequencyEstimator estimator = newFrequencyEstimator(error_tolerance, .1, h);
        for(int trial = 0; trial < trials; trial++)
        {
          estimator = newFrequencyEstimator(error_tolerance, delta, h);
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
  
  private static void FETest(){
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
        if(estimators[h].getEstimateUpperBound(freq[i]) < (long)(error_tolerance * n)) {
          System.out.format("length is: %d, i is %d, freq[i] is: %d, Estimate is %d, threshold is %f", freq.length, i, freq[i], estimators[h].getEstimate(freq[i]), error_tolerance*n);
        }
        if(estimators[h].getEstimateUpperBound(freq[i]) < (long)(error_tolerance * n))
        {
          System.out.format("Error 1. h is: %d and estimate is: %d and threshold is: %d \n", h, estimators[h].getEstimateUpperBound(freq[i]), (long)(error_tolerance * n));
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
  
  private static void realCountsInBoundsAfterMerge() {
    int n = 1000000;
    int size = 15000;
    double delta = .1;
    double error_tolerance = 1.0/size;
  
    double prob1 = .01;
    double prob2 = .005;
    int numEstimators = 2;
    
    System.out.println("start");
    for(int h=0; h<numEstimators; h++) {
      FrequencyEstimator estimator1 = newFrequencyEstimator(error_tolerance, delta, h);
      FrequencyEstimator estimator2 = newFrequencyEstimator(error_tolerance, delta, h);
      PositiveCountersMap realCounts = new PositiveCountersMap();
      for (int i=0; i<n; i++) {
        long key1 = randomGeometricDist(prob1);
        long key2 = randomGeometricDist(prob2);
        
        estimator1.update(key1);
        estimator2.update(key2);
        
        //System.out.format("key1: %d, estimate: %d, lowerbound: %d\n", key1, estimator1.getEstimate(key1), estimator1.getEstimateLowerBound(key1));
        //System.out.format("key2: %d, estimate: %d, lowerbound: %d\n", key2, estimator2.getEstimate(key2), estimator2.getEstimateLowerBound(key2));
      
        // Updating the real counters
        realCounts.increment(key1);
        realCounts.increment(key2);
      }
      FrequencyEstimator merged = estimator1.merge(estimator2);

      int bad = 0;
      int i = 0;
      for ( long key : realCounts.keys()) {
        i = i + 1;
      
        long realCount = realCounts.get(key);
        long upperBound = merged.getEstimateUpperBound(key);
        long lowerBound = merged.getEstimateLowerBound(key);

        if(upperBound <  realCount || realCount < lowerBound) {
          bad = bad + 1;
          System.out.format("bad estimate in class %s, key is: %d, h is %d, upperbound: %d, realCount: %d, "
              + "lowerbound: %d \n", merged.getClass().getSimpleName(), key, h, upperBound, realCount, lowerBound);
        }
      }
      if(bad > delta * i) {
        System.out.format("too many bad estimates after merging estimators for estimator %s \n",
            merged.getClass().getSimpleName());
      }
      System.out.format("bad is %d\n", bad);
    }
    System.out.println("Completed test of counts in bounds after union operation");
  }
  
  private static void strongMergeTest() {
    int n = 100000;
    int size = 1500;
    double delta = .1;
    double error_tolerance = 1.0/size;
    int num_to_merge = 10;
    FrequencyEstimator[] estimators = new FrequencyEstimator[num_to_merge];
  
    double prob = .01;
    int numEstimators = 2;
    
    System.out.println("start of more stringent merge test");
    for(int h=0; h<numEstimators; h++) {
      for(int z = 0; z < num_to_merge; z++) 
        estimators[z] = newFrequencyEstimator(error_tolerance, delta, h);
      
      PositiveCountersMap realCounts = new PositiveCountersMap();
      for (int i=0; i<n; i++) {
        for(int z = 0; z < num_to_merge; z++) {
          long key = randomGeometricDist(prob);
        
          estimators[z].update(key);
          // Updating the real counters
          realCounts.increment(key);
        }
      }
      
      FrequencyEstimator merged = estimators[0];
      for(int z = 0; z < num_to_merge; z++) {
        if(z == 0)
          continue;
        merged = merged.merge(estimators[z]);
      }

      int bad = 0;
      int i = 0;
      for ( long key : realCounts.keys()) {
        i = i + 1;
      
        long realCount = realCounts.get(key);
        long upperBound = merged.getEstimateUpperBound(key);
        long lowerBound = merged.getEstimateLowerBound(key);

        if(upperBound <  realCount || realCount < lowerBound) {
          bad = bad + 1;
          System.out.format("bad estimate in class %s, key is: %d, h is %d, upperbound: %d, realCount: %d, "
              + "lowerbound: %d \n", merged.getClass().getSimpleName(), key, h, upperBound, realCount, lowerBound);
        }
      }
      if(bad > delta * i) {
        System.out.format("too many bad estimates after merging estimators for estimator %s \n",
            merged.getClass().getSimpleName());
      }
      System.out.format("bad is %d\n", bad);
    }
    System.out.println("Completed more stringent test of merge operation");
  }
  
  static private FrequencyEstimator newFrequencyEstimator(double error_parameter, double failure_prob, int i){
    switch (i){
      case 0: return new SpaceSaving(error_parameter);
      case 1: return new CountMinFastFE(error_parameter, failure_prob);
    }
    return null;
  }
 }