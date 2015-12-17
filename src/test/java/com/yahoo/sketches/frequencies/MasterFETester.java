package com.yahoo.sketches.frequencies;

import java.util.Collection;
import org.testng.Assert;
import org.testng.annotations.Test;


public class MasterFETester{

  public static void main(String[] args) {
    FETest();
    System.out.println("Done FE Test");
    realCountsInBoundsAfterMerge();
    System.out.println("Done realCountsoinBoundsAFterMerge Test");
    strongMergeTest();
    System.out.println("Done StrongMerge Test");
    updateOneTime();
    System.out.println("Done UpdateOneTime Test");
    ErrorTestZipfBigParam();
    System.out.println("Done Error Test Big Param");
    ErrorTestZipfSmallParam();
    System.out.println("Done Error Test Small Param");
    ErrorTestZipfBigParamSmallSketch();
    System.out.println("Done Error Test BigParamSmallSketch");
  }
  
  @Test
  static private void updateOneTime() {
    int size = 100;
    double error_tolerance = 1.0/size;
    double delta = .01;
    int numEstimators = 6;
    for (int h=0; h<numEstimators; h++){
      FrequencyEstimator estimator = newFrequencyEstimator(error_tolerance, delta, h);
      Assert.assertEquals(estimator.getEstimateUpperBound(13L), 0);
      Assert.assertEquals(estimator.getEstimateLowerBound(13L), 0);
      Assert.assertEquals(estimator.getMaxError(), 0);
      Assert.assertEquals(estimator.getEstimate(13L), 0);
      estimator.update(13L);
      //Assert.assertEquals(estimator.getEstimate(13L), 1);
    }
  }
  
  /**
   * @param prob the probability of success for the geometric distribution. 
   * @return a random number generated from the geometric distribution.
   */
  static private long randomGeometricDist(double prob) {
    assert(prob > 0.0 && prob < 1.0);
    return 1 + (long) (Math.log(Math.random()) / Math.log(1.0 - prob));
  }
  
  static double zeta(long n, double theta) 
  {

    // the zeta function, used by the below zipf function
    // (this is not often called from outside this library)
    // ... but have made it public now to speed things up

    int i;
    double ans=0.0;
    
    for (i=1; i <= n; i++)
      ans += Math.pow(1./i, theta);
    return(ans);
  }

  
 //this draws values from the zipf distribution
 // n is range, theta is skewness parameter
  // theta = 0 gives uniform dbn,
  // theta > 1 gives highly skewed dbn. 
  static private long zipf(double theta, long n, double zetan) {
    double alpha;
    double eta;
    double u;
    double uz;
    double val;

    // randinit must be called before entering this procedure for
    // the first time since it uses the random generators

    alpha = 1. / (1. - theta);
    eta = (1. - Math.pow(2./n, 1. - theta)) / (1. - zeta(2,theta)/zetan);

    u = 0.0;
    while(u == 0.0)
      u = Math.random();
    uz = u * zetan;
    if (uz < 1.) val = 1;
    else if (uz < (1. + Math.pow(0.5, theta))) val = 2;
    else val = 1 + (n * Math.pow(eta*u - eta + 1., alpha));

    return (long) val;
  }
  
  public void testRandomGeometricDist() {
    long maxKey = 0L;
    double prob = .1;
    for (int i=0; i<100; i++) {
      long key = randomGeometricDist(prob);
      if (key > maxKey) maxKey = key;
      // If you succeed with probability p the probability 
      // of failing 20/p times is smaller than 1/2^20.
      Assert.assertTrue(maxKey < 20.0/prob);
    }
  }
  
  
  @Test
  private static void FETest(){
    int numEstimators = 6; 
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
      key = randomGeometricDist(prob)+1;
      realCounts.increment(key);
      for(int h=0; h<numEstimators; h++)
        estimators[h].update(key); 
    }
    
    for(int h=0; h<numEstimators; h++) {
      long[] freq = estimators[h].getFrequentKeys();
     
      for(int i = 0; i < freq.length; i++) 
        Assert.assertTrue((estimators[h].getEstimateUpperBound(freq[i]) >= (long)(error_tolerance * n)));
      
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
          Assert.assertTrue(found == 1);
        }  
      }
    }
  }
  
  @Test
  private static void ErrorTestZipfSmallParam(){
    int size = 512;
    int n = 20000*size; 
    double delta = .1;
    double error_tolerance = 1.0/size;
    int trials = 1;
    long stream[] = new long[n];
    
    double zet=zeta(n,0.7);
    PositiveCountersMap realCounts = new PositiveCountersMap();
    
    for(int i = 0; i <n; i++) {
      stream[i] = zipf(0.7, n, zet);
      realCounts.increment(stream[i]);
    }
    
    int numEstimators = 6;
    
    for (int h=0; h<numEstimators; h++){
      FrequencyEstimator estimator = newFrequencyEstimator(error_tolerance, .1, h);
      
      for(int trial = 0; trial < trials; trial++)
      {
        estimator = newFrequencyEstimator(error_tolerance, delta, h);
        for (int i=0; i<n; i++) {
          //long key = randomGeometricDist(prob);
          estimator.update(stream[i]);
        }
        long sum = 0;
        long max_error = 0;
        long error;
        long distinct_count = 0;
        long max_freq = 0;
        long max_error_key = 0;
        long max_freq_key = 0;
        
        Collection<Long> keysCollection = realCounts.keys();

        for (long the_key : keysCollection) {
          distinct_count++;
          if(realCounts.get(the_key) > max_freq) {
            max_freq = realCounts.get(the_key);
            max_freq_key = the_key;
          }
          if(realCounts.get(the_key) > estimator.getEstimate(the_key)) {
            error = (realCounts.get(the_key) - estimator.getEstimate(the_key));
            if(error > max_error)
            {
              max_error = error;
              max_error_key = the_key;
            }
            sum = sum + error;
          }
          else {
            error = (estimator.getEstimate(the_key)- realCounts.get(the_key));
            if(error > max_error)
            {
              max_error = error;
              max_error_key = the_key;
            }
            sum = sum + error;
          }
        }
        Assert.assertTrue(max_error <= 2*n*error_tolerance); 
      }  
    }
  }
  
  @Test
  private static void ErrorTestZipfBigParam(){
    int size = 512;
    int n = 20000*size; 
    double delta = .1;
    double error_tolerance = 1.0/size;
    int trials = 1;
    long stream[] = new long[n];
    
    double zet=zeta(n,1.1);
    PositiveCountersMap realCounts = new PositiveCountersMap();
    
    System.out.println("starting loop");
    for(int i = 0; i <n; i++) {
      stream[i] = zipf(1.1, n, zet);
      realCounts.increment(stream[i]);
    }
    System.out.println("done loop");
    
    int numEstimators = 6;
    
    for (int h=0; h<numEstimators; h++){
      FrequencyEstimator estimator = newFrequencyEstimator(error_tolerance, .1, h);
      
      for(int trial = 0; trial < trials; trial++)
      {
        estimator = newFrequencyEstimator(error_tolerance, delta, h);
        for (int i=0; i<n; i++) {
          //long key = randomGeometricDist(prob);
          estimator.update(stream[i]);
        }
        long sum = 0;
        long max_error = 0;
        long error;
        long distinct_count = 0;
        long max_freq = 0;
        
        Collection<Long> keysCollection = realCounts.keys();

        for (long the_key : keysCollection) {
          distinct_count++;
          if(realCounts.get(the_key) > max_freq) {
            max_freq = realCounts.get(the_key);
          }
          if(realCounts.get(the_key) > estimator.getEstimate(the_key)) {
            error = (realCounts.get(the_key) - estimator.getEstimate(the_key));
            if(error > max_error)
              max_error = error;
            sum = sum + error;
          }
          else {
            error = ( estimator.getEstimate(the_key) - realCounts.get(the_key));
            if(error > max_error)
              max_error = error;
            sum = sum + error;
          } 
        }
        Assert.assertTrue(max_error <= 2*n*error_tolerance);    
      }  
    }
  }
  
  @Test
  private static void ErrorTestZipfBigParamSmallSketch(){
    int size = 64;
    int n = 20000*size; 
    double delta = .1;
    double error_tolerance = 1.0/size;
    int trials = 1;
    long stream[] = new long[n];
    
    double zet=zeta(n,1.1);
    PositiveCountersMap realCounts = new PositiveCountersMap();
    
    System.out.println("starting loop");
    for(int i = 0; i <n; i++) {
      stream[i] = zipf(1.1, n, zet);
      realCounts.increment(stream[i]);
    }
    System.out.println("done loop");
    
    int numEstimators = 6;
    
    for (int h=0; h<numEstimators; h++){
      FrequencyEstimator estimator = newFrequencyEstimator(error_tolerance, .1, h);
      
      for(int trial = 0; trial < trials; trial++)
      {
        estimator = newFrequencyEstimator(error_tolerance, delta, h);
        for (int i=0; i<n; i++) {
          //long key = randomGeometricDist(prob);
          estimator.update(stream[i]);
        }
        long sum = 0;
        long max_error = 0;
        long error;
        long distinct_count = 0;
        long max_freq = 0;
        
        Collection<Long> keysCollection = realCounts.keys();

        for (long the_key : keysCollection) {
          distinct_count++;
          if(realCounts.get(the_key) > max_freq) {
            max_freq = realCounts.get(the_key);
          }
          if(realCounts.get(the_key) > estimator.getEstimate(the_key)) {
            error = (realCounts.get(the_key) - estimator.getEstimate(the_key));
            if(error > max_error)
              max_error = error;
            sum = sum + error;
          }
          else {
            error = ( estimator.getEstimate(the_key) - realCounts.get(the_key));
            if(error > max_error)
              max_error = error;
            sum = sum + error;
          } 
        }
        Assert.assertTrue(max_error <= 2*n*error_tolerance);    
      }  
    }
  }
  
  @Test
  private static void realCountsInBoundsAfterMerge() {
    int n = 1000000;
    int size = 15000;
    double delta = .1;
    double error_tolerance = 1.0/size;
  
    double prob1 = .01;
    double prob2 = .005;
    int numEstimators = 6;
    
    System.out.println("start");
    for(int h=0; h<numEstimators; h++) {
      FrequencyEstimator estimator1 = newFrequencyEstimator(error_tolerance, delta, h);
      FrequencyEstimator estimator2 = newFrequencyEstimator(error_tolerance, delta, h);
      PositiveCountersMap realCounts = new PositiveCountersMap();
      for (int i=0; i<n; i++) {
        long key1 = randomGeometricDist(prob1)+1;
        long key2 = randomGeometricDist(prob2)+1;
        
        estimator1.update(key1);
        estimator2.update(key2);
        
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
        }
      }
      Assert.assertTrue(bad <= delta * i);
    }
  }
  
  @Test
  private static void strongMergeTest() {
    int n = 10000;
    int size = 1500;
    double delta = .1;
    double error_tolerance = 1.0/size;
    int num_to_merge = 10;
    FrequencyEstimator[] estimators = new FrequencyEstimator[num_to_merge];
  
    double prob = .01;
    int numEstimators = 6;
    
    System.out.println("start of more stringent merge test");
    for(int h=0; h<numEstimators; h++) {
      for(int z = 0; z < num_to_merge; z++) 
        estimators[z] = newFrequencyEstimator(error_tolerance, delta, h);
      
      PositiveCountersMap realCounts = new PositiveCountersMap();
      for (int i=0; i<n; i++) {
        for(int z = 0; z < num_to_merge; z++) {
          long key = randomGeometricDist(prob)+1;
        
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
        }
      }
      Assert.assertTrue(bad <= delta * i); 
    }
  }
  
  static private FrequencyEstimator newFrequencyEstimator(double error_parameter, double failure_prob, int i){
    switch (i){
      //case 2: return new SpaceSaving(error_parameter);
      //case 3: return new SpaceSavingTrove(error_parameter);
      //case 0: return new CountMinFastFE(error_parameter, failure_prob);
      //case 1: return new CountMinFastFECU(error_parameter, failure_prob);
      case 0: return new SpaceSavingGood(error_parameter);
      case 1: return new FrequentItems(error_parameter);
      case 2: return new FrequentItemsLPWR(error_parameter);
      case 3: return new FrequentItemsDHWR(error_parameter);
      case 4: return new FrequentItemsEfficientDeletes(error_parameter);
      case 5: return new FrequentItemsID(error_parameter);
    }
    return null;
  }
 }