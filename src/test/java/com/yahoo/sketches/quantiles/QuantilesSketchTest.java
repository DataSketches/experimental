/*
 * Copyright 2015, Yahoo! Inc.
 * Licensed under the terms of the Apache License 2.0. See LICENSE file at the project root for terms.
 */
package com.yahoo.sketches.quantiles;

//import static com.yahoo.sketches.quantiles.Util.*;
//import static com.yahoo.sketches.quantiles.QuantilesSketch.*;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

//@SuppressWarnings("cast")
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
                   QuantilesSketch.EpsilonFromK.getAdjustedEpsilon(kArr[i]),
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

    //System.out.printf("Passed: endToEndTest()\n");
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

    //    System.out.printf ("Passed: testConstructAuxiliary6()\n");
  }

  public void auxVersusAux6 () { //TODO Depends on both MQS and QS
    double phi_inverse = (Math.sqrt(5.0) - 1.0) / 2.0;
    double bouncy = phi_inverse;

    Util.rand.setSeed (917351); // arbitrary seed that makes this test deterministic    
    MergeableQuantileSketch mq  = new MergeableQuantileSketch (16);
    bouncy = phi_inverse;
    for (int i = 0; i < 1357; i++) {
      mq.update (bouncy);
      bouncy += phi_inverse;
      while (bouncy > 1.0) { bouncy -= 1.0; }
    }

    Util.rand.setSeed (917351); // arbitrary seed that makes this test deterministic    
    QuantilesSketch mq6 = new QuantilesSketch (16);
    bouncy = phi_inverse;
    for (int i = 0; i < 1357; i++) {
      mq6.update (bouncy);
      bouncy += phi_inverse;
      while (bouncy > 1.0) { bouncy -= 1.0; }
    }

    Auxiliary au   = mq.constructAuxiliary();
    Auxiliary au6 = mq6.constructAuxiliary();

    assert (au.auxK_ == au6.auxK_);
    assert (au.auxN_ == au6.auxN_);
    assert (au.auxSamplesArr_.length == au6.auxSamplesArr_.length);
    assert (au.auxCumWtsArr_.length == au6.auxCumWtsArr_.length);

    for (int i = 0; i < au.auxSamplesArr_.length; i++) {
      assert (au.auxSamplesArr_[i] == au6.auxSamplesArr_[i]);
    }

    for (int i = 0; i < au.auxCumWtsArr_.length; i++) {
      assert (au.auxCumWtsArr_[i] == au6.auxCumWtsArr_[i]);
    }

    //    System.out.printf ("Passed: AU vs AU6\n");

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

    //    System.out.printf ("Passed: bigTestMinMax6\n");
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

    //    System.out.printf ("Passed: smallTestMinMax6\n");
  }
  
  @Test
  public void checkToString() {
    int k = 256;
    QuantilesSketch qs = new QuantilesSketch(k);
    
    int n = 1000000;
    for (int i=1; i<=n; i++) {
      qs.update(i);
    }
    println(qs.toString(true, true));
  }
  
  @Test
  public void checkQuantilesSimple() {
    int k = 256;
    QuantilesSketch qs = new QuantilesSketch(k);
    int n = 1000000;
    int start = 0;
    for (int i=0; i<n; i++) {
      qs.update(start + i);
    }
    //println(qs.toString(true, true));
    
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
  public void printlnTest() {
    println("PRINTING: "+this.getClass().getName());
  }
  
  /**
   * @param s value to print 
   */
  static void println(String s) {
    //System.out.println(s); //disable here
  }
  
}