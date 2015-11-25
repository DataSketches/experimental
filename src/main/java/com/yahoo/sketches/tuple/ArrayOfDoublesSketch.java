package com.yahoo.sketches.tuple;

import java.nio.ByteBuffer;

public abstract class ArrayOfDoublesSketch {

  protected long[] keys_;
  protected double[][] values_;
  protected long theta_;

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

  public abstract boolean isEmpty();

  public boolean isEstimationMode() {
    return ((theta_ < Long.MAX_VALUE) && !isEmpty());
  }

  public abstract int getRetainedEntries();

  public double getTheta() {
    return theta_ / (double) Long.MAX_VALUE;
  }

  public abstract double[][] getValues();

  public abstract ByteBuffer serializeToByteBuffer();
  // For deserialization there is a convention to have a constructor which takes ByteBuffer

  long getThetaLong() {
    return theta_;
  }

}
