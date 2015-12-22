package com.yahoo.sketches.tuple;

public abstract class ArrayOfDoublesSketch {

  // The concept of being empty is about representing an empty set.
  // So a sketch can be non-empty, and have no entries.
  // For example, as a result of a sampling, when some data was presented to the sketch, but no entries were retained.
  static enum Flags { IS_BIG_ENDIAN, IS_IN_SAMPLING_MODE, IS_EMPTY, HAS_ENTRIES }
  static final int SIZE_OF_KEY_BYTES = 8;
  static final int SIZE_OF_VALUE_BYTES = 8;
  
  protected long theta_;
  protected int numValues_;
  protected boolean isEmpty_ = true;

  public abstract int getRetainedEntries();
  public abstract byte[] toByteArray();
  public abstract double[][] getValues();

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

  public int getNumValues() {
    return numValues_;
  }
  
  public boolean isEstimationMode() {
    return ((theta_ < Long.MAX_VALUE) && !isEmpty());
  }

  public double getTheta() {
    return theta_ / (double) Long.MAX_VALUE;
  }

  long getThetaLong() {
    return theta_;
  }

  abstract ArrayOfDoublesSketchIterator iterator();
}
