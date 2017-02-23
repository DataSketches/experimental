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

public class RandomSketchesAndDatumGenerator {
  private static long base = 0L;

  public static Sketch generateRandomSketch(int k) {
    UpdateSketch usk = UpdateSketch.builder().build(k);
    long u = 2 * k;
    for (long i = 0; i < u; i++) {
      usk.update(i + base);
    }
    base += u;
    return usk;
  }

  public static Memory generateRandomMemory(int k) {
    int bytes = Sketches.getMaxUpdateSketchBytes(k);
    byte[] arr = new byte[bytes];
    NativeMemory mem = new NativeMemory(arr);
    UpdateSketch usk = UpdateSketch.builder().initMemory(mem).build(k);
    long u = 2 * k;
    for (long i = 0; i < u; i++) {
      usk.update(i + base);
    }
    base += u;
    return mem;
  }

  public static long generateRandomLongDatum(int k) {
    // TODO - implement
    return 0;
  }

  public static long[] generateRandomLongsDatum(int k) {
    // TODO - implement
    return new long[0];
  }

  public static byte generateRandomByteDatum(int k) {
    // TODO - implement
    return 0;
  }

  public static byte[] generateRandomBytesDatum(int k) {
    // TODO - implement
    return new byte[0];
  }

  public static char generateRandomCharDatum(int k) {
    // TODO - implement
    return 0;
  }

  public static char[] generateRandomCharsDatum(int k) {
    // TODO - implement
    return new char[0];
  }

  public static int generateRandomIntDatum(int k) {
    // TODO - implement
    return 0;
  }

  public static int[] generateRandomIntsDatum(int k) {
    // TODO - implement
    return new int[0];
  }

  public static short generateRandomShortDatum(int k) {
    // TODO - implement
    return 0;
  }

  public static double generateRandomDoubleDatum(int k) {
    // TODO - implement
    return 0;
  }

  public static float generateRandomFloatDatum(int k) {
    // TODO - implement
    return 0;
  }
}
