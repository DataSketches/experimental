/*
 * Copyright 2015, Yahoo! Inc.
 * Licensed under the terms of the Apache License 2.0. See LICENSE file at the project root for terms.
 */
package com.yahoo.sketches.quantiles;

import org.testng.annotations.Test;
import static org.testng.Assert.assertEquals;

@SuppressWarnings("cast")
public class QuantilesSketchTest { 

  @Test
    public void testAdjustedFindEpsForK () {
    // note: there is a big fudge factor in these numbers, so they don't need to be computed exactly
    double absTol = 1e-14; // we just want to catch gross bugs
    int [] kArr = {2,16,1024,1 << 30};
    double [] epsArr = { // these were computed by an earlier ocaml version of the function
      0.821714930853465,
      0.12145410223356,
      0.00238930378957284,
      3.42875166500824e-09 };
    for (int i = 0; i < 4; i++) {
      assertEquals(epsArr[i], 
                   QuantilesSketch.EpsilonFromK.adjustedFindEpsForK (kArr[i]),
                   absTol,
                   "adjustedFindEpsForK() doesn't match precomputed value");
    }
    for (int i = 0; i < 3; i++) {
      QuantilesSketch mq = new QuantilesSketch (kArr[i]);
      assertEquals(epsArr[i], 
                   mq.getNormalizedCountError(),
                   absTol,
                   "getNormalizedCountError() doesn't match precomputed value");
    }
  }

  // Please note that this is a randomized test that CAN fail.
  // The probability of failure could be reduced by increasing k.
  // Actually, setting the seed has now made it deterministic.
  @Test
  public void endToEndTest6 () {

    Util.rand.setSeed (917351); // arbitrary seed that makes this test deterministic

    QuantilesSketch mq  = new QuantilesSketch (256);
    QuantilesSketch mq2 = new QuantilesSketch (256);

    for (int item = 1000000; item >= 1; item--) {
      if (item % 4 == 0) {
        mq.update ((double) item);
      }
      else {
        mq2.update ((double) item);
      }
    }

    QuantilesSketch.mergeInto (mq, mq2);

    int numPhiValues = 99;
    double [] phiArr = new double [numPhiValues];
    for (int q = 1; q <= 99; q++) {
      phiArr[q-1] = ((double) q) / 100.0;
    }
    double [] splitPoints = mq.getQuantiles (phiArr);

    /*
    for (int i = 0; i < 99; i++) {
      System.out.printf ("%d\t%.6f\t%.6f\n", i, phiArr[i], splitPoints[i]);
    }
    */

    for (int q = 1; q <= 99; q++) {
      double nominal = 1e6 * ((double) q) / 100.0;
      double reported = splitPoints[q-1];
      assert reported >= nominal - 10000.0;
      assert reported <= nominal + 10000.0;
    }

    double [] pdfResult = mq.getPDF (splitPoints);
    double subtotal = 0.0;
    for (int q = 1; q <= 99; q++) {
      double phi = ((double) q) / 100.0;
      subtotal += pdfResult[q-1];
      assert subtotal >= phi - 0.01;
      assert subtotal <= phi + 0.01;
    }
    // should probably assert that the pdf sums to 1.0

    double [] cdfResult = mq.getCDF (splitPoints);
    for (int q = 1; q <= 99; q++) {
      double phi = ((double) q) / 100.0;
      subtotal = cdfResult[q-1];
      assert subtotal >= phi - 0.01;
      assert subtotal <= phi + 0.01;
    }
    // should probably assert that the final cdf value is 1.0

    //System.out.printf ("Passed: endToEndTest()\n");
  }


  @Test
  public void testConstructAuxiliary6 () {
    for (int k = 1; k <= 32; k+= 31) {
      QuantilesSketch mq = new QuantilesSketch (k);
      for (int numItemsSoFar = 0; numItemsSoFar < 1000; numItemsSoFar++) {
        Auxiliary au = mq.constructAuxiliary ();
        int numSamples = mq.numSamplesInSketch ();
        double [] auxItems = au.auxSamplesArr_;
        long [] auxAccum = au.auxCumWtsArr_;

        assert mq.getK() == au.auxK_;
        assert mq.getN() == au.auxN_;
        assert numItemsSoFar == au.auxN_;

        assert auxItems.length == numSamples;
        assert auxAccum.length == numSamples + 1;

        double mqSumOfSamples = mq.sumOfSamplesInSketch ();
        double auSumOfSamples = Util.sumOfDoublesInSubArray (auxItems, 0, numSamples);

        // the following test might be able to detect errors in handling the samples
        // e.g. accidentally dropping or duplicating a sample
        assert (Math.floor(0.5 + mqSumOfSamples)) == (Math.floor(0.5 + auSumOfSamples));

        // the following test might be able to detect errors in handling the sample weights
        assert auxAccum[numSamples] == numItemsSoFar;

        for (int i = 0; i < numSamples-1; i++) {
          assert auxItems[i] <= auxItems[i+1]; // assert sorted order
          assert auxAccum[i] <  auxAccum[i+1]; // assert cumulative property
        }

        // This is a better test when the items are inserted in reverse order
        // as follows, but the negation seems kind of awkward.
        mq.update (-1.0 * ((double) (numItemsSoFar + 1)));

      } // end of loop over test stream

    } // end of loop over values of k

    //    System.out.printf ("Passed: testConstructAuxiliary6()\n");
  }

  public void auxVersusAux6 () {
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
  public void bigTestMinMax6 () {
    QuantilesSketch mq1  = new QuantilesSketch (32);
    QuantilesSketch mq2  = new QuantilesSketch (32);
    QuantilesSketch mq3  = new QuantilesSketch (32);
    for (int i = 999; i >= 1; i--) {
      mq1.update ((double) i);
      mq2.update ((double) (1000+i));
      mq3.update ((double) i);
    }
    assert (mq1.getQuantile (0.0) == 1.0);
    assert (mq1.getQuantile (1.0) == 999.0);

    assert (mq2.getQuantile (0.0) == 1001.0);
    assert (mq2.getQuantile (1.0) == 1999.0);

    assert (mq3.getQuantile (0.0) == 1.0);
    assert (mq3.getQuantile (1.0) == 999.0);

    double [] queries = {0.0, 1.0};

    double [] resultsA = mq1.getQuantiles(queries);
    assert (resultsA[0] == 1.0);
    assert (resultsA[1] == 999.0);

    QuantilesSketch.mergeInto (mq1, mq2);
    QuantilesSketch.mergeInto (mq2, mq3);

    double [] resultsB = mq1.getQuantiles(queries);
    assert (resultsB[0] == 1.0);
    assert (resultsB[1] == 1999.0);

    double [] resultsC = mq2.getQuantiles(queries);
    assert (resultsC[0] == 1.0);
    assert (resultsC[1] == 1999.0);

    //    System.out.printf ("Passed: bigTestMinMax6\n");
  }


  @Test
  public void smallTestMinMax6 () {
    QuantilesSketch mq1  = new QuantilesSketch (32);
    QuantilesSketch mq2  = new QuantilesSketch (32);
    QuantilesSketch mq3  = new QuantilesSketch (32);
    for (int i = 8; i >= 1; i--) {
      mq1.update ((double) i);
      mq2.update ((double) (10+i));
      mq3.update ((double) i);
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

    double [] queries = {0.0, 0.5, 1.0};

    double [] resultsA = mq1.getQuantiles(queries);
    assert (resultsA[0] == 1.0);
    assert (resultsA[1] == 5.0);
    assert (resultsA[2] == 8.0);

    QuantilesSketch.mergeInto (mq1, mq2);
    QuantilesSketch.mergeInto (mq2, mq3);

    double [] resultsB = mq1.getQuantiles(queries);
    assert (resultsB[0] == 1.0);
    assert (resultsB[1] == 11.0);
    assert (resultsB[2] == 18.0);

    double [] resultsC = mq2.getQuantiles(queries);
    assert (resultsC[0] == 1.0);
    assert (resultsC[1] == 11.0);
    assert (resultsC[2] == 18.0);

    //    System.out.printf ("Passed: smallTestMinMax6\n");
  }


}