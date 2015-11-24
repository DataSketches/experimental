/*
 * Copyright 2015, Yahoo! Inc.
 * Licensed under the terms of the Apache License 2.0. See LICENSE file at the project root for terms.
 */
package com.yahoo.sketches.quantiles;

public class Space {
  
  private static double lg(double x) {
    return ( Math.log(x)) / (Math.log(2.0) );
  }
  
  /**
   * This is the upper bound size, in bytes, of a MergeableQuantileSketch single array data 
   * structure given <i>k</i> and <i>n</i>.
   * @param k determines the accuracy of the sketch and the size of the updatable data structure,
   * which is a function of k. 
   * @param n The number of elements in the input stream
   * @return the upper bound size of the data structure in bytes
   */
  public static long spaceFormula(long k, long n) {
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
  
  public static String spaceTable(long multiplier) {
    StringBuilder sb = new StringBuilder();
    sb.append("           |");
    for (int kpow = 4; kpow <= 10; kpow++) {
      long k = 1L << kpow;
      sb.append(String.format("\t%d", k));
    }
    sb.append("\n");
    sb.append("-------------------------------------------------------------------------\n");
    for (int npow = 2; npow <= 32; npow++) {
      long n = ((long) 1) << npow;
      sb.append(String.format("%10d |", n));
      for (int kpow = 4; kpow <= 10; kpow++) {
        long k = ((long) 1) << kpow;
        sb.append(String.format("\t%d", (multiplier * (spaceFormula(k, n)))));
      }
      sb.append("\n");
    }
    return sb.toString();
  }
  
  public static void main(String[] args) {
    System.out.println(spaceTable(8));
  }
}
