/*
 * Copyright 2015, Yahoo! Inc.
 * Licensed under the terms of the Apache License 2.0. See LICENSE file at the project root for terms.
 */
package com.yahoo.sketches.quantiles;

import org.testng.annotations.Test;

@SuppressWarnings("cast")
public class MergeableQuantileSketchTest { 

  /* the cost of testing for k is k^2, */
  @Test
  public void regressionTestMergeableQuantileSketchStructureAfterUpdates() {
    for (int k = 1; k <= 100; k++) { // was 300
      long longK = (long) k;
      MergeableQuantileSketch mq = new MergeableQuantileSketch (k);
      for (long n = 0; n < (longK * longK + 100); n++) {
        MergeableQuantileSketch.validateMergeableQuantileSketchStructure (mq, k, n);
        mq.update (Math.random ());
      }
      //if (k % 10 == 0) { System.out.printf ("Tested updates with k = %d\n", k); }
    }
  }

  /* the cost of testing for k is k^4, */
  @Test
  public void regressionTestMergeableQuantileSketchStructureAfterMerges() {
    for (int k = 1; k <= 9; k++) { // was 20
      long longK = (long) k;
      for (long n = 0; n < (longK * longK + 100); n++) {
        for (long n1 = 0; n1 <= n; n1++) {
          long n2 = n - n1;
          MergeableQuantileSketch mq1 = new MergeableQuantileSketch(k);
          MergeableQuantileSketch mq2 = new MergeableQuantileSketch(k);
          
          for (long j1 = 0; j1 < n1; j1++) {
            mq1.update (Math.random ());            
          }

          for (long j2 = 0; j2 < n2; j2++) {
            mq2.update (Math.random ());            
          }

          MergeableQuantileSketch.mergeInto (mq2, mq1);
          MergeableQuantileSketch.validateMergeableQuantileSketchStructure(mq1, k, n1 + n2);

        } // end of loop over n1
      } // end of loop over n
      //      System.out.printf ("Tested merges with k = %d\n", k);
    } // end of loop over k
  }

  @Test
  public void testConstructAuxiliary() {
    for (int k = 1; k <= 32; k+= 31) {
      MergeableQuantileSketch mq = new MergeableQuantileSketch(k);
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
        double auSumOfSamples = Util.sumOfDoublesInArrayPrefix (auxItems, numSamples);

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
  }

  // Please note that this is a randomized test that CAN fail.
  // The probability of failure could be reduced by increasing k.
  // Actually, setting the seed made it deterministic.
  @Test
  public void endToEndTest () {
    Util.rand.setSeed (917351); // arbitrary seed that makes this test deterministic

    MergeableQuantileSketch mq  = new MergeableQuantileSketch (256);
    MergeableQuantileSketch mq2 = new MergeableQuantileSketch (256);

    for (int item = 1000000; item >= 1; item--) {
      if (item % 4 == 0) {
        mq.update ((double) item);
      }
      else {
        mq2.update ((double) item);
      }
    }

    MergeableQuantileSketch.mergeInto (mq2, mq);

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
  }

  @Test
   static void bigTestMinMax () {
    MergeableQuantileSketch mq1  = new MergeableQuantileSketch (32);
    MergeableQuantileSketch mq2  = new MergeableQuantileSketch (32);
    MergeableQuantileSketch mq3  = new MergeableQuantileSketch (32);
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

    MergeableQuantileSketch.mergeInto (mq2, mq1);
    MergeableQuantileSketch.mergeInto (mq3, mq2);

    double [] resultsB = mq1.getQuantiles(queries);
    assert (resultsB[0] == 1.0);
    assert (resultsB[1] == 1999.0);

    double [] resultsC = mq2.getQuantiles(queries);
    assert (resultsC[0] == 1.0);
    assert (resultsC[1] == 1999.0);
  }

  @Test
  static void smallTestMinMax () {
    MergeableQuantileSketch mq1  = new MergeableQuantileSketch (32);
    MergeableQuantileSketch mq2  = new MergeableQuantileSketch (32);
    MergeableQuantileSketch mq3  = new MergeableQuantileSketch (32);
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

    MergeableQuantileSketch.mergeInto (mq2, mq1);
    MergeableQuantileSketch.mergeInto (mq3, mq2);

    double [] resultsB = mq1.getQuantiles(queries);
    assert (resultsB[0] == 1.0);
    assert (resultsB[1] == 11.0);
    assert (resultsB[2] == 18.0);

    double [] resultsC = mq2.getQuantiles(queries);
    assert (resultsC[0] == 1.0);
    assert (resultsC[1] == 11.0);
    assert (resultsC[2] == 18.0);
  }

  public void qsAuxVersusMQSAux () { //TODO Depends on both MQS and QS
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
    QuantilesSketch mq6 = new QuantilesSketch(16);
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
  }
  
  static void runRegressionTests () {
    MergeableQuantileSketchTest mqst = new MergeableQuantileSketchTest();
    MergeableQuantileSketchTest.bigTestMinMax ();   
    MergeableQuantileSketchTest.smallTestMinMax ();   
    mqst.regressionTestMergeableQuantileSketchStructureAfterUpdates ();
    mqst.regressionTestMergeableQuantileSketchStructureAfterMerges ();
    mqst.testConstructAuxiliary ();
    mqst.endToEndTest ();
    mqst.qsAuxVersusMQSAux();

    QuantilesSketchTest mq6t = new QuantilesSketchTest();
    mq6t.checkEndToEnd ();
    mq6t.checkConstructAuxiliary ();
    mq6t.checkBigMinMax ();
    mq6t.checkSmallMinMax ();

    UtilTest utest = new UtilTest();
    utest.testPOLZBSA ();
    utest.checkBlockyTandemMergeSort();
    utest.testQuadraticTimeIncrementHistogramCounters ();
    utest.testLinearTimeIncrementHistogramCounters ();
  }
  
  public static void main (String [] args) {
    runRegressionTests ();
  }

}