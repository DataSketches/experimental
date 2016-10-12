/*
 * Copyright 2016, Yahoo! Inc. Licensed under the terms of the
 * Apache License 2.0. See LICENSE file at the project root for terms.
 */

package com.yahoo.sketches.hllmap;

import java.math.BigInteger;

import com.yahoo.sketches.hash.MurmurHash3;

public final class Util {

  /**
   * Returns the HLL array index and value as a 16-bit coupon given the identifier to be hashed
   * and k.
   * @param identifier the given identifier
   * @param k the size of the HLL array and cannot exceed 1024
   * @return the HLL array index and value
   */
  static final int coupon16(byte[] identifier, int k) {
    long[] hash = MurmurHash3.hash(identifier, 0L);
    int hllIdx = (int) (((hash[0] >>> 1) % k) & 0X3FF); //hash[0] for 10-bit address
    int lz = Long.numberOfLeadingZeros(hash[1]);
    int value = ((lz > 62)? 62 : lz) + 1;
    return (value << 10) | hllIdx;
  }

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

  /**
   * Computes the inverse integer power of 2: 1/(2^e) = 2^(-e).
   * @param e a positive value between 0 and 1023 inclusive
   * @return  the inverse integer power of 2: 1/(2^e) = 2^(-e)
   */
  public static double invPow2(int e) {
    assert (e | (1024 - e - 1)) >= 0 : "e cannot be negative or greater than 1023: " + e;
    return Double.longBitsToDouble((0x3ffL - e) << 52);
  }

  static void println(String s) { System.out.println(s); }
}
