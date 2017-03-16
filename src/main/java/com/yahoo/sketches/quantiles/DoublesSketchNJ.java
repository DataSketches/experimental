/*
 * Copyright 2016, Yahoo! Inc. Licensed under the terms of the
 * Apache License 2.0. See LICENSE file at the project root for terms.
 */

package com.yahoo.sketches.quantiles;

@SuppressWarnings("unused")
public class DoublesSketchNJ {
  private static final int POOL_SIZE = 10;
  private int k_;
  private long n_;
  private int growthTrigger_;
  private int basePop_;
  private double[] baseBuf_ = null;
  private double[][] levels_ = null;

  /**
   * blah
   * @param k blah
   */
  public DoublesSketchNJ(final int k) {
    k_ = k;
    n_ = 0;
    growthTrigger_ = 1;
    basePop_ = 0;
  }

  public long getN() {
    return n_;
  }

  public int getK() {
    return k_;
  }

}
