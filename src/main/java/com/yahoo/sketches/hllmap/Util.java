/*
 * Copyright 2016, Yahoo! Inc. Licensed under the terms of the
 * Apache License 2.0. See LICENSE file at the project root for terms.
 */

package com.yahoo.sketches.hllmap;

import java.math.BigInteger;

import com.yahoo.sketches.SketchesArgumentException;

public final class Util {

  /**
   * Returns a string view of a byte array
   * @param arr the given byte array
   * @param signed set true if you want the byte values signed.
   * @param littleEndian set true if you want Little-Endian order
   * @param sep the separator string between bytes
   * @return a string view of a byte array
   */
  public static final String bytesToString(
      byte[] arr, boolean signed, boolean littleEndian, String sep) {
    StringBuilder sb = new StringBuilder();
    int mask = (signed) ? 0XFFFFFFFF : 0XFF;
    int arrLen = arr.length;
    if (littleEndian) {
      for (int i = 0; i < arrLen-1; i++) {
        sb.append(arr[i] & mask).append(sep);
      }
      sb.append(arr[arrLen-1] & mask);
    } else {
      for (int i = arrLen; i-- > 1; ) {
        sb.append(arr[i] & mask).append(sep);
      }
      sb.append(arr[0] & mask);
    }
    return sb.toString();
  }

  /**
   * Returns the next prime number that is greater than the given target. There will be
   * no prime numbers less than the returned prime number that are greater than the given target.
   * @param target the starting value to begin the search for the next prime
   * @return the next prime number that is greater than or equal to the given target.
   */
  static final int nextPrime(int target) {
    return BigInteger.valueOf(target).nextProbablePrime().intValueExact();
  }

  static final void checkK(int k) {
    if (!com.yahoo.sketches.Util.isPowerOf2(k) || (k > 1024) || (k < 16)) {
      throw new SketchesArgumentException("K must be power of 2 and (16 <= k <= 1024): " + k);
    }
  }

  static final void checkGrowthFactor(float growthFactor) {
    if (growthFactor <= 1.0) {
      throw new SketchesArgumentException("growthFactor must be > 1.0: " + growthFactor);
    }
  }

  static final void checkTgtEntries(int tgtEntries) {
    if (tgtEntries < 16) {
      throw new SketchesArgumentException("tgtEntries must be >= 16");
    }
  }

  static final void checkKeySizeBytes(int keySizeBytes) {
    if (keySizeBytes < 4) {
      throw new SketchesArgumentException("KeySizeBytes must be >= 4: " + keySizeBytes);
    }
  }

  //TODO consolidate these
  static final boolean isBitOne(byte[] byteArr, int bitIndex) {
    int byteIdx = bitIndex / 8;
    int shift = bitIndex % 8;
    int v = byteArr[byteIdx];
    return (v & (1 << shift)) > 0;
  }

  static final boolean isBitZero(byte[] byteArr, int bitIndex) {
    int byteIdx = bitIndex / 8;
    int shift = bitIndex % 8;
    int v = byteArr[byteIdx];
    return (v & (1 << shift)) == 0;
  }

  static final void setBitToOne(byte[] byteArr, int bitIndex) {
    int byteIdx = bitIndex / 8;
    int shift = bitIndex % 8;
    int v = byteArr[byteIdx];
    byteArr[byteIdx] = (byte)(v | (1 << shift));
  }

  static final void setBitToZero(byte[] byteArr, int bitIndex) {
    int byteIdx = bitIndex / 8;
    int shift = bitIndex % 8;
    int v = byteArr[byteIdx];
    byteArr[byteIdx] = (byte)(v & ~(1 << shift));
  }

  //TODO move to sketches.Util eventually

  /**
   * Computes the inverse integer power of 2: 1/(2^e) = 2^(-e).
   * @param e a positive value between 0 and 1023 inclusive
   * @return  the inverse integer power of 2: 1/(2^e) = 2^(-e)
   */
  public static double invPow2(int e) {
    assert (e | (1024 - e - 1)) >= 0 : "e cannot be negative or greater than 1023: " + e;
    return Double.longBitsToDouble((1023L - e) << 52);
  }

  static String fmtLong(long value) {
    return String.format("%,d", value);
  }

  static String fmtDouble(double value) {
    return String.format("%,.3f", value);
  }

//  public static void main(String[] args) {
//    println(""+ nextPrime(13));
//  }

  static void println(String s) { System.out.println(s); }
}
