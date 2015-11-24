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
  public void regressionTestMergeableQuantileSketchStructureAfterUpdates () {
    for (int k = 1; k <= 100; k++) { // was 300
      long longK = (long) k;
      MergeableQuantileSketch mq = new MergeableQuantileSketch (k);
      for (long n = 0; n < (longK * longK + 100); n++) {
        MergeableQuantileSketch.validateMergeableQuantileSketchStructure (mq, k, n);
        mq.update (Math.random ());
      }
      //      if (k % 10 == 0) { System.out.printf ("Tested updates with k = %d\n", k); }
    }
    //System.out.printf ("Passed: regressionTestMergeableQuantileSketchStructureAfterUpdates ()\n");
  }

  /* the cost of testing for k is k^4, */
  @Test
  public void regressionTestMergeableQuantileSketchStructureAfterMerges () {
    for (int k = 1; k <= 9; k++) { // was 20
      long longK = (long) k;
      for (long n = 0; n < (longK * longK + 100); n++) {
        for (long n1 = 0; n1 <= n; n1++) {
          long n2 = n - n1;
          MergeableQuantileSketch mq1 = new MergeableQuantileSketch (k);
          MergeableQuantileSketch mq2 = new MergeableQuantileSketch (k);
          
          for (long j1 = 0; j1 < n1; j1++) {
            mq1.update (Math.random ());            
          }

          for (long j2 = 0; j2 < n2; j2++) {
            mq2.update (Math.random ());            
          }

          MergeableQuantileSketch.mergeInto (mq1, mq2);

          MergeableQuantileSketch.validateMergeableQuantileSketchStructure (mq1, k, n1 + n2);

        } // end of loop over n1
      } // end of loop over n
      //      System.out.printf ("Tested merges with k = %d\n", k);
    } // end of loop over k
    //System.out.printf ("Passed: regressionTestMergeableQuantileSketchStructureAfterMerges ()\n");
  }

  @Test
  public void testConstructAuxiliary () {
    for (int k = 1; k <= 32; k+= 31) {
      MergeableQuantileSketch mq = new MergeableQuantileSketch (k);
      for (int numItemsSoFar = 0; numItemsSoFar < 1000; numItemsSoFar++) {
        Auxiliary au = mq.constructAuxiliary ();
        int numSamples = mq.numSamplesInSketch ();
        double [] auxItems = au.auItems;
        long [] auxAccum = au.auAccum;

        assert mq.getK() == au.auK;
        assert mq.getN() == au.auN;
        assert numItemsSoFar == au.auN;

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

    //System.out.printf ("Passed: testConstructAuxiliary()\n");
  }


  // Please note that this is a randomized test that CAN fail.
  // The probability of failure could be reduced by increasing k.
  @Test
  public void endToEndTest () {
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

    MergeableQuantileSketch.mergeInto (mq, mq2);

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


      //      System.out.printf ("%d\t%.6f\t%.6f\n", q, phi, subtotal);


  // a couple of basic unit tests for the histogram construction helper functions.
  @Test
  public void testQuadraticTimeIncrementHistogramCounters () {

    double [] samples = {0.1, 0.2, 0.3, 0.4, 0.5};

    {
      double [] splitPoints = {0.25, 0.4};
      long counters [] = {0, 0, 0};
      long answers  [] = {200, 100, 200};
      Util.quadraticTimeIncrementHistogramCounters (samples, 5, 100, splitPoints, counters);
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
      Util.quadraticTimeIncrementHistogramCounters (samples, 5, 100, splitPoints, counters);
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
      Util.quadraticTimeIncrementHistogramCounters (samples, 5, 100, splitPoints, counters);
      for (int j = 0; j < counters.length; j++) {
        assert counters[j] == answers[j];
        // System.out.printf ("counter[%d] = %d\n", j, counters[j]);
      }
      // System.out.printf ("\n");
    } 

   //System.out.printf ("Passed: quadraticTimeIncrementHistogramCounters\n");

  }

  @Test
  public void testLinearTimeIncrementHistogramCounters () {
    double [] samples = {0.1, 0.2, 0.3, 0.4, 0.5};

    {
      double [] splitPoints = {0.25, 0.4};
      long counters [] = {0, 0, 0};
      long answers  [] = {200, 100, 200};
      Util.linearTimeIncrementHistogramCounters (samples, 5, 100, splitPoints, counters);
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
      Util.linearTimeIncrementHistogramCounters (samples, 5, 100, splitPoints, counters);
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
      Util.linearTimeIncrementHistogramCounters (samples, 5, 100, splitPoints, counters);
      for (int j = 0; j < counters.length; j++) {
        assert counters[j] == answers[j];
        // System.out.printf ("counter[%d] = %d\n", j, counters[j]);
      }
      // System.out.printf ("\n");
    } 

   //System.out.printf ("Passed: linearTimeIncrementHistogramCounters\n");
  }

  /* need to write tests where there are zero or one splitpoints, and zero samples */

  static void runRegressionTests () {
    MergeableQuantileSketchTest mqst = new MergeableQuantileSketchTest();
    mqst.regressionTestMergeableQuantileSketchStructureAfterUpdates ();
    mqst.regressionTestMergeableQuantileSketchStructureAfterMerges ();
    mqst.testQuadraticTimeIncrementHistogramCounters ();
    mqst.testLinearTimeIncrementHistogramCounters ();
    mqst.testConstructAuxiliary ();
    mqst.endToEndTest ();
    UtilTest utest = new UtilTest();
    utest.checkBlockyTandemMergeSort();
    
    System.out.println("Regression Tests Complete.");
  }

  
  public static void main (String [] args) {
    runRegressionTests ();
  }

}