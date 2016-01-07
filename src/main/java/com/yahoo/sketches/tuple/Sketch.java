/*
 * Copyright 2015, Yahoo! Inc.
 * Licensed under the terms of the Apache License 2.0. See LICENSE file at the project root for terms.
 */
package com.yahoo.sketches.tuple;

import java.nio.ByteBuffer;

public abstract class Sketch<S extends Summary> {

  protected long[] keys_;
  protected S[] summaries_;
  protected long theta_;
  protected boolean isEmpty_ = true;

  public double getEstimate() {
    if (!isEstimationMode()) return getRetainedEntries();
    return getRetainedEntries() / getTheta();
  }

  public double getUpperBound(int numStdDev) {
    if (!isEstimationMode()) return getRetainedEntries();
    return Util.upperBound(getEstimate(), getTheta(), numStdDev);
  }

  public double getLowerBound(int numStdDev) {
    if (!isEstimationMode()) return getRetainedEntries();
    return Util.lowerBound(getEstimate(), getTheta(), numStdDev);
  }

  public boolean isEmpty() {
    return isEmpty_;
  }

  public boolean isEstimationMode() {
    return ((theta_ < Long.MAX_VALUE) && !isEmpty());
  }

  public abstract int getRetainedEntries();

  public double getTheta() {
    return theta_ / (double) Long.MAX_VALUE;
  }

  public abstract S[] getSummaries();

  public abstract ByteBuffer serializeToByteBuffer();
  // For deserialization there is a convention to have a constructor which takes ByteBuffer

  long getThetaLong() {
    return theta_;
  }

}
