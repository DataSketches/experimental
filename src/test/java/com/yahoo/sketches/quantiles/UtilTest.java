package com.yahoo.sketches.quantiles;

import java.util.Arrays;

//import static org.testng.Assert.*;

import org.testng.annotations.Test;

public class UtilTest {

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

 /*****************************************************************/

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

 /*****************************************************************/

 private static long [] makeTheTandemArray (double [] arr) {
   long [] brr = new long [arr.length + 1];  /* make it one longer, just like in the sketches */
   for (int i = 0; i < arr.length; i++) {
     brr[i] = (long) (1e12 * (1.0 - arr[i])); /* it's a better test with the order reversed like this */
   }
   brr[arr.length] = 0;
   return brr;
 }

 /*****************************************************************/
 /**
  * 
  * @param numTries number of tries
  * @param maxArrLen maximum length of array size
  */
 @Test
 public void testBlockyTandemMergeSort (int numTries, int maxArrLen) {
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
   System.out.printf ("Passed: testBlockyTandemMergeSort\n");
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
