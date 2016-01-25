/*
 * Copyright 2015, Yahoo! Inc.
 * Licensed under the terms of the Apache License 2.0. See LICENSE file at the project root for terms.
 */
package com.yahoo.sketches.quantiles;

import static com.yahoo.sketches.quantiles.Util.hiBitPos;
import static com.yahoo.sketches.quantiles.Util.lg;
import static com.yahoo.sketches.quantiles.Util.sumOfDoublesInSubArray;
import static org.testng.Assert.assertEquals;

import java.util.Arrays;

import org.testng.annotations.Test;

import com.yahoo.sketches.quantiles.Util.EpsilonFromK;

public class UtilTest {
  
  @Test
  public void checkGetAdjustedEpsilon() {
    double eps = EpsilonFromK.getAdjustedEpsilon(227);
    assertEquals(eps, .01, .005);
  }
  
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void checkGetAdjustedEpsilonException() {
    EpsilonFromK.getAdjustedEpsilon(1);
  }
  
  @Test
  public void checkLg() {
    int lgbase2 = (int)lg(4096);
    assertEquals(lgbase2, 12);
  }
  
  @Test
  public void checkHiBitPos() {
    int bitPos = hiBitPos(4096);
    assertEquals(bitPos, 12);
  }
  
  @Test
  public void checkSumOfDoublesInArrayPrefix() {
    double[] arr = {1, 2, 3, 4, 5, 6, 7, 8};
    double sum = MergeableQuantileSketch.sumOfDoublesInArrayPrefix(arr, 4);
    assertEquals(sum, 10.0, 0.0);
  }
  
  @Test
  public void checkSumOfDoublesInSubArray() {
    double[] arr = {1, 2, 3, 4, 5, 6, 7, 8};
    double sum = sumOfDoublesInSubArray(arr, 0, 4);
    assertEquals(sum, 10.0, 0.0);
  }
  
  @Test
  public void testPOLZBSA () {
    int [] answers = {9, 8, 7, 7, 7, 4, 4, 4, 1, 1};
    for (int i = 0, j = 9; i < 10; i++, j--) {
      int result = Util.positionOfLowestZeroBitStartingAt(109L, i);
      //      System.out.printf ("%d %d\n", i, result);
      assert (answers[j] == result);
    }
  }

  // a couple of basic unit tests for the histogram construction helper functions.
  @Test
  public void testQuadraticTimeIncrementHistogramCounters () {
    double [] samples = {0.1, 0.2, 0.3, 0.4, 0.5};
    {
      double [] splitPoints = {0.25, 0.4};
      long counters [] = {0, 0, 0};
      long answers  [] = {200, 100, 200};
      Util.bilinearTimeIncrementHistogramCounters (samples, 0, 5, 100, splitPoints, counters);
      for (int j = 0; j < counters.length; j++) {
        assert counters[j] == answers[j];
        // System.out.printf ("counter[%d] = %d\n", j, counters[j]);
      }
      // System.out.printf ("\n");
    }
 
    {
      double [] splitPoints = {0.01, 0.02};
      long counters [] = {0, 0, 0};
      long answers  [] = {0, 0, 500};
      Util.bilinearTimeIncrementHistogramCounters (samples, 0, 5, 100, splitPoints, counters);
      for (int j = 0; j < counters.length; j++) {
        assert counters[j] == answers[j];
        // System.out.printf ("counter[%d] = %d\n", j, counters[j]);
      }
      // System.out.printf ("\n");
    }
 
    {
      double [] splitPoints = {0.8, 0.9};
      long counters [] = {0, 0, 0};
      long answers  [] = {500, 0, 0};
      Util.bilinearTimeIncrementHistogramCounters (samples, 0, 5, 100, splitPoints, counters);
      for (int j = 0; j < counters.length; j++) {
        assert counters[j] == answers[j];
        // System.out.printf ("counter[%d] = %d\n", j, counters[j]);
      }
      // System.out.printf ("\n");
    } 
  }

  @Test
  public void testLinearTimeIncrementHistogramCounters () {
    double [] samples = {0.1, 0.2, 0.3, 0.4, 0.5};

    {
      double [] splitPoints = {0.25, 0.4};
      long counters [] = {0, 0, 0};
      long answers  [] = {200, 100, 200};
      Util.linearTimeIncrementHistogramCounters (samples, 0, 5, 100, splitPoints, counters);
      for (int j = 0; j < counters.length; j++) {
        assert counters[j] == answers[j];
        // System.out.printf ("counter[%d] = %d\n", j, counters[j]);
      }
      // System.out.printf ("\n");
    }
 
    {
      double [] splitPoints = {0.01, 0.02};
      long counters [] = {0, 0, 0};
      long answers  [] = {0, 0, 500};
      Util.linearTimeIncrementHistogramCounters (samples, 0, 5, 100, splitPoints, counters);
      for (int j = 0; j < counters.length; j++) {
        assert counters[j] == answers[j];
        // System.out.printf ("counter[%d] = %d\n", j, counters[j]);
      }
      // System.out.printf ("\n");
    }
 
    {
      double [] splitPoints = {0.8, 0.9};
      long counters [] = {0, 0, 0};
      long answers  [] = {500, 0, 0};
      Util.linearTimeIncrementHistogramCounters (samples, 0, 5, 100, splitPoints, counters);
      for (int j = 0; j < counters.length; j++) {
        assert counters[j] == answers[j];
        // System.out.printf ("counter[%d] = %d\n", j, counters[j]);
      }
      // System.out.printf ("\n");
    } 
  }

  /* need to write tests where there are zero or one splitpoints, and zero samples */


//The remainder of this file is a brute force test of corner cases
 // for blockyTandemMergeSort.

 private static void assertMergeTestPrecondition (double [] arr, long [] brr, int arrLen, int blkSize) {
   int violationsCount = 0;
   for (int i = 0; i < arrLen-1; i++) {
     if (((i+1) % blkSize) == 0) continue;
     if (arr[i] > arr[i+1]) { violationsCount++; }
   }

   for (int i = 0; i < arrLen; i++) {
     if (brr[i] != (long) (1e12 * (1.0 - arr[i]))) violationsCount++;
   }
   if (brr[arrLen] != 0) { violationsCount++; }

   assert violationsCount == 0;
 }

 private static void  assertMergeTestPostcondition (double [] arr, long [] brr, int arrLen) {
   int violationsCount = 0;
   for (int i = 0; i < arrLen-1; i++) {
     if (arr[i] > arr[i+1]) { violationsCount++; }
   }

   for (int i = 0; i < arrLen; i++) {
     if (brr[i] != (long) (1e12 * (1.0 - arr[i]))) violationsCount++;
   }
   if (brr[arrLen] != 0) { violationsCount++; }

   assert violationsCount == 0;
 }


 private static double [] makeMergeTestInput (int arrLen, int blkSize) {
   double [] arr = new double [arrLen]; 

   double pick = Math.random ();

   for (int i = 0; i < arrLen; i++) {     
     if (pick < 0.01) { // every value the same
       arr[i] = 0.3; 
     }        
     else if (pick < 0.02) { // ascending values
       int j = i+1;
       int denom = arrLen+1;
       arr[i] = ((double) j) / ((double) denom);
     } 
     else if (pick < 0.03) { // descending values
       int j = i+1;
       int denom = arrLen+1;
       arr[i] = 1.0 - (((double) j) / ((double) denom));
     } 
     else { // random values
       arr[i] = Math.random (); 
     } 
   }

   for (int start = 0; start < arrLen; start += blkSize) {
     Arrays.sort (arr, start, Math.min (arrLen, start + blkSize));
   }

   return arr;
 }

 private static long [] makeTheTandemArray (double [] arr) {
   long [] brr = new long [arr.length + 1];  /* make it one longer, just like in the sketches */
   for (int i = 0; i < arr.length; i++) {
     brr[i] = (long) (1e12 * (1.0 - arr[i])); /* it's a better test with the order reversed like this */
   }
   brr[arr.length] = 0;
   return brr;
 }

 @Test
 public void checkBlockyTandemMergeSort() {
   testBlockyTandemMergeSort(10, 50);
 }
 
 /**
  * 
  * @param numTries number of tries
  * @param maxArrLen maximum length of array size
  */
 private static void testBlockyTandemMergeSort (int numTries, int maxArrLen) {
   for (int arrLen = 0; arrLen <= maxArrLen; arrLen++) {
     for (int blkSize = 1; blkSize <= arrLen + 100; blkSize++) {
       for (int tryno = 1; tryno <= numTries; tryno++) {  
         double [] arr = makeMergeTestInput (arrLen, blkSize);
         long [] brr = makeTheTandemArray (arr);
         assertMergeTestPrecondition (arr, brr, arrLen, blkSize);
         Util.blockyTandemMergeSort (arr, brr, arrLen, blkSize);
         assertMergeTestPostcondition (arr, brr, arrLen);
       }
     }
   }
   //System.out.printf ("Passed: testBlockyTandemMergeSort\n");
 } 

  
// we are retaining this stand-alone test for now because it is more exhaustive
  
  @SuppressWarnings("unused")
  private static void exhaustiveMain (String[] args) {
    assert (args.length == 1);
    int  numTries = Integer.parseInt (args[0]);
    System.out.printf ("Testing blockyTandemMergeSort\n");
    for (int arrLen = 0; true ; arrLen++) { 
      for (int blkSize = 1; blkSize <= arrLen + 100; blkSize++) {
        System.out.printf ("Testing %d times with arrLen = %d and blkSize = %d\n", numTries, arrLen, blkSize);
        for (int tryno = 1; tryno <= numTries; tryno++) {  
          double [] arr = makeMergeTestInput (arrLen, blkSize);
          long [] brr = makeTheTandemArray (arr);
          assertMergeTestPrecondition (arr, brr, arrLen, blkSize);
          Util.blockyTandemMergeSort (arr, brr, arrLen, blkSize);
          assertMergeTestPostcondition (arr, brr, arrLen);
        }
      }
    }
  }
 
}
