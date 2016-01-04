/*
 * Copyright 2015, Yahoo! Inc.
 * Licensed under the terms of the Apache License 2.0. See LICENSE file at the project root for terms.
 */
package com.yahoo.sketches.quantiles;

import static com.yahoo.sketches.quantiles.Util.lg;
import static com.yahoo.sketches.Util.*;

/**
 * Utility functions for computing space consumed by the MergeableQuantileSketch.
 * 
 * @author Kevin Lang
 */
public final class Space {
  
  private Space() {}
  
  /**
   * This is the upper bound element space of an updatable QuantileSketch data structure when 
   * configured as a single array and given <i>k</i> and <i>n</i>.
   * 
   * @param k buffer size in elements. This determines the accuracy of the sketch and the 
   * size of the updatable data structure, which is a function of k.
   * 
   * @param n The number of elements in the input stream
   * @return the maximum retained elements of a QuantileSketch
   */
  public static long upperBoundElementSpace(long k, long n) {
    if (n < 2 * k) {
      double ceilLgN = Math.ceil(lg(n));
      return (long) (Math.pow(2.0, ceilLgN) );
    }
    else {
      long nOver2K = n / (2 * k);
      double lgNover2K = lg(nOver2K);
      long floorLgNover2K = (long) Math.floor(lgNover2K);
      return k * (2 + 1 + floorLgNover2K);
    }
  }
  
  /**
   * Returns a pretty print string of a table of the maximum sizes of a QuantileSketch 
   * data structure configured as a single array over a range of <i>n</i> and <i>k</i>. 
   * @param elementSizeBytes the given element size in bytes
   * @return a pretty print string of a table of the maximum sizes of a QuantileSketch
   */
  public static String spaceTable(long elementSizeBytes) {
    StringBuilder sb = new StringBuilder();
    sb.append("Quantiles Size Table:").append(LS);
    sb.append("      N : K => |");
    for (int kpow = 4; kpow <= 10; kpow++) {
      long k = 1L << kpow;
      sb.append(String.format("%,8d", k));
    }
    sb.append(LS);
    sb.append("-------------------------------------------------------------------------\n");
    for (int npow = 2; npow <= 32; npow++) {
      long n = 1L << npow;
      sb.append(String.format("%,14d |", n));
      for (int kpow = 4; kpow <= 10; kpow++) {
        long k = ((long) 1) << kpow;
        sb.append(String.format("%,8d", (elementSizeBytes * (upperBoundElementSpace(k, n)))));
      }
      sb.append(LS);
    }
    return sb.toString();
  }
  
  static void println(String s) { System.out.println(s); }
  
  /**
   * Pretty prints a table of the maximum sizes of a QuantileSketch 
   * data structure configured as a single array over a range of <i>n</i> and <i>k</i> and given
   * an element size of 8 bytes.
   * @param args Not used.
   */
  public static void main(String[] args) {
    println(spaceTable(8));
    long n = 2047;
    int k = 1024;
    int bbCnt = HeapQuantilesSketch.computeBaseBufferCount(k, n);
    int maxLevels = HeapQuantilesSketch.computeNumLevelsNeeded(k, n);
    int bytes = bbCnt*8 + maxLevels*k*8;
    println("N: "+n);
    println("bbCnt: "+bbCnt);
    println("maxLvs: "+maxLevels);
    println("bytes: "+bytes);
  }
}
