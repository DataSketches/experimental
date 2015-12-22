/*
 * Copyright 2015, Yahoo! Inc.
 * Licensed under the terms of the Apache License 2.0. See LICENSE file at the project root for terms.
 */
package com.yahoo.sketches.quantiles;

import static java.lang.System.*;

import java.util.Arrays;
import java.util.Random;

/**
 * This class contains a highly specialized sort called blockyTandemMergeSort ().
 *
 * It also contains a couple of subroutines that are used while building histograms and a 
 * few other minor things.
 */
final class Util { 

  /**
   * The java line separator character as a String.
   */
  public static final String LS = System.getProperty("line.separator");

  static Random rand = new Random();

  /**
   * Computes epsilon from K. The following table are examples.
   * <code><pre>
   *           eps      eps from inverted
   *     K   empirical  adjusted formula
   *  -------------------------------------
   *    16   0.121094   0.121454102233560
   *    32   0.063477   0.063586601346532
   *    64   0.033081   0.033169048393679
   *   128   0.017120   0.017248096847308
   *   256   0.008804   0.008944835012965
   *   512   0.004509   0.004627803568920
   *  1024   0.002303   0.002389303789572
   *
   *  these could be used in a unit test
   *  2   0.821714930853465
   *  16   0.12145410223356
   *  1024   0.00238930378957284
   *  1073741824   3.42875166500824e-09
   * </pre></code>
   */
  public static class EpsilonFromK {
    // the following delta was used while crunching down the empirical results
    private static final double deltaForEps = 0.01;  

    // this is a heuristic fudge factor that causes the inverted formula to better match the empirical
    private static final double adjustKForEps = 4.0 / 3.0;  // fudge factor

    // ridiculously fine tolerance given the fudge factor; 1e-3 would probably suffice
    private static final double bracketedBinarySearchForEpsTol = 1e-15; 

    private static double kOfEpsFormula(double eps) {
      return (1.0 / eps) * (Math.sqrt(Math.log(1.0 / (eps * deltaForEps))));
    }

    private static boolean epsForKPredicate(double eps, double kf) {
      return kOfEpsFormula(eps) >= kf;
    }

    private static double bracketedBinarySearchForEps(double kf, double lo, double hi) {
      assert lo < hi;
      assert epsForKPredicate(lo, kf);
      assert !epsForKPredicate(hi, kf);
      if ((hi - lo) / lo < bracketedBinarySearchForEpsTol) {
        return lo;
      }
      double mid = (lo + hi) / 2.0;
      assert mid > lo;
      assert mid < hi;
      if (epsForKPredicate(mid, kf)) {
        return bracketedBinarySearchForEps(kf, mid, hi);
      }
      else {
        return bracketedBinarySearchForEps(kf, lo, mid);
      }
    }

    /**
     * Finds the theoretical epsilon given K and a fudge factor.
     * @param k The given value of k
     * @param ff The given fudge factor. No fudge factor = 1.0. 
     * @return the resulting epsilon
     */
    public static double getTheoreticalEpsilon(int k, double ff) {
      if (k < 2) throw new IllegalArgumentException("K must be greater than one.");
      // don't need to check in the other direction because an int is very small
      double kf = k*ff;
      assert kf >= 2.15; // ensures that the bracketing succeeds
      assert kf < 1e12;  // ditto, but could actually be bigger
      double lo = 1e-16;
      double hi = 1.0 - 1e-16;
      assert epsForKPredicate(lo, kf);
      assert !epsForKPredicate(hi, kf);
      return bracketedBinarySearchForEps(kf, lo, hi);
    }

    //also used by test
    /**
     * From extensive empirical testing we recommend most users use this method for deriving 
     * epsilon. This uses a fudge factor of 4/3 times the theoretical calculation of epsilon.
     * @param k the given k that must be greater than one.
     * @return the resulting epsilon
     */
    public static double getAdjustedEpsilon(int k) {
      return getTheoreticalEpsilon(k, adjustKForEps);
    }
  } //End of EpsilonFromK

  // Performs two merges in tandem. One of them provides the sort keys
  // while the other one passively undergoes the same data motion.
  private static void tandemMerge (double [] keySrc, long [] valSrc,
                                    int arrStart1, int arrLen1,
                                    int arrStart2, int arrLen2,
                                    double [] keyDst, long [] valDst,
                                    int arrStart3) {
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
      arraycopy(keySrc, i1, keyDst, i3, arrStop1 - i1);
      arraycopy(valSrc, i1, valDst, i3, arrStop1 - i1);
    }
    else {
      assert i2 < arrStop2;
      arraycopy(keySrc, i2, keyDst, i3, arrStop2 - i2);
      arraycopy(valSrc, i2, valDst, i3, arrStop2 - i2);
    }
  }

  // blockyTandemMergeSortRecursion() is called by blockyTandemMergeSort()
  // In addition to performing the algorithm's top down recursion,
  // it manages the buffer swapping that eliminates most copying.
  // It also maps the input's pre-sorted blocks into the subarrays 
  // that are processed by tandemMerge().
  private static void blockyTandemMergeSortRecursion (double [] keySrc, long [] valSrc,
      double [] keyDst, long [] valDst, int grpStart, int grpLen, /* indices of blocks */
      int blkSize, int arrLim) {
    // Important note: grpStart and grpLen do NOT refer to positions in the underlying array.
    // Instead, they refer to the pre-sorted blocks, such as block 0, block 1, etc.

    assert (grpLen > 0);
    if (grpLen == 1) return;
    int grpLen1 = grpLen / 2;
    int grpLen2 = grpLen - grpLen1;
    assert (grpLen1 >= 1);
    assert (grpLen2 >= grpLen1);

    int grpStart1 = grpStart;
    int grpStart2 = grpStart + grpLen1;

    //swap roles of src and dst
    blockyTandemMergeSortRecursion (keyDst, valDst,
                           keySrc, valSrc,
                           grpStart1, grpLen1, blkSize, arrLim);

    //swap roles of src and dst
    blockyTandemMergeSortRecursion (keyDst, valDst,
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

  // blockyTandemMergeSort() is an implementation of top-down merge sort specialized
  // for the case where the input contains successive equal-length blocks
  // that have already been sorted, so that only the top part of the
  // merge tree remains to be executed. Also, two arrays are sorted in tandem,
  // as discussed above.
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

  // Now a couple of helper functions for histogram construction.

  // Because of the nested loop, cost is O(numSamples * numSplitPoints) which is actually bilinear, not quadratic.
  // This method does NOT require the samples to be sorted.
  /**
   * Because of the nested loop, cost is O(numSamples * numSplitPoints) which is actually bilinear, 
   * not quadratic. This method does NOT require the samples to be sorted.
   * @param samples array of samples
   * @param offset into samples array
   * @param numSamples number of samples in samples array
   * @param weight of the samples
   * @param splitPoints must be unique and sorted. Number of splitPoints +1 = counters.length.
   * @param counters array of counters
   */
  static void quadraticTimeIncrementHistogramCounters (double[] samples, int offset, 
      int numSamples, long weight, double[] splitPoints, long[] counters) {
    // samples must be sorted
    // splitPoints must be unique and sorted
    // question: what happens if they aren't unique?
    assert (splitPoints.length + 1 == counters.length);
    for (int i = 0; i < numSamples; i++) {
      double sample = samples[i+offset];
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

  /**
   * This one does a linear time simultaneous walk of the samples and splitPoints. Because this
   * internal procedure is called multiple times, we require the caller to ensure these 3 properties:
   * <ol><li>samples array must be sorted.</li>
   * <li>splitPoints must be unique and sorted</li>
   * <li>number of SplitPoints + 1 == counters.length</li>
   * </ol>
   * @param samples sorted array of samples
   * @param offset into samples array
   * @param numSamples number of samples in samples array
   * @param weight of the samples
   * @param splitPoints must be unique and sorted. Number of splitPoints +1 = counters.length.
   * @param counters array of counters
   */
  static void linearTimeIncrementHistogramCounters (double[] samples, int offset, int numSamples, 
      long weight, double[] splitPoints, long[] counters) {

    int numSplitPoints = splitPoints.length;

    int i = 0;
    int j = 0;

    while (i < numSamples && j < numSplitPoints) {
      if (samples[i+offset] < splitPoints[j]) {
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

  // several miscellaneous utility functions

  static double lg(double x) {
    return ( Math.log(x)) / (Math.log(2.0) );
  }

  /**
   * Zero based position of the highest one-bit of the given long
   * @param num the given long
   * @return Zero based position of the highest one-bit of the given long
   */
  static int hiBitPos(long num) {
    return 63 - Long.numberOfLeadingZeros(num);
  }

  static int positionOfLowestZeroBitStartingAt(long numIn, int startingPos) {
    long num = numIn >>> startingPos;
    int pos = 0;
    while ((num & 1L) != 0) {
      num = num >>> 1;
      pos++;
    }
    return (pos + startingPos);
  }

  static double sumOfDoublesInArrayPrefix(double[] arr, int prefixLength) {
    double total = 0.0;
    for (int i = 0; i < prefixLength; i++) {
      total += arr[i];
    }
    return total;
  }

  static double sumOfDoublesInSubArray(double[] arr, int subArrayStart, int subArrayLength) {
    double total = 0.0;
    int subArrayStop = subArrayStart + subArrayLength;
    for (int i = subArrayStart; i < subArrayStop; i++) {
      total += arr[i];
    }
    return total;
  }

//  public static void main(String[] args) {
//    long v = 1;
//    for (int i=0; i<64; i++) {
//      long w = v << i;
//      long w2 = w -1;
//      System.out.println(i+"\t"+Long.toBinaryString(w2)+"\t"+hiBitPos(w2)+"\t"+w2);
//    }
//  }
  
}