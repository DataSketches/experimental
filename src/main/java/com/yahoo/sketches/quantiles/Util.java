package com.yahoo.sketches.quantiles;

import java.util.Arrays;

/*************************************************************************************/
/*************************************************************************************/
/*************************************************************************************/

/**
 * This class contains a highly specialized sort called blockyTandemMergeSort ().
 *
 * It also contains a couple of subroutines that are used while building histograms.
 *
 * Plus a few other minor things.
 */


class Util { 

  /****************************************************************/
  /****************************************************************/
  /****************************************************************/

  // Performs two merges in tandem. One of them provides the sort keys
  // while the other one passively undergoes the same data motion.

  private static void tandemMerge (double [] keySrc, long [] valSrc,
                                    int arrStart1, int arrLen1,
                                    int arrStart2, int arrLen2,
                                    double [] keyDst, long [] valDst,
                                    int arrStart3) 
  {

    int arrStop1 = arrStart1 + arrLen1;
    int arrStop2 = arrStart2 + arrLen2;

    int i1 = arrStart1;
    int i2 = arrStart2;

    int i3 = arrStart3;

    while (i1 < arrStop1 && i2 < arrStop2) {
      if (keySrc[i2] < keySrc[i1]) { 
        keyDst[i3] = keySrc[i2];
        valDst[i3] = valSrc[i2];
        i3++; i2++;
      }     
      else { 
        keyDst[i3] = keySrc[i1];
        valDst[i3] = valSrc[i1];
        i3++; i1++;
      } 
    }

    if (i1 < arrStop1) {
      System.arraycopy(keySrc, i1, keyDst, i3, arrStop1 - i1);
      System.arraycopy(valSrc, i1, valDst, i3, arrStop1 - i1);
    }
    else {
      assert i2 < arrStop2;
      System.arraycopy(keySrc, i2, keyDst, i3, arrStop2 - i2);
      System.arraycopy(valSrc, i2, valDst, i3, arrStop2 - i2);
    }

  }

  /********************************/

  // blockyTandemMergeSortRecursion() is called by blockyTandemMergeSort()
  // In addition to performing the algorithm's top down recursion,
  // it manages the buffer swapping that eliminates most copying.
  // It also maps the input's pre-sorted blocks into the subarrays 
  // that are processed by tandemMerge().

  private static void blockyTandemMergeSortRecursion (double [] keySrc, long [] valSrc,
                                                       double [] keyDst, long [] valDst,
                                                       int grpStart, int grpLen, /* indices of blocks */
                                                       int blkSize, int arrLim) {

    // Important note: grpStart and grpLen do NOT refer positions in the underlying array. 
    // Instead, they refer to the pre-sorted blocks, such as block 0, block 1, etc.

    assert (grpLen > 0);
    if (grpLen == 1) return;
    int grpLen1 = grpLen / 2;
    int grpLen2 = grpLen - grpLen1;
    assert (grpLen1 >= 1);
    assert (grpLen2 >= grpLen1);

    int grpStart1 = grpStart;
    int grpStart2 = grpStart + grpLen1;

    blockyTandemMergeSortRecursion (keyDst, valDst, /* swap roles of src and dst */
                           keySrc, valSrc,
                           grpStart1, grpLen1, blkSize, arrLim);

    blockyTandemMergeSortRecursion (keyDst, valDst, /* swap roles of src and dst */
                           keySrc, valSrc,
                           grpStart2, grpLen2, blkSize, arrLim);

    // here we convert indices of blocks into positions in the underlying array.
    int arrStart1 = grpStart1 * blkSize;
    int arrStart2 = grpStart2 * blkSize;
    int arrLen1   = grpLen1   * blkSize;
    int arrLen2   = grpLen2   * blkSize;

    // special code for the final block which might be shorter than blkSize.
    if (arrStart2 + arrLen2 > arrLim) { arrLen2 = arrLim - arrStart2; } 
 
    tandemMerge (keySrc, valSrc,
                 arrStart1, arrLen1, 
                 arrStart2, arrLen2,
                 keyDst, valDst,
                 arrStart1); // which will be arrStart3

  }

  /********************************/

  // blockyTandemMergeSort() is an implementation of top-down merge sort specialized
  // for the case where the input contains successive equal-length blocks
  // that have already been sorted, so that only the top part of the
  // merge tree remains to be executed. Also, two arrays are sorted in tandem,
  // as discussed above.

  // package private
  static void blockyTandemMergeSort (double [] keyArr, long [] valArr, int arrLen, int blkSize) {
    assert blkSize >= 1;
    if (arrLen <= blkSize) return;
    int numblks = arrLen / blkSize;
    if (numblks * blkSize < arrLen) numblks += 1;
    assert (numblks * blkSize >= arrLen);

    // duplicate the input is preparation for the "ping-pong" copy reduction strategy. 
    double [] keyTmp = Arrays.copyOf (keyArr, arrLen);
    long [] valTmp   = Arrays.copyOf (valArr, arrLen);

    blockyTandemMergeSortRecursion (keyTmp, valTmp,
                                    keyArr, valArr,
                                    0, numblks,
                                    blkSize, arrLen);

    /* verify sorted order */
    for (int i = 0; i < arrLen-1; i++) {
      assert keyArr[i] <= keyArr[i+1];
    }
  }

  /*****************************************************************/
  /*****************************************************************/
  /*****************************************************************/
  // Now a couple of helper functions for histogram construction.

  // Because of the nested loop, cost is O(numSamples * numSplitPoints) which is actually bilinear, not quadratic.
  // This subroutine does NOT require the samples to be sorted.

  // package private
  static void quadraticTimeIncrementHistogramCounters (double [] samples, int numSamples, long weight, 
                                                              double [] splitPoints, long [] counters) {
    // samples must be sorted
    // splitPoints must be unique and sorted
    // question: what happens if they aren't unique?
    assert (splitPoints.length + 1 == counters.length);
    for (int i = 0; i < numSamples; i++) {
      double sample = samples[i];
      int j = 0;

      for (j = 0; j < splitPoints.length; j++) {
        double splitpoint = splitPoints[j];
        if (sample < splitpoint) { 
          break;
        }
      }
      assert j < counters.length;
      // System.out.printf ("%.2f in bucket %d\n", sample, j);
      counters[j] += weight;
    }
  }

  /**********************************************************************/
  // this one does a linear time simultaneous walk of the samples and splitPoints
  // It DOES require the samples to be sorted

  // package private
  static void linearTimeIncrementHistogramCounters (double [] samples, int numSamples, 
                                                    long weight, double [] splitPoints, long [] counters) {
    // 1) samples must be sorted
    // 2) splitPoints must be unique and sorted (what happens if they aren't unique?)
    // 3) numSplitPoints + 1 == counters.length
    // Because this internal procedure is called multiple times, 
    // we will require the caller to ensure these 3 properties.

    int numSplitPoints = splitPoints.length;

    int i = 0;
    int j = 0;

    while (i < numSamples && j < numSplitPoints) {
      if (samples[i] < splitPoints[j]) {
        counters[j] += weight; // this sample goes into this bucket
        i++; // move on to next sample and see whether it also goes into this bucket
      }
      else {
        j++; // no more samples for this bucket. move on the next bucket.
      }
    }

    // now either i == numSamples (we are out of samples), or
    // j == numSplitPoints (out of buckets, but there are more samples remaining)
    // we only need to do something in the latter case.
 
    if (j == numSplitPoints) {
      counters[numSplitPoints] += (weight * (numSamples - i));
    }

  }

  /***************************************************************/
  /***************************************************************/
  /***************************************************************/

  // several miscellaneous utility functions

  // package private
  static double sumOfDoublesInArrayPrefix (double [] arr, int prefixLength) {
    double total = 0.0;
    for (int i = 0; i < prefixLength; i++) {
      total += arr[i];
    }
    return total;
  }

  /***************************************************************/
  /***************************************************************/
  /***************************************************************/
  // The remainder of this file is a brute force test of corner cases
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

  // package private
  static void testBlockyTandemMergeSort (int numTries, int maxArrLen) {
    for (int arrLen = 0; arrLen <= maxArrLen; arrLen++) {
      for (int blkSize = 1; blkSize <= arrLen + 100; blkSize++) {
        for (int tryno = 1; tryno <= numTries; tryno++) {  
          double [] arr = makeMergeTestInput (arrLen, blkSize);
          long [] brr = makeTheTandemArray (arr);
          assertMergeTestPrecondition (arr, brr, arrLen, blkSize);
          blockyTandemMergeSort (arr, brr, arrLen, blkSize);
          assertMergeTestPostcondition (arr, brr, arrLen);
        }
      }
    }
    System.out.printf ("Passed: testBlockyTandemMergeSort\n");
  } 

  /*****************************************************************/


  // we are retaining this stand-alone test for now because it is more exhaustive

  //  private static void exhaustiveMain (String[] args) {
  //    assert (args.length == 1);
  //    int  numTries = Integer.parseInt (args[0]);
  //    System.out.printf ("Testing blockyTandemMergeSort\n");
  //    for (int arrLen = 0; true ; arrLen++) { 
  //      for (int blkSize = 1; blkSize <= arrLen + 100; blkSize++) {
  //        System.out.printf ("Testing %d times with arrLen = %d and blkSize = %d\n", numTries, arrLen, blkSize);
  //        for (int tryno = 1; tryno <= numTries; tryno++) {  
  //          double [] arr = makeMergeTestInput (arrLen, blkSize);
  //          long [] brr = makeTheTandemArray (arr);
  //          assertMergeTestPrecondition (arr, brr, arrLen, blkSize);
  //          blockyTandemMergeSort (arr, brr, arrLen, blkSize);
  //          assertMergeTestPostcondition (arr, brr, arrLen);
  //        }
  //      }
  //    }
  //  } 
  //
  //  public static void main (String[] args) {  
  //    exhaustiveMain (args);
  //  }

}