package com.yahoo.sketches.tuple;

public class HeapArrayOfDoublesSketchIterator implements ArrayOfDoublesSketchIterator {

  private long[] keys_;
  private double[][] values_;
  private int i_;

  HeapArrayOfDoublesSketchIterator(long[] keys, double[][] values) {
    keys_ = keys;
    values_ = values;
    i_ = -1;
  }

  @Override
  public boolean next() {
    i_++;
    while (i_ < keys_.length) {
      if (keys_[i_] != 0) return true;
      i_++;
    }
    return false;
  }

  @Override
  public long getKey() {
    return keys_[i_];
  }

  @Override
  public double[] getValues() {
    return values_[i_];
  }

}
