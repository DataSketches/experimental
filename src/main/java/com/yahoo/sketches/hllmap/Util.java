/*
 * Copyright 2016, Yahoo! Inc. Licensed under the terms of the
 * Apache License 2.0. See LICENSE file at the project root for terms.
 */

package com.yahoo.sketches.hllmap;

import java.math.BigInteger;

public class Util {

  /**
   * Returns <tt>true</tt> if the two specified sub-arrays of bytes are <i>equal</i> to one another.
   * Two arrays are considered equal if all corresponding pairs of elements in the two arrays are
   * equal. In other words, two arrays are equal if and only if they contain the same elements
   * in the same order.
   *
   * @param a one sub-array to be tested for equality
   * @param offsetA the offset in bytes of the start of sub-array <i>a</i>.
   * @param b the other sub-array to be tested for equality
   * @param offsetB the offset in bytes of the start of sub-array <i>b</i>.
   * @param length the length in bytes of the two sub-arrays.
   * @return <tt>true</tt> if the two sub-arrays are equal
   */
  public static boolean equals(byte[] a, int offsetA, byte[] b, int offsetB, int length) {
      if (a==b)
          return true;
      if (a==null || b==null)
          return false;

      for (int i=0; i<length; i++)
          if (a[i + offsetA] != b[i + offsetB])
              return false;

      return true;
  }

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
   * Returns the next prime number that is greater than or equal to the given target. There will be
   * no prime numbers less than the returned prime number that are greater than the given target.
   * @param target the starting value to begin the search for the next prime
   * @return the next prime number that is greater than or equal to the given target.
   */
  static final int nextPrime(int target) {
    //We may want to replace this with a table lookup for our application.
    return BigInteger.valueOf(target).nextProbablePrime().intValueExact();
  }

  static final byte[] getBytes(byte[] byteArr, int sizeBytes, int index) {
    byte[] key = new byte[sizeBytes];
    for (int i = 0; i < sizeBytes; i++) {
      key[i] = byteArr[index * sizeBytes];
    }
    return key;
  }

  static final void putBytes(byte[] byteArr, int sizeBytes, int index, byte[] value) {
    for (int i = 0; i < sizeBytes; i++) {
      byteArr[index * sizeBytes] = value[i];
    }
  }

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

  static void println(String s) { System.out.println(s); }
}
