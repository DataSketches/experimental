/*
 * Copyright 2016, Yahoo! Inc. Licensed under the terms of the
 * Apache License 2.0. See LICENSE file at the project root for terms.
 */

package com.yahoo.sketches.ip;

import static com.yahoo.sketches.TestingUtil.milliSecToString;
import static com.yahoo.sketches.ip.Util.bytesToString;
import static com.yahoo.sketches.ip.Util.evenlyLgSpaced;
import static com.yahoo.sketches.ip.Util.intToBytes;
import static com.yahoo.sketches.ip.Util.longToBytes;
import static com.yahoo.sketches.ip.Util.println;

import com.yahoo.membership.baltar.UniqueValueCounter;
import com.yahoo.sketches.theta.Sketches;
import com.yahoo.sketches.theta.UpdateSketch;

//import org.testng.annotations.Test;

@SuppressWarnings("unused")
public class UniqueValueCounterRSETest {

  private boolean useUvCounter;
  private UniqueValueCounter uvCounter;
  private UpdateSketch sketch;
  private int ipv4 = 10 << 24; //10.0.0.0
  private long v = 0;
  private byte[] ipv4bytes = new byte[4];
  //private byte[] ipv6bytes = new byte[16];
  private byte[] valBytes = new byte[8];

  //@SuppressWarnings("unused")

  public void testBaltarCounterIPv4() {
    useUvCounter = false;
    int startLgX = 0;
    int endLgX = 16;
    int ppo = 4; //Points per Octave
    int points = ppo * (endLgX - startLgX) + 1;
    int startLgTrials = 10;
    int endLgTrials = 10;
    int[] xPoints = evenlyLgSpaced(startLgX, endLgX, points);
    int[] tPoints = evenlyLgSpaced(startLgTrials, endLgTrials, points);

    if (useUvCounter) {
      uvCounter = new UniqueValueCounter(4);
    } else {
      sketch = Sketches.updateSketchBuilder().build(1 << 9);
    }
    println("U\tTrials\tMean\tBias\tRE");
    double sum=0, sumErr=0,sumErrSq=0;

    //at each point do multiple trials.
    long startMs = System.currentTimeMillis();
    long lastX = 0;
    for (int pt = 0; pt < points ; pt++) {
      int x = xPoints[pt];
      if (x == lastX) continue;
      lastX = x;
      sum = sumErr = sumErrSq = 0;
      int trials = tPoints[pt];

      for (int t = 0; t < trials; t++) { //each trial
        if (useUvCounter) {
          ipv4++;  //different IP for each trial
        } else {
          sketch.reset();
        }
        double est = 0;
        for (long i=0; i< x; i++) { //x is the #uniques per trial
          v++;  //different values for the uniques
          if (useUvCounter) {
            ipv4bytes = intToBytes(ipv4, ipv4bytes);
            valBytes = longToBytes(v, valBytes);
            //printIPandValue(ipv4bytes, valBytes);
            est = uvCounter.add(ipv4bytes, valBytes);
          } else {
            sketch.update(v);
          }
        }
        if (!useUvCounter) {
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
    long endMs = System.currentTimeMillis();
    println("\nH:M:S.mS : "+milliSecToString(endMs - startMs));

  }

  private static int getBFsizeBytes(int count) {
    // count thresholds are per table level
    if (count <= 7) return 4;
    if (count <= 68) return 8;
    if (count <= 1434) return 64;
    if (count <= 29569) return 512;
    if (count <= 147634) return 2048;
    return 8192;
  }

  private static void printIPandValue(byte[] ip, byte[] value) {
    println("IP: "+bytesToString(ip, false, false, ".")
      + "; Val: "+bytesToString(value, false, false, "."));
  }

  public static void main(String[] args) {
    UniqueValueCounterRSETest test = new UniqueValueCounterRSETest();
    test.testBaltarCounterIPv4();
  }

}
