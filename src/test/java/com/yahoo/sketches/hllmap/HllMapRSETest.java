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

import com.yahoo.sketches.theta.Sketch;
import com.yahoo.sketches.theta.Sketches;
import com.yahoo.sketches.theta.UpdateSketch;

public class HllMapRSETest {

  //@Test
  public void test1() {
    assertEquals(1, 1);
    assertTrue(true);
    assertFalse(false);
    println(TAB + LS);
  }

  @Test
  public void testHllMap() {
    testRSE(true);
    println(LS);
  }
  @Test
  public void testSketch() {
    testRSE(false);
    println(LS);
  }

  @SuppressWarnings("null")
  public void testRSE(boolean useHllMap) {
    //test parameters
    int startLgX = 0;
    int endLgX = 16; //65K
    int startLgTrials = 10;
    int endLgTrials = startLgTrials;
    int ppo = 4; //Points per Octave

    int points = ppo * (endLgX - startLgX) + 1;
    int[] xPoints = evenlyLgSpaced(startLgX, endLgX, points);
    int[] tPoints = evenlyLgSpaced(startLgTrials, endLgTrials, points);

    //HllMap config
    int keySize = 4;
    int k = 512;
    float rf = 2.0F;
    int initEntries = 1 << startLgTrials;

    //Other
    HllMap hllMap = null;
    UpdateSketch sketch = null;
    long v = 0;
    byte[] ipv4bytes = new byte[4];
    //byte[] ipv6bytes = new byte[16];
    byte[] valBytes = new byte[8];

    if (useHllMap) {
      println("HllMap: k: " + k);
    } else {
      sketch = Sketches.updateSketchBuilder().build(k);
      println("Theta Sketch: k: " + k);
    }

    println("U\tTrials\tMean\tBias\tRE");
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
      if (useHllMap) {
        hllMap = HllMap.getInstance(initEntries, keySize, k, rf);
      }
      for (int t = 0; t < trials; t++) { //each trial
        if (useHllMap) {
          ipv4++;  //different IP for each trial
        } else {
          sketch.reset();
        }
        double est = 0;
        long startnS = System.nanoTime();
        for (long i=0; i< x; i++) { //x is the #uniques per trial
          v++;  //different values for the uniques
          if (useHllMap) {
            ipv4bytes = intToBytes(ipv4, ipv4bytes);
            valBytes = longToBytes(v, valBytes);
            //printIPandValue(ipv4bytes, valBytes);
            int coupon = Util.coupon16(valBytes, k);
            est = hllMap.update(ipv4bytes, coupon);
          } else {
            sketch.update(v);
          }
        }
        long endnS = System.nanoTime();
        totnS += endnS - startnS;
        if (!useHllMap) {
          est = sketch.getEstimate();
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
    println(String.format("\nUpdates          : %,d", v));
    if (useHllMap) {
      println(String.format("Table  Entries   : %,d",hllMap.getTableEntries()));
      println(String.format("Capacity Entries : %,d",hllMap.getCapacityEntries()));
      println(String.format("Count Entries    : %,d",hllMap.getCurrentCountEntries()));
      println(               "Entry bytes     : " + hllMap.getEntrySizeBytes());
      println(String.format("RAM Usage Bytes  : %,d",hllMap.getMemoryUsageBytes()));
    } else {
      println(String.format("Sketch Size Bytes: %,d", Sketch.getMaxUpdateSketchBytes(k)));
    }
    long endMs = System.currentTimeMillis();
    long deltamS = endMs - startMs;
    double updnS2 = ((double)totnS)/v;
    println(String.format(  "nS/Update       : %.1f", updnS2));
    println("Total: H:M:S.mS : "+milliSecToString(deltamS));
  }



  static void printIPandValue(byte[] ip, byte[] value) {
    println("IP: "+bytesToString(ip, false, false, ".")
      + "; Val: "+bytesToString(value, false, false, "."));
  }

  public static void main(String[] args) {

  }

  static void println(String s) { System.out.println(s); }
  static void print(String s) { System.out.print(s); }
}
