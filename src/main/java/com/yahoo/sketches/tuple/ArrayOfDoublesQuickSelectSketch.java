/*
 * Copyright 2015, Yahoo! Inc.
 * Licensed under the terms of the Apache License 2.0. See LICENSE file at the project root for terms.
 */
package com.yahoo.sketches.tuple;

/**
 * Top level class for hash table based implementation, which uses quick select algorithm
 * when the time comes to rebuild the hash table and throw away some entries.
 */

import com.yahoo.sketches.QuickSelect;

public abstract class ArrayOfDoublesQuickSelectSketch extends UpdatableArrayOfDoublesSketch {

  public static final byte serialVersionUID = 1;

  static final int SERIAL_VERSION_BYTE = 0;
  static final int SKETCH_TYPE_BYTE = 1;
  static final int FLAGS_BYTE = 2;
  static final int LG_NOM_ENTRIES_BYTE = 3;
  static final int LG_CUR_CAPACITY_BYTE = 4;
  static final int LG_RESIZE_FACTOR_BYTE = 5;
  static final int NUM_VALUES_BYTE = 6;
  static final int RETAINED_ENTRIES_INT = 7;
  static final int THETA_LONG = 11;
  static final int SAMPLING_P_FLOAT = 19;
  static final int ENTRIES_START = 23;

  protected static final int MIN_NOM_ENTRIES = 32;
  protected static final int DEFAULT_LG_RESIZE_FACTOR = 3;
  protected static final double REBUILD_RATIO_AT_RESIZE = 0.5;
  protected static final double REBUILD_RATIO_AT_TARGET_SIZE = 15.0 / 16.0;

  // these can be derived from other things, but are kept here for performance
  protected int rebuildThreshold_;
  protected int mask_;

  // 7 bits, last zero to make even
  private static final int STRIDE_MASK = 0xfe;
  protected static final int getStride(long key) {
    // make odd and independent of index assuming that lower 32 bits are used for index
    return ((int) ((key >> 32) & STRIDE_MASK)) + 1;
  }

  protected abstract void updateValues(int index, double[] values);
  protected abstract void setNotEmpty();
  protected abstract boolean isInSamplingMode();
  protected abstract int getResizeFactor();
  protected abstract int getCurrentCapacity();
  protected abstract void rebuild(int newCapacity);
  protected abstract long getKey(int index);
  protected abstract void setKey(int index, long key);
  protected abstract void setValues(int index, double[] values, boolean isCopyRequired);
  protected abstract void incrementCount();
  protected abstract void setThetaLong(long theta);

  /*
   * Rebuilds reducing the actual number of entries to the nominal number of entries if needed
   */
  public void trim() {
    if (getRetainedEntries() > getNominalEntries()) {
      updateTheta();
      rebuild();
    }
  }

  // non-public methods below

  // this is a special back door insert for merging
  // not sufficient by itself without keeping track of theta of another sketch
  void merge(long key, double[] values) {
    setNotEmpty();
    if (key < theta_) {
      int countBefore = getRetainedEntries();
      int index = findOrInsert(key);
      if (getRetainedEntries() == countBefore) {
        updateValues(index, values);
      } else {
        setValues(index, values, true);
      }
      rebuildIfNeeded();
    }
  }

  protected void rebuildIfNeeded() {
    if (getRetainedEntries() < rebuildThreshold_) return;
    if (getCurrentCapacity() > getNominalEntries()) {
      updateTheta();
      rebuild();
    } else {
      rebuild(getCurrentCapacity() * getResizeFactor());
    }
  }
  
  protected void rebuild() {
    rebuild(getCurrentCapacity());
  }

  protected void insert(long key, double[] values) {
    int index = (int) key & mask_;
    while (getKey(index) != 0) {
      index = (index + getStride(key)) & mask_;
    }
    setKey(index, key);
    setValues(index, values, false);
    incrementCount();
  }

  protected void setRebuildThreshold() {
    if (getCurrentCapacity() > getNominalEntries()) {
      rebuildThreshold_ = (int) (getCurrentCapacity() * REBUILD_RATIO_AT_TARGET_SIZE);
    } else {
      rebuildThreshold_ = (int) (getCurrentCapacity() * REBUILD_RATIO_AT_RESIZE);
    }
  }

  @Override
  protected void insertOrIgnore(long key, double[] values) {
    if (values.length != getNumValues()) throw new IllegalArgumentException("input array of values must have " + getNumValues() + " elements, but has " + values.length);
    setNotEmpty();
    if (key == 0 || key >= theta_) return;
    int countBefore = getRetainedEntries();
    int index = findOrInsert(key);
    if (getRetainedEntries() > countBefore) {
      setValues(index, values, true);
    } else {
      updateValues(index, values);
    }
    rebuildIfNeeded();
  }

  protected int findOrInsert(long key) {
    int index = (int) key & mask_;
    long keyAtIndex;
    while ((keyAtIndex = getKey(index)) != 0) {
      if (keyAtIndex == key) return index;
      index = (index + getStride(key)) & mask_;
    }
    setKey(index, key);
    incrementCount();
    return index;
  }

  protected void updateTheta() {
    long[] keys = new long[getRetainedEntries()];
    int i = 0;
    for (int j = 0; j < getCurrentCapacity(); j++) {
      long key = getKey(j); 
      if (key != 0) keys[i++] = key;
    }
    setThetaLong(QuickSelect.select(keys, 0, getRetainedEntries() - 1, getNominalEntries()));
  }
}
