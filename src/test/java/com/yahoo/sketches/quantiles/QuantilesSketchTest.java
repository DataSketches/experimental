/*
 * Copyright 2015, Yahoo! Inc.
 * Licensed under the terms of the Apache License 2.0. See LICENSE file at the project root for terms.
 */
package com.yahoo.sketches.quantiles;

import org.testng.annotations.Test;
import static org.testng.Assert.*;
import static com.yahoo.sketches.quantiles.QuantilesSketch.*;
import static com.yahoo.sketches.quantiles.Util.*;
import static java.lang.Math.*;

public class QuantilesSketchTest { 

  @Test
    public void checkGetAdjustedEpsilon() {
    // note: there is a big fudge factor in these numbers, so they don't need to be computed exactly
    double absTol = 1e-14; // we just want to catch gross bugs
    int[] kArr = {2,16,1024,1 << 30};
    double[] epsArr = { // these were computed by an earlier ocaml version of the function
      0.821714930853465,
      0.12145410223356,
      0.00238930378957284,
      3.42875166500824e-09 };
    for (int i = 0; i < 4; i++) {
      assertEquals(epsArr[i], 
                   Util.EpsilonFromK.getAdjustedEpsilon(kArr[i]),
                   absTol,
                   "adjustedFindEpsForK() doesn't match precomputed value");
    }
    for (int i = 0; i < 3; i++) {
      QuantilesSketch mq = new QuantilesSketch(kArr[i]);
      assertEquals(epsArr[i], 
                   mq.getNormalizedRankError(),
                   absTol,
                   "getNormalizedCountError() doesn't match precomputed value");
    }
  }

  // Please note that this is a randomized test that CAN fail.
  // The probability of failure could be reduced by increasing k.
  // Actually, setting the seed has now made it deterministic.
  @Test
  public void checkEndToEnd() {
    Util.rand.setSeed(917351); // arbitrary seed that makes this test deterministic
    QuantilesSketch qs  = new QuantilesSketch(256);
    QuantilesSketch qs2 = new QuantilesSketch(256);

    for (int item = 1000000; item >= 1; item--) {
      if (item % 4 == 0) {
        qs.update(item);
      }
      else {
        qs2.update(item);
      }
    }

    QuantilesSketch.mergeInto(qs2, qs);

    int numPhiValues = 99;
    double[] phiArr = new double[numPhiValues];
    for (int q = 1; q <= 99; q++) {
      phiArr[q-1] = q / 100.0;
    }
    double[] splitPoints = qs.getQuantiles(phiArr);

    /*
    for (int i = 0; i < 99; i++) {
      System.out.printf("%d\t%.6f\t%.6f\n", i, phiArr[i], splitPoints[i]);
    }
    */

    for (int q = 1; q <= 99; q++) {
      double nominal = 1e6 * q / 100.0;
      double reported = splitPoints[q-1];
      assert reported >= nominal - 10000.0;
      assert reported <= nominal + 10000.0;
    }

    double[] pdfResult = qs.getPDF(splitPoints);
    double subtotal = 0.0;
    for (int q = 1; q <= 99; q++) {
      double phi = q / 100.0;
      subtotal += pdfResult[q-1];
      assert subtotal >= phi - 0.01;
      assert subtotal <= phi + 0.01;
    }
    // should probably assert that the pdf sums to 1.0

    double[] cdfResult = qs.getCDF(splitPoints);
    for (int q = 1; q <= 99; q++) {
      double phi = q / 100.0;
      subtotal = cdfResult[q-1];
      assert subtotal >= phi - 0.01;
      assert subtotal <= phi + 0.01;
    }
    // should probably assert that the final cdf value is 1.0
  }

  @Test
  public void checkConstructAuxiliary() {
    for (int k = 1; k <= 32; k+= 31) {
      QuantilesSketch qs = new QuantilesSketch(k);
      for (int numItemsSoFar = 0; numItemsSoFar < 1000; numItemsSoFar++) {
        Auxiliary au = qs.constructAuxiliary();
        int numSamples = qs.numValidSamples();
        double[] auxItems = au.auxSamplesArr_;
        long[] auxAccum = au.auxCumWtsArr_;

        assert qs.getK() == au.auxK_;
        assert qs.getN() == au.auxN_;
        assert numItemsSoFar == au.auxN_;

        assert auxItems.length == numSamples;
        assert auxAccum.length == numSamples + 1;

        double mqSumOfSamples = qs.sumOfSamplesInSketch();
        double auSumOfSamples = Util.sumOfDoublesInSubArray(auxItems, 0, numSamples);

        // the following test might be able to detect errors in handling the samples
        // e.g. accidentally dropping or duplicating a sample
        assert Math.floor(0.5 + mqSumOfSamples) == Math.floor(0.5 + auSumOfSamples);

        // the following test might be able to detect errors in handling the sample weights
        assert auxAccum[numSamples] == numItemsSoFar;

        for (int i = 0; i < numSamples-1; i++) {
          assert auxItems[i] <= auxItems[i+1]; // assert sorted order
          assert auxAccum[i] <  auxAccum[i+1]; // assert cumulative property
        }

        // This is a better test when the items are inserted in reverse order
        // as follows, but the negation seems kind of awkward.
        qs.update (-1.0 * (numItemsSoFar + 1) );
      } // end of loop over test stream
    } // end of loop over values of k
  }

  @Test
  public void checkBigMinMax () {
    QuantilesSketch mq1  = new QuantilesSketch (32);
    QuantilesSketch mq2  = new QuantilesSketch (32);
    QuantilesSketch mq3  = new QuantilesSketch (32);
    for (int i = 999; i >= 1; i--) {
      mq1.update(i);
      mq2.update(1000+i);
      mq3.update(i);
    }
    assert (mq1.getQuantile (0.0) == 1.0);
    assert (mq1.getQuantile (1.0) == 999.0);

    assert (mq2.getQuantile (0.0) == 1001.0);
    assert (mq2.getQuantile (1.0) == 1999.0);

    assert (mq3.getQuantile (0.0) == 1.0);
    assert (mq3.getQuantile (1.0) == 999.0);

    double[] queries = {0.0, 1.0};

    double[] resultsA = mq1.getQuantiles(queries);
    assert (resultsA[0] == 1.0);
    assert (resultsA[1] == 999.0);

    QuantilesSketch.mergeInto (mq2, mq1);
    QuantilesSketch.mergeInto (mq3, mq2);

    double[] resultsB = mq1.getQuantiles(queries);
    assert (resultsB[0] == 1.0);
    assert (resultsB[1] == 1999.0);

    double[] resultsC = mq2.getQuantiles(queries);
    assert (resultsC[0] == 1.0);
    assert (resultsC[1] == 1999.0);
  }

  @Test
  public void checkSmallMinMax () {
    QuantilesSketch mq1  = new QuantilesSketch (32);
    QuantilesSketch mq2  = new QuantilesSketch (32);
    QuantilesSketch mq3  = new QuantilesSketch (32);
    for (int i = 8; i >= 1; i--) {
      mq1.update(i);
      mq2.update(10+i);
      mq3.update(i);
    }
    assert (mq1.getQuantile (0.0) == 1.0);
    assert (mq1.getQuantile (0.5) == 5.0);
    assert (mq1.getQuantile (1.0) == 8.0);

    assert (mq2.getQuantile (0.0) == 11.0);
    assert (mq2.getQuantile (0.5) == 15.0);
    assert (mq2.getQuantile (1.0) == 18.0);

    assert (mq3.getQuantile (0.0) == 1.0);
    assert (mq3.getQuantile (0.5) == 5.0);
    assert (mq3.getQuantile (1.0) == 8.0);

    double[] queries = {0.0, 0.5, 1.0};

    double[] resultsA = mq1.getQuantiles(queries);
    assert (resultsA[0] == 1.0);
    assert (resultsA[1] == 5.0);
    assert (resultsA[2] == 8.0);

    QuantilesSketch.mergeInto (mq2, mq1);
    QuantilesSketch.mergeInto (mq3, mq2);

    double[] resultsB = mq1.getQuantiles(queries);
    assert (resultsB[0] == 1.0);
    assert (resultsB[1] == 11.0);
    assert (resultsB[2] == 18.0);

    double[] resultsC = mq2.getQuantiles(queries);
    assert (resultsC[0] == 1.0);
    assert (resultsC[1] == 11.0);
    assert (resultsC[2] == 18.0);
  }
  
  @Test
  public void checkToStringDetail() {
    int k = 227;
    QuantilesSketch qs = new QuantilesSketch(k);
    
    int n = 1000000;
    for (int i=1; i<=n; i++) {
      qs.update(i);
    }
    println(qs.toString());
    println(qs.toString(false, true));
    
    int n2 = (int)qs.getStreamLength();
    assertEquals(n2, n);
    qs.update(Double.NaN);
    qs.reset();
    assertEquals(qs.getStreamLength(), 0);
  }
  
  @Test
  public void checkMisc() {
    int k = 227;
    QuantilesSketch qs = new QuantilesSketch(k);
    
    int n = 1000000;
    for (int i=1; i<=n; i++) {
      qs.update(i);
    }
    int n2 = (int)qs.getStreamLength();
    assertEquals(n2, n);
    qs.update(Double.NaN);
    qs.reset();
    assertEquals(qs.getStreamLength(), 0);
  }
  
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void checkConstructorException() {
    @SuppressWarnings("unused")
    QuantilesSketch qs = new QuantilesSketch(0);
  }
  
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void checkGetQuantiles() {
    int k = 227;
    QuantilesSketch qs = new QuantilesSketch(k);
    
    int n = 1000000;
    for (int i=1; i<=n; i++) {
      qs.update(i);
    }
    double[] frac = {-0.5};
    qs.getQuantiles(frac);
  }
  
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void checkGetQuantile() {
    int k = 227;
    QuantilesSketch qs = new QuantilesSketch(k);
    
    int n = 1000000;
    for (int i=1; i<=n; i++) {
      qs.update(i);
    }
    double frac = -0.5;
    qs.getQuantile(frac);
  }
  
  @Test
  public void simpleQuantilesCheck() {
    int k = 256;
    QuantilesSketch qs = new QuantilesSketch(k);
    int n = 1000000;
    int start = 0;
    for (int i=0; i<n; i++) {
      qs.update(start + i);
    }
    double rankError = qs.getNormalizedRankError();
    
    double[] ranks = {0.0, 0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9, 1.0};
    double[] values = qs.getQuantiles(ranks);
    double maxV = qs.getMaxValue();
    double minV = qs.getMinValue();
    double delta = maxV - minV;
    println("This prints the relative value errors for illustration.");
    println("The sketch does not and can not guarantee relative value errors, period!");
    for (int i=0; i<ranks.length; i++) {
      double rank = ranks[i];
      double value = values[i];
      if (rank == 0.0) { assertEquals(value, minV, 0.0); }
      else if (rank == 1.0) { assertEquals(value, maxV, 0.0); }
      else {
        double rankUB = rank + rankError;
        double valueUB = minV + delta*rankUB;
        double rankLB = Math.max(rank - rankError, 0.0);
        double valueLB = minV + delta*rankLB;
        assertTrue(value < valueUB);
        assertTrue(value > valueLB);

        double valRelPctErrUB = valueUB/ value -1.0;
        double valRelPctErrLB = valueLB/ value -1.0;
        println(valueLB+" <= "+value+" <= "+valueUB+", UBerr: "+ valRelPctErrUB+", LBer: "+valRelPctErrLB);
      }
    }
  }
  
  @Test
  public void checkComputeNumLevelsNeeded() {
    int n = 1 << 20;
    int k = 227;
    int lvls1 = computeNumLevelsNeeded(k, n);
    int lvls2 = (int)Math.max(floor(lg((double)n/k)),0);
    assertEquals(lvls1, lvls2);
  }
  
  @Test
  public void checkComputeBitPattern() {
    int n = 1 << 20;
    int k = 227;
    long bitP = computeBitPattern(k, n);
    assertEquals(bitP, n/(2L*k));
  }
  
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void checkValidateSplitPoints() {
    double[] arr = {2, 1};
    validateSplitPoints(arr);
  }
  
  @Test
  public void checkMerge() {
    int k = 227;
    QuantilesSketch qs1 = new QuantilesSketch(k);
    QuantilesSketch qs2 = new QuantilesSketch(k);
    
    int n = 1000000;
    for (int i=1; i<=n; i++) {
      qs1.update(i);
    }
    qs2.merge(qs1);
    double med1 = qs1.getQuantile(0.5);
    double med2 = qs2.getQuantile(0.5);
    println(med1+","+med2);
    
  }
  
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void checkMergeException() {
    int k = 227;
    QuantilesSketch qs1 = new QuantilesSketch(k);
    QuantilesSketch qs2 = new QuantilesSketch(2*k);
    qs2.merge(qs1);
  }
  
  @Test
  public void checkInternalBuildHistogram() {
    int k = 227;
    QuantilesSketch qs = new QuantilesSketch(k);
    int n = 1000000;
    for (int i=1; i<=n; i++) {
      qs.update(i);
    }
    double[] spts = {100000, 500000, 900000};
    qs.getPDF(spts);
  }
  
  
  @Test
  public void checkComputeBaseBufferCount() {
    int n = 1 << 20;
    int k = 227;
    long bbCnt = computeBaseBufferCount(k, n);
    assertEquals(bbCnt, n % (2L*k));
  }
  
  @Test
  public void printlnTest() {
    println("PRINTING: "+this.getClass().getName());
  }
  
  /**
   * @param s value to print 
   */
  static void println(String s) {
    System.out.println(s); //disable here
  }
  
}