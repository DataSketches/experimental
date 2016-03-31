/*
 * Copyright 2015, Yahoo! Inc. Licensed under the terms of the Apache License 2.0. See LICENSE file
 * at the project root for terms.
 */

package com.yahoo.sketches.frequencies;

import org.testng.annotations.Test;
import org.testng.Assert;
import java.lang.Math;
import java.util.Collection;

/**
 * Tests SpaceSaving class
 * 
 * @author Justin8712
 * 
 */
public class SpaceSavingTest {

  //@Test(expectedExceptions = IllegalArgumentException.class)
  public void construct() {
    int size = 100;
    double error_tolerance = 1.0 / size;
    SpaceSaving spacesaving = new SpaceSaving(error_tolerance);
    Assert.assertNotNull(spacesaving);
    // Should throw exception
    spacesaving = new SpaceSaving(-134);
  }

  //@Test
  public void updateOneTime() {
    int size = 100;
    double error_tolerance = 1.0 / size;
    SpaceSaving spacesaving = new SpaceSaving(error_tolerance);
    spacesaving.update(13L);
    Assert.assertEquals(spacesaving.nnz(), 1);
  }

  //@Test(expectedExceptions = IllegalArgumentException.class)
  public void updateOneTimeException1() {
    int size = 100;
    double error_tolerance = 1.0 / size;
    SpaceSaving spacesaving = new SpaceSaving(error_tolerance);
    spacesaving.update(13L, 0);
  }

  //@Test(expectedExceptions = IllegalArgumentException.class)
  public void updateOneTimeException2() {
    int size = 100;
    double error_tolerance = 1.0 / size;
    SpaceSaving spacesaving = new SpaceSaving(error_tolerance);
    spacesaving.update(13L, -2);
  }


  //@Test
  public void sizeDoesNotGrow() {
    int size = 100;
    double error_tolerance = 1.0 / size;
    SpaceSaving spacesaving = new SpaceSaving(error_tolerance);
    for (long key = 0L; key < 10000L; key++) {
      spacesaving.update(key, 1);
      Assert.assertTrue(spacesaving.nnz() <= size + 1);
    }
  }

  //@Test
  public void estimatesAreCorectBeofreDeletePhase() {
    int size = 100;
    double error_tolerance = 1.0 / size;
    SpaceSaving spacesaving = new SpaceSaving(error_tolerance);
    for (long key = 0L; key < 99L; key++) {
      spacesaving.update(key);
      Assert.assertTrue(spacesaving.getEstimate(key) == 1);
      Assert.assertTrue(spacesaving.getMaxError() == 0);
    }
  }

  /**
   * @param prob the probability of success for the geometric distribution.
   * @return a random number generated from the geometric distribution.
   */
  static private long randomGeometricDist(double prob) {
    assert (prob > 0.0 && prob < 1.0);
    return (long) (Math.log(Math.random()) / Math.log(1.0 - prob));
  }

  //@Test
  public void testRandomGeometricDist() {
    long maxKey = 0L;
    double prob = .1;
    for (int i = 0; i < 100; i++) {
      long key = randomGeometricDist(prob);
      if (key > maxKey)
        maxKey = key;
      // If you succeed with probability p the probability
      // of failing 20/p times is smaller than 1/2^20.
      Assert.assertTrue(maxKey < 20.0 / prob);
    }
  }

  //@Test
  public void realCountsInBounds() {
    int n = 4213;
    int maxSize = 50;
    long key;
    double prob = .04;

    double error_tolerance = 1.0 / maxSize;
    SpaceSaving spacesaving = new SpaceSaving(error_tolerance);

    PositiveCountersMap realCounts = new PositiveCountersMap();
    for (int i = 0; i < n; i++) {
      key = randomGeometricDist(prob);
      spacesaving.update(key);
      realCounts.increment(key);
      long realCount = realCounts.get(key);
      long upperBound = spacesaving.getEstimate(key);
      long lowerBound = spacesaving.getEstimate(key) - spacesaving.getMaxError();
      Assert.assertTrue(upperBound >= realCount && realCount >= lowerBound);
    }
  }


  //@Test
  public void testFrequent() {
    int n = 4213;
    int maxSize = 50;
    long key;
    double prob = .04;
    double error_tolerance = 1.0 / maxSize;
    SpaceSaving spacesaving = new SpaceSaving(error_tolerance);

    PositiveCountersMap realCounts = new PositiveCountersMap();
    for (int i = 0; i < n; i++) {
      key = randomGeometricDist(prob);
      spacesaving.update(key);
      realCounts.increment(key);
    }
    long[] freq = spacesaving.getFrequentKeys();
    for (int i = 0; i < freq.length; i++) {
      Assert.assertTrue(spacesaving.getEstimate(freq[i]) >= n / (maxSize + 1));
    }
    Collection<Long> keysCollection = realCounts.keys();

    int found;
    for (long the_key : keysCollection) {
      if (realCounts.get(the_key) > n / (maxSize + 1)) {
        found = 0;
        for (int i = 0; i < freq.length; i++) {
          if (freq[i] == the_key) {
            found = 1;
          }
        }
        Assert.assertTrue(found == 1);
      }
    }
  }

  //@Test
  public void errorWithinLimits() {
    int n = 100;
    int maxSize = 20;
    long key;
    double prob = .1;

    double error_tolerance = 1.0 / maxSize;
    SpaceSaving spacesaving = new SpaceSaving(error_tolerance);

    for (int i = 0; i < n; i++) {
      key = randomGeometricDist(prob);
      spacesaving.update(key);
      long upperBound = spacesaving.getEstimate(key);
      long lowerBound = spacesaving.getEstimate(key) - spacesaving.getMaxError();
      Assert.assertTrue(upperBound - lowerBound <= i / maxSize);

      key = randomGeometricDist(prob);
      upperBound = spacesaving.getEstimate(key);
      lowerBound = spacesaving.getEstimate(key) - spacesaving.getMaxError();
      Assert.assertTrue(upperBound - lowerBound <= i / maxSize);

    }
  }

  //@Test
  public void realCountsInBoundsAfterUnion() {
    int n = 1000;
    int maxSize1 = 100;
    int maxSize2 = 400;
    double prob1 = .01;
    double prob2 = .005;

    double error_tolerance1 = 1.0 / maxSize1;
    SpaceSaving spacesaving1 = new SpaceSaving(error_tolerance1);

    double error_tolerance2 = 1.0 / maxSize2;
    SpaceSaving spacesaving2 = new SpaceSaving(error_tolerance2);

    PositiveCountersMap realCounts = new PositiveCountersMap();

    for (int i = 0; i < n; i++) {
      long key1 = randomGeometricDist(prob1);
      long key2 = randomGeometricDist(prob2);

      spacesaving1.update(key1);
      spacesaving2.update(key2);

      // Updating the real counters
      realCounts.increment(key1);
      realCounts.increment(key2);
    }
    SpaceSaving spacesaving = spacesaving1.merge(spacesaving2);

    for (long key : realCounts.keys()) {
      long realCount = realCounts.get(key);
      long upperBound = spacesaving.getEstimateUpperBound(key);
      long lowerBound = spacesaving.getEstimateLowerBound(key);
      Assert.assertTrue(upperBound >= realCount && realCount >= lowerBound);
    }
  }

  ////@Test
  public void stressTestUpdateTime() {
    int n = 2000000;
    int size = 100000;
    double eps = 1.0 / size;
    int trials = 100;
    double total_updates_per_s = 0;
    for (int trial = 0; trial < trials; trial++) {
      SpaceSaving spacesaving = new SpaceSaving(eps);
      int key = 0;
      double startTime = System.nanoTime();
      for (int i = 0; i < n; i++) {
        // long key = randomGeometricDist(prob);
        spacesaving.update(key++);
      }
      double endTime = System.nanoTime();
      double timePerUpdate = (endTime - startTime) / (1000000.0 * n);
      double updatesPerSecond = 1000.0 / timePerUpdate;
      total_updates_per_s += updatesPerSecond;
    }
    System.out.format("Amortized updates per second for SpaceSaving: %f\n",
        (total_updates_per_s / trials));
    Assert.assertTrue(total_updates_per_s / trials > 1000000);
  }

  //@Test
  public void printlnTest() {
    println("PRINTING: " + this.getClass().getName());
  }

  /**
   * @param s value to print
   */
  static void println(String s) {
    // System.out.println(s); //disable here
  }

}
