/*
 * Copyright 2016, Yahoo! Inc. Licensed under the terms of the Apache License 2.0. See LICENSE file
 * at the project root for terms.
 */
package com.yahoo.sketches.utils;

import com.yahoo.memory.Memory;
import com.yahoo.memory.NativeMemory;
import com.yahoo.sketches.theta.Sketch;
import com.yahoo.sketches.theta.Sketches;
import com.yahoo.sketches.theta.UpdateSketch;

import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;

public class RandomSketchesAndDatumGenerator {
  private static AtomicLong base = new AtomicLong(0L);
  private static Random random = new Random();

  private static final int ARRAY_SIZE = 3;

  public static Sketch generateRandomSketch(int k) {
    UpdateSketch usk = UpdateSketch.builder().build(k);
    buildUpdateSketch(k, usk);
    return usk;
  }

  public static Memory generateRandomMemory(int k) {
    int bytes = Sketches.getMaxUpdateSketchBytes(k);
    byte[] arr = new byte[bytes];
    NativeMemory mem = new NativeMemory(arr);
    UpdateSketch usk = UpdateSketch.builder().initMemory(mem).build(k);
    buildUpdateSketch(k, usk);
    return mem;
  }

  private static void buildUpdateSketch(int k, UpdateSketch usk) {
    long u = 2 * k;
    for (long i = 0; i < u; i++) {
      usk.update(i + base.get());
    }
    base.addAndGet(u);
  }

  public static long generateRandomLongDatum() {
    return random.nextLong();
  }

  public static long[] generateRandomLongsDatum() {
    return random.longs(ARRAY_SIZE).toArray();
  }

  public static byte generateRandomByteDatum() {
    return (byte) random.nextLong();
  }

  public static byte[] generateRandomBytesDatum() {
    byte[] res = new byte[ARRAY_SIZE];
    for (int i = 0; i < ARRAY_SIZE; i++) {
      res[i] = generateRandomByteDatum();
    }
    return res;
  }

  public static char generateRandomCharDatum() {
    return (char) random.nextLong();
  }

  public static char[] generateRandomCharsDatum() {
    char[] res = new char[ARRAY_SIZE];
    for (int i = 0; i < ARRAY_SIZE; i++) {
      res[i] = generateRandomCharDatum();
    }
    return res;
  }

  public static int generateRandomIntDatum() {
    return (int) random.nextLong();
  }

  public static int[] generateRandomIntsDatum() {
    return random.ints(ARRAY_SIZE).toArray();
  }

  public static short generateRandomShortDatum() {
    return (short) random.nextLong();
  }

  public static double generateRandomDoubleDatum() {
    return random.nextDouble();
  }

  public static float generateRandomFloatDatum() {
    return random.nextFloat();
  }
}
