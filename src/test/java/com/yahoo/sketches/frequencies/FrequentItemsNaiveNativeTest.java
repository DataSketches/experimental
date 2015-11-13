package com.yahoo.sketches.frequencies;

import org.testng.annotations.Test;

import gnu.trove.map.hash.TLongLongHashMap;

import org.testng.Assert;
import java.lang.Math;


/**
 * Tests FrequentItemsNaiveNative class
 * 
 * @author edo
 * 
 */
public class FrequentItemsNaiveNativeTest {
  
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void construct() {
    double errorTolerance = 0.1;
    FrequentItemsNaiveNative frequentItems1 = new FrequentItemsNaiveNative(errorTolerance);
    Assert.assertNotNull(frequentItems1);
    FrequentItemsNaiveNative frequentItems2 = new FrequentItemsNaiveNative();
    Assert.assertNotNull(frequentItems2);
    // Should throw exception
    @SuppressWarnings("unused")
    FrequentItemsNaiveNative frequentItems3 = new FrequentItemsNaiveNative(0.0);
  }
    
  @Test
  public void estimatesAreCorectBeofreDeletePhase() {
    double errorTolerance = 0.01;
    FrequentItemsNaiveNative frequentItems = new FrequentItemsNaiveNative(errorTolerance);
    for (long key=0L; key<95L; key++){
      frequentItems.update(key);
      Assert.assertTrue(frequentItems.getEstimate(key) == 1);
      Assert.assertTrue(frequentItems.getEstimateLowerBound(key) == frequentItems.getEstimateUpperBound(key));
    }
  }
  
  @Test
  public void realCountsInBounds() {
    int n = 4213;
    double errorTolerance = 0.01;
    double prob = .04; 
    FrequentItemsNaiveNative frequentItems = new FrequentItemsNaiveNative(errorTolerance);
    PositiveCountersMap realCounts = new PositiveCountersMap();
    for (int i=0; i<n; i++){   
      long key = randomGeometricDist(prob);
      frequentItems.update(key);
      realCounts.increment(key);
      Assert.assertTrue(frequentItems.getEstimateLowerBound(key) <= realCounts.get(key));
      Assert.assertTrue(frequentItems.getEstimateUpperBound(key) >= realCounts.get(key));   
    }
  }
  
  @Test
  public void errorWithinLimits() {
    int n = 100;
    int maxSize = 20;
    double errorTolerance = 1.0/maxSize;
    long key;
    double prob = .1;
    FrequentItemsNaiveNative frequentItems = new FrequentItemsNaiveNative(errorTolerance);
    for (int i=0; i<n; i++){
      key = randomGeometricDist(prob);
      frequentItems.update(key);
      Assert.assertTrue(frequentItems.getEstimateUpperBound(key) - frequentItems.getEstimateLowerBound(key) <= i*errorTolerance);  
    }
  }

  @Test
  public void errorWithinLimitsRandomValueUpdates() {
    int n = 10000;
    double errorTolerance = 0.00001;
    double prob = 0.00001;
    long sumOfValues = 0;
    FrequentItemsNaiveNative frequentItems = new FrequentItemsNaiveNative(errorTolerance);
    for (int i=0; i<n; i++){
      long key = randomGeometricDist(prob);
      long value = randomGeometricDist(prob);
      sumOfValues+=value;
      frequentItems.update(key,value);
      Assert.assertTrue(frequentItems.getEstimateUpperBound(key) - frequentItems.getEstimateLowerBound(key) <= sumOfValues*errorTolerance);  
    }
  }
  
  @Test
  public void stressTestUpdateTime() {
    int n = 100000;
    double errorTolerance = 0.001;
    FrequentItemsNaiveNative frequentItems = new FrequentItemsNaiveNative(errorTolerance);
    double prob = 0.1*errorTolerance;
    long[] keys = new long[n];
    for (int i=0; i<n; i++){
      keys[i] = randomGeometricDist(prob);
    }
    final long startTime = System.currentTimeMillis();
    for (int i=0; i<n; i++){
      frequentItems.update(keys[i]);
    }
    final long endTime = System.currentTimeMillis();
    double timePerUpdate = (double)(endTime-startTime)/(double)n;
    System.out.println("Amortized time per sketch update: " + timePerUpdate);
    Assert.assertTrue(timePerUpdate < 10E-3);
  }

  @Test
  public void realCountsInBoundsAfterMerge() {
    int n = 1000;
    double errorTolerance1 = 0.02;
    double errorTolerance2 = 0.01;
    double prob1 = .01;
    double prob2 = .005;
   
    TLongLongHashMap realCounts = new TLongLongHashMap();
    FrequentItemsNaiveNative frequentItems1 = new FrequentItemsNaiveNative(errorTolerance1);
    FrequentItemsNaiveNative frequentItems2 = new FrequentItemsNaiveNative(errorTolerance2);
    long sumOfValues = 0;
    for (int i=0; i<n; i++){
      long key1 = randomGeometricDist(prob1);
      frequentItems1.update(key1);
      realCounts.adjustOrPutValue(key1,1,1);
      sumOfValues+=1;
      
      long key2 = randomGeometricDist(prob2);
      frequentItems1.update(key2);
      realCounts.adjustOrPutValue(key2,1,1);
      sumOfValues+=1;
    }
    frequentItems1.merge(frequentItems2);
    FrequentItemsNaiveNative frequentItems = frequentItems1;
    for ( long key : realCounts.keys()){
      long realCount = realCounts.get(key);
      long lowerBound = frequentItems.getEstimateLowerBound(key);
      long upperBound = frequentItems.getEstimateUpperBound(key);
      Assert.assertTrue(upperBound >= realCount);
      Assert.assertTrue(realCount >= lowerBound);
      Assert.assertTrue(upperBound - lowerBound < sumOfValues*frequentItems.getErrorTolerance());
    }
  }
  
  @Test
  public void realCountsInBoundsAfterMergeWithValues() {
    int n = 10000;
    double errorTolerance1 = 0.002;
    double errorTolerance2 = 0.001;
    double prob1 = .001;
    double prob2 = .0005;
   
    TLongLongHashMap realCounts = new TLongLongHashMap();
    FrequentItemsNaiveNative frequentItems1 = new FrequentItemsNaiveNative(errorTolerance1);
    FrequentItemsNaiveNative frequentItems2 = new FrequentItemsNaiveNative(errorTolerance2);
    long sumOfValues = 0;
    for (int i=0; i<n; i++){
      long key1 = randomGeometricDist(prob1);
      long value1 = randomGeometricDist(prob1);
      sumOfValues += value1;
      frequentItems1.update(key1,value1);
      realCounts.adjustOrPutValue(key1,value1,value1);
      
      long key2 = randomGeometricDist(prob2);
      long value2 = randomGeometricDist(prob2);
      sumOfValues += value2;
      frequentItems1.update(key2,value2);
      realCounts.adjustOrPutValue(key2,value2,value2);
    }
    FrequentItemsNaiveNative frequentItems = (FrequentItemsNaiveNative) frequentItems1.merge(frequentItems2);
    for ( long key : realCounts.keys()){
      long realCount = realCounts.get(key);
      long lowerBound = frequentItems.getEstimateLowerBound(key);
      long upperBound = frequentItems.getEstimateUpperBound(key);
      
      Assert.assertTrue(upperBound >= realCount);
      Assert.assertTrue(realCount >= lowerBound);
      Assert.assertTrue(upperBound - lowerBound < sumOfValues*frequentItems.getErrorTolerance());
    }
  }
  
  /**
   * @param prob the probability of success for the geometric distribution. 
   * @return a random number generated from the geometric distribution.
   */
  static private long randomGeometricDist(double prob){
    assert(prob > 0.0 && prob < 1.0);
    return (long) (Math.log(Math.random()) / Math.log(1.0 - prob));
  }
  
  @Test
  public void testRandomGeometricDist() {
    long maxKey = 0L;
    double prob = .1;
    for (int i=0; i<100; i++){
      long key = randomGeometricDist(prob) ;
      if (key > maxKey) maxKey = key;
      // If you succeed with probability p the probability 
      // of failing 20/p times is smaller than 1/2^20.
      Assert.assertTrue(maxKey < 20.0/prob);
    }
  }

}
