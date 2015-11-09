package com.yahoo.sketches.frequencies;

import org.testng.Assert;
import org.testng.annotations.Test;


public class FrequentItemsCuckooHashTest {

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void construct() {
    int size = 100;
    FrequentItemsCuckooHash frequentItems = new FrequentItemsCuckooHash(size);
    Assert.assertNotNull(frequentItems);
    // Should throw exception
    frequentItems = new FrequentItemsCuckooHash(-134);
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
   
  @Test
  public void realCountsInBounds() {
    int n = 4213;
    int maxSize = 50;
    long key;
    double prob = .04; 
    FrequentItemsCuckooHash frequentItems = new FrequentItemsCuckooHash(maxSize);
    PositiveCountersMap realCounts = new PositiveCountersMap();
    for (int i=0; i<n; i++){   
      key = randomGeometricDist(prob);
      frequentItems.update(key);
      realCounts.increment(key);
      long realCount = realCounts.get(key);
      long lowerBound = frequentItems.get(key);
      long upperBound = frequentItems.get(key) + frequentItems.getMaxError();
      Assert.assertTrue(upperBound >=  realCount && realCount >= lowerBound);   
    }
  }
  
  @Test
  public void errorWithinLimits() {
    int n = 100;
    int maxSize = 20;
    long key;
    double prob = .1; 
    FrequentItemsCuckooHash frequentItems = new FrequentItemsCuckooHash(maxSize);
    for (int i=0; i<n; i++){
      key = randomGeometricDist(prob);
      frequentItems.update(key);
      long lowerBound = frequentItems.get(key);
      long upperBound = frequentItems.get(key) + frequentItems.getMaxError();
      Assert.assertTrue(upperBound - lowerBound <= i/maxSize);  
    }
  } 
    
  /*
  @Test
  public void realCountsInBoundsAfterUnion() {
    int n = 1000;
    int maxSize1 = 100;
    int maxSize2 = 400;
    double prob1 = .01;
    double prob2 = .005;
   
    PositiveCountersMap realCounts = new PositiveCountersMap();
    FrequentItemsCuckooHash frequentItems1 = new FrequentItemsCuckooHash(maxSize1);
    FrequentItemsCuckooHash frequentItems2 = new FrequentItemsCuckooHash(maxSize2);
    for (int i=0; i<n; i++){
      long key1 = randomGeometricDist(prob1);
      long key2 = randomGeometricDist(prob2);
      
      frequentItems1.update(key1);
      frequentItems2.update(key2);
      
      // Updating the real counters
      realCounts.increment(key1);
      realCounts.increment(key2);
    }
    FrequentItemsCuckooHash frequentItems = frequentItems1.union(frequentItems2);

    for ( long key : realCounts.keys()){
      
      long realCount = realCounts.get(key);
      long lowerBound = frequentItems.get(key);
      long upperBound = frequentItems.get(key) + frequentItems.getMaxError();
      Assert.assertTrue(upperBound >=  realCount && realCount >= lowerBound);
    }
  }*/
  
  @Test
  public void stressTestUpdateTime() {
    int n = 1000000;
    int maxSize = 1000;  
    FrequentItemsCuckooHash frequentItems = new FrequentItemsCuckooHash(maxSize);
    double prob = 1.0/n;
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
    //System.out.println("Amortized time per update: " + timePerUpdate);
    Assert.assertTrue(timePerUpdate < 10E-3);
  }

  @Test
  public void stressAndErrorTest() {
    int n = 100000000;
    int maxSize = 100000;  
    FrequentItemsCuckooHash frequentItems = new FrequentItemsCuckooHash(maxSize);
    long[] keys = new long[n];
    for (int i=0; i<n; i++){
      keys[i] = (i < n/2) ? i%1000 : i;
    }
    final long startTime = System.currentTimeMillis();
    for (int i=0; i<n; i++){
      frequentItems.update(keys[i]);
    }
    final long endTime = System.currentTimeMillis();
    double timePerUpdate = (double)(endTime-startTime)/(double)n;
    System.out.println("Expected " + (int) (1000/timePerUpdate) + " updates per second.");
    System.out.println("Maximal error is " + frequentItems.getMaxError());
    Assert.assertTrue(timePerUpdate < 10E-3);
  }
  
}
