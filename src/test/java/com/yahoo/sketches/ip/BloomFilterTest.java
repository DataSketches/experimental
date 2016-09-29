/*
 * Copyright 2016, Yahoo! Inc. Licensed under the terms of the
 * Apache License 2.0. See LICENSE file at the project root for terms.
 */

package com.yahoo.sketches.ip;

//import org.testng.Assert;
import org.testng.annotations.Test;

import com.yahoo.membership.baltar.LinearProbingHashTable;
import com.yahoo.sketches.hash.MurmurHash3;

public class BloomFilterTest {

  @Test
  public void test1() {
    int sizeBytes = 1 << 20;
    int keySz = 4;
    int valSz = 8192;
    LinearProbingHashTable table = new LinearProbingHashTable(sizeBytes, keySz, valSz);
    int slots = table.max();
    println("Max slots: " + slots);
    println("Max Fill : "+ ((int) (0.7 * slots)));
  }

  @Test
  public void testForInt() {
    long v = 0;
    int trials = 1 << 24;
    println("U\tBias\tRE");
    double sum, sumErr,sumErrSq;

    for (int u=1; u<=43; u++) {
      sum = sumErr = sumErrSq = 0;
      for (int i = 0; i <  trials; i++) {
        int bf = 0;
        for (int j = 0; j < u; j++) {
          bf = setBitInt(v++, bf);
        }
        int est = estCountInt(bf, 32);
        sum += est;
        double err = u - est;
        sumErr += err;
        sumErrSq += err * err;
      }
      double mean = sum /trials;
      double meanErr = sumErr/trials;
      double varErr = (sumErrSq - meanErr * sumErr/trials)/(trials -1);
      double relErr = Math.sqrt(varErr)/u;
      double bias = mean/u - 1.0;
      String line = String.format("%d\t%.2f%%\t%.2f%%", u, bias*100, relErr*100);
      println(line);
    }
  }

  @Test
  public void testForLong() {
    long v = 0;
    int trials = 1 << 24;
    println("U\tBias\tRE");
    double sum, sumErr,sumErrSq;

    for (int u=8; u<=70; u++) {
      sum = sumErr = sumErrSq = 0;
      for (int i = 0; i <  trials; i++) {
        long bf = 0;
        for (int j = 0; j < u; j++) {
          bf = setBitLong(v++, bf);
        }
        int est = estCountLong(bf, 64);
        sum += est;
        double err = u - est;
        sumErr += err;
        sumErrSq += err * err;
      }
      double mean = sum /trials;
      double meanErr = sumErr/trials;
      double varErr = (sumErrSq - meanErr * sumErr/trials)/(trials -1);
      double relErr = Math.sqrt(varErr)/u;
      double bias = mean/u - 1.0;
      String line = String.format("%d\t%.2f%%\t%.2f%%", u, bias*100, relErr*100);
      println(line);
    }
  }

  private static final int estCountInt(int bf, int bfBits) {
    int bitCount = Integer.bitCount(bf);
    double s = bfBits;
    return (int) -(s * Math.log(1.0 - bitCount / s));
  }

  private static final int estCountLong(long bf, int bfBits) {
    int bitCount = Long.bitCount(bf);
    double s = bfBits;
    return (int) -(s * Math.log(1.0 - bitCount / s));
  }

  private static final int setBitInt(long v, int bf) {
    long [] key = {v};
    long h = MurmurHash3.hash(key, 0L)[0] >>> 1;
    int bit = (int)(h % 32);
    int mask = 1 << bit;
    return bf |= mask;
  }

  private static final long setBitLong(long v, long bf) {
    long [] key = {v};
    long h = MurmurHash3.hash(key, 0L)[0] >>> 1;
    int bit = (int)(h % 64);
    long mask = 1L << bit;
    return bf |= mask;
  }

  static final void printLongBytes(byte[] arr) {
      println(String.format("%d, %d, %d, %d, %d, %d, %d, %d",
          arr[0], arr[1], arr[2], arr[3], arr[4], arr[5], arr[6], arr[7]));
  }

  static void println(String s) { System.out.println(s); }

  public static void main(String[] args) {
    BloomFilterTest test = new BloomFilterTest();
    test.testForLong();
  }

}
