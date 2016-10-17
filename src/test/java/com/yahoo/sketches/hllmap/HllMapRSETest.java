/*
 * Copyright 2016, Yahoo! Inc. Licensed under the terms of the
 * Apache License 2.0. See LICENSE file at the project root for terms.
 */

package com.yahoo.sketches.hllmap;

import static com.yahoo.sketches.hllmap.MapTestingUtil.LS;
import static com.yahoo.sketches.hllmap.MapTestingUtil.TAB;
import static com.yahoo.sketches.hllmap.MapTestingUtil.bytesToString;
import static com.yahoo.sketches.hllmap.MapTestingUtil.evenlyLgSpaced;
import static com.yahoo.sketches.hllmap.MapTestingUtil.intToBytes;
import static com.yahoo.sketches.hllmap.MapTestingUtil.longToBytes;
import static com.yahoo.sketches.hllmap.MapTestingUtil.milliSecToString;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import org.testng.annotations.Test;

import com.yahoo.sketches.ResizeFactor;
import com.yahoo.sketches.hll.HllSketch;
import com.yahoo.sketches.hll.HllSketchBuilder;
import com.yahoo.sketches.theta.Sketch;
import com.yahoo.sketches.theta.Sketches;
import com.yahoo.sketches.theta.UpdateSketch;
import com.yahoo.sketches.theta.UpdateSketchBuilder;

public class HllMapRSETest {

  enum SketchEnum { HLL_MAP, THETA, HLL}

  //@Test
  public void test1() {
    assertEquals(1, 1);
    assertTrue(true);
    assertFalse(false);
    println(TAB + LS);
  }


  @Test
  public void testHllMap() {
    testRSE(SketchEnum.HLL_MAP);
    println(LS);
  }
  @Test
  public void testTheta() {
    testRSE(SketchEnum.THETA);
    println(LS);
  }

  @Test
  public void testHll() {
    testRSE(SketchEnum.HLL);
    println(LS);
  }

  @SuppressWarnings("null")
  public void testRSE(SketchEnum skEnum) {
    //test parameters
    int startLgX = 0; //1
    int endLgX = 16;  //65K
    int startLgTrials = 6;
    int endLgTrials = startLgTrials;
    int ppo = 4; //Points per Octave

    int points = ppo * (endLgX - startLgX) + 1;
    int[] xPoints = evenlyLgSpaced(startLgX, endLgX, points);
    int[] tPoints = evenlyLgSpaced(startLgTrials, endLgTrials, points);

    //HllMap config
    int keySize = 4;
    int lgK = 9;
    int k = 1 << lgK;
    float rf = 2.0F;
    int initEntries = 1 << startLgTrials;

    //Other
    HllMap hllMap = null;
    UpdateSketchBuilder thBldr = Sketches.updateSketchBuilder().setResizeFactor(ResizeFactor.X1).setNominalEntries(k);
    UpdateSketch thSketch = null;
    HllSketchBuilder hllBldr = HllSketch.builder().setLogBuckets(lgK).setHipEstimator(true);
    HllSketch hllSk = null;
    long v = 0;
    byte[] ipv4bytes = new byte[4];
    //byte[] ipv6bytes = new byte[16];
    byte[] valBytes = new byte[8];

    if (skEnum == SketchEnum.HLL_MAP) {
      println("HllMap: k:\t" + k);
    } else if (skEnum == SketchEnum.THETA) {
      thSketch = thBldr.build();
      println("Theta Sketch: k:\t" + k);
    } else { //HLL
      hllSk = hllBldr.build();
      println("HLL Sketch: k:\t" + k);
    }

    println("U\tTrials\tMean\tBias\tRSE");
    double sum=0, sumErr=0,sumErrSq=0;

    //at each point do multiple trials.
    long startMs = System.currentTimeMillis(); //start the clock
    long totnS = 0;
    long lastX = 0;
    for (int pt = 0; pt < points ; pt++) {
      int x = xPoints[pt];
      if (x == lastX) continue;
      lastX = x;
      sum = sumErr = sumErrSq = 0;
      int trials = tPoints[pt];
      int ipv4 = 10 << 24; //10.0.0.0
      if (skEnum == SketchEnum.HLL_MAP) {
        hllMap = HllMap.getInstance(initEntries, keySize, k, rf); //renew per trial set
      } //else do nothing to the other sketches
      for (int t = 0; t < trials; t++) { //each trial
        double est = 0;
        long startnS = 0, endnS = 0;
        if (skEnum == SketchEnum.HLL_MAP) {
          ipv4++;  //different IP for each trial
          /********************/
          startnS = System.nanoTime();
          for (long i=0; i< x; i++) { //x is the #uniques per trial
            v++;  //different values for the uniques
            ipv4bytes = intToBytes(ipv4, ipv4bytes);
            valBytes = longToBytes(v, valBytes);
            int coupon = Map.coupon16(valBytes, k);
            est = hllMap.update(ipv4bytes, coupon);
          }
          endnS = System.nanoTime();
          /********************/
        } else if (skEnum == SketchEnum.THETA) {
          thSketch.reset();
          /********************/
          startnS = System.nanoTime();
          for (long i=0; i< x; i++) { //x is the #uniques per trial
            v++;  //different values for the uniques
            thSketch.update(v);
          }
          endnS = System.nanoTime();
          /********************/
        } else { //HLL
          hllSk = hllBldr.build(); //no reset on HLL !
          /********************/
          startnS = System.nanoTime();
          for (long i=0; i< x; i++) { //x is the #uniques per trial
            v++;  //different values for the uniques
            hllSk.update(v);
          }
          endnS = System.nanoTime();
          /********************/
        }
        totnS += endnS - startnS;
        if (skEnum == SketchEnum.THETA) {
          thSketch.rebuild();
          est = thSketch.getEstimate();
        } else if (skEnum == SketchEnum.HLL) {
          est = hllSk.getEstimate();
        }
        sum += est;
        double err = est - x;
        sumErr += err;
        sumErrSq += err * err;
      }
      double mean = sum /trials;
      double meanErr = sumErr/trials;
      double varErr = (sumErrSq - meanErr * sumErr/trials)/(trials -1);
      double relErr = Math.sqrt(varErr)/x;
      double bias = mean/x - 1.0;
      String line = String.format("%d\t%d\t%.2f\t%.2f%%\t%.2f%%", x, trials, mean, bias*100, relErr*100);
      println(line);
    }
    println(String.format("\nUpdates          :\t%,d", v));
    if (skEnum == SketchEnum.HLL_MAP) {
      println(String.format("Table  Entries   :\t%,d",hllMap.getTableEntries()));
      println(String.format("Capacity Entries :\t%,d",hllMap.getCapacityEntries()));
      println(String.format("Count Entries    :\t%,d",hllMap.getCurrentCountEntries()));
      println(              "Entry bytes      :\t" + hllMap.getEntrySizeBytes());
      println(String.format("RAM Usage Bytes  :\t%,d",hllMap.getMemoryUsageBytes()));
    } else if (skEnum == SketchEnum.THETA) {
      println(String.format("Sketch Size Bytes:\t%,d", Sketch.getMaxUpdateSketchBytes(k)));
    } else {
      println(String.format("Sketch Size Bytes:\t%,d", k + 16));
    }
    long endMs = System.currentTimeMillis();
    long deltamS = endMs - startMs;
    double updnS2 = ((double)totnS)/v;
    println(String.format(  "nS/Update        :\t%.1f", updnS2));
    println(                "Total: H:M:S.mS  :\t"+milliSecToString(deltamS));
  }



  static void printIPandValue(byte[] ip, byte[] value) {
    println("IP:\t"+bytesToString(ip, false, false, ".")
      + "\tVal:\t"+bytesToString(value, false, false, "."));
  }

  public static void main(String[] args) {

  }

  static void println(String s) { System.out.println(s); }
  static void print(String s) { System.out.print(s); }
}
