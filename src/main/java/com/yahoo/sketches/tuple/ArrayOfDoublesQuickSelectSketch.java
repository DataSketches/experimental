/*
 * Copyright 2015, Yahoo! Inc.
 * Licensed under the terms of the Apache License 2.0. See LICENSE file at the project root for terms.
 */
package com.yahoo.sketches.tuple;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static com.yahoo.sketches.Util.ceilingPowerOf2;

import com.yahoo.sketches.QuickSelect;

public class ArrayOfDoublesQuickSelectSketch extends ArrayOfDoublesSketch {

  public static final byte serialVersionUID = 1;

  private static final int MIN_NOM_ENTRIES = 32;
  private static final int DEFAULT_LG_RESIZE_RATIO = 3;
  private static final double REBUILD_RATIO_AT_RESIZE = 0.5;
  private static final double REBUILD_RATIO_AT_TARGET_SIZE = 15.0 / 16.0;
  private int nomEntries_;
  private int lgResizeRatio_;
  private int count_;
  private float samplingProbability_;
  private boolean isEmpty_ = true;
  private int rebuildThreshold_;
  protected int numValues_;
  
  /**
   * This is to create an instance of a QuickSelectSketch with default resize factor.
   * @param nomEntries Nominal number of entries. Forced to the nearest power of 2 greater than given value.
   * @param numValues number of double values to keep for each key
   */
  ArrayOfDoublesQuickSelectSketch(int nomEntries, int numValues) {
    this(nomEntries, DEFAULT_LG_RESIZE_RATIO, numValues);
  }

  /**
   * This is to create an instance of a QuickSelectSketch with default resize factor and a given sampling probability.
   * @param nomEntries Nominal number of entries. Forced to the nearest power of 2 greater than given value.
   * @param samplingProbability <a href="{@docRoot}/resources/dictionary.html#p">See Sampling Probability, <i>p</i></a>
   * @param numValues number of double values to keep for each key
   */
  ArrayOfDoublesQuickSelectSketch(int nomEntries, float samplingProbability, int numValues) {
    this(nomEntries, DEFAULT_LG_RESIZE_RATIO, samplingProbability, numValues);
  }

  /**
   * This is to create an instance of a QuickSelectSketch with custom resize factor
   * @param nomEntries Nominal number of entries. Forced to the nearest power of 2 greater than given value.
   * @param lgResizeRatio log2(resizeRatio) - value from 0 to 3:
   * 0 - no resizing (max size allocated),
   * 1 - double internal hash table each time it reaches a threshold
   * 2 - grow four times
   * 3 - grow eight times (default)
   * @param numValues number of double values to keep for each key
   */
  ArrayOfDoublesQuickSelectSketch(int nomEntries, int lgResizeRatio, int numValues) {
    this(nomEntries, lgResizeRatio, 1f, numValues);
  }

  /**
   * This is to create an instance of a QuickSelectSketch with custom resize factor and sampling probability
   * @param nomEntries Nominal number of entries. Forced to the nearest power of 2 greater than given value.
   * @param lgResizeRatio log2(resizeRatio) - value from 0 to 3:
   * 0 - no resizing (max size allocated),
   * 1 - double internal hash table each time it reaches a threshold
   * 2 - grow four times
   * 3 - grow eight times (default)
   * @param samplingProbability
   * @param numValues number of double values to keep for each key
   */
  ArrayOfDoublesQuickSelectSketch(int nomEntries, int lgResizeRatio, float samplingProbability, int numValues) {
    nomEntries_ = ceilingPowerOf2(nomEntries);
    lgResizeRatio_ = lgResizeRatio;
    samplingProbability_ = samplingProbability;
    numValues_ = numValues;
    theta_ = (long) (Long.MAX_VALUE * (double) samplingProbability);
    int startingSize = 1 << Util.startingSubMultiple(
      Integer.numberOfTrailingZeros(ceilingPowerOf2(nomEntries) * 2), // target table size is twice the number of nominal entries
      lgResizeRatio,
      Integer.numberOfTrailingZeros(MIN_NOM_ENTRIES)
    );
    keys_ = new long[startingSize];
    values_ = new double[startingSize][];
    setRebuildThreshold();
  }

  /**
   * This is to create an instance of a QuickSelectSketch given a serialized form
   * @param buffer ByteBuffer with serialized QukckSelectSketch
   */
  public ArrayOfDoublesQuickSelectSketch(ByteBuffer buffer) {
    byte version = buffer.get();
    if (version != serialVersionUID) throw new RuntimeException("Serial version mismatch. Expected: " + serialVersionUID + ", actual: " + version);
    SerializerDeserializer.validateType(buffer, SerializerDeserializer.SketchType.ArrayOfDoublesQuickSelectSketch);
    byte flags = buffer.get();
    boolean isBigEndian = (flags & (1 << Flags.IS_BIG_ENDIAN.ordinal())) > 0;
    if (isBigEndian ^ buffer.order().equals(ByteOrder.BIG_ENDIAN)) throw new RuntimeException("Byte order mismatch");
    nomEntries_ = 1 << buffer.get();
    int currentCapacity = 1 << buffer.get();
    lgResizeRatio_ = buffer.get();
    numValues_ = buffer.get();
    int count = 0;
    Long thetaLong = null;
    boolean hasEntries = (flags & (1 << Flags.HAS_ENTRIES.ordinal())) > 0;
    if (hasEntries) {
      count = buffer.getInt();
      thetaLong = buffer.getLong();
    }
    boolean isInSamplingMode = (flags & (1 << Flags.IS_IN_SAMPLING_MODE.ordinal())) > 0;
    samplingProbability_ = 1f;
    if (isInSamplingMode) samplingProbability_ = buffer.getFloat();
    theta_ = (long) (Long.MAX_VALUE * (double) samplingProbability_);
    keys_ = new long[currentCapacity];
    values_ = new double[currentCapacity][];
    for (int i = 0; i < count; i++) {
      long key = buffer.getLong();
      double[] values = new double[numValues_];
      for (int j = 0; j < numValues_; j++) values[j] = buffer.getDouble();
      insert(key, values);
    }
    if (thetaLong != null) setThetaLong(thetaLong);
    setIsEmpty((flags & (1 << Flags.IS_EMPTY.ordinal())) > 0);
    setRebuildThreshold();
  }

  @Override
  public double[][] getValues() {
    double[][] values = new double[count_][];
    int i = 0;
    for (int j = 0; j < values_.length; j++) {
      if (values_[j] != null) values[i++] = values_[j].clone();
    }
    return values;
  }

  @Override
  public boolean isEmpty() {
    return isEmpty_;
  }

  @Override
  public int getRetainedEntries() {
    return count_;
  }

  /*
   * Rebuilds reducing the actual number of entries to the nominal number of entries if needed
   */
  public void trim() {
    if (count_ > nomEntries_) {
      updateTheta();
      rebuild(keys_.length);
    }
  }

  public ArrayOfDoublesCompactSketch compact() {
    long[] keys = null;
    double[][] values = null;
    if (!isEmpty()) {
      trim();
      keys = new long[count_];
      values = new double[count_][];
      int i = 0;
      for (int j = 0; j < values_.length; j++) {
        if (values_[j] != null) {
          keys[i] = keys_[j];
          values[i++] = values_[j].clone();
        }
      }
    }
    return new ArrayOfDoublesCompactSketch(keys, values, theta_);
  }

  private enum Flags { IS_BIG_ENDIAN, IS_IN_SAMPLING_MODE, IS_EMPTY, HAS_ENTRIES }

  /**
   * @return serialized representation of the sketch
   */
  @Override
  public ByteBuffer serializeToByteBuffer() {
    int sizeBytes = 
        1 // version
      + 1 // sketch type
      + 1 // flags
      + 1 // log2(nomEntries)
      + 1 // log2(currentCapacity)
      + 1 // log2(resizeRatio)
      + 1; // numValues
    if (count_ > 0) {
      sizeBytes +=
          4 // count
        + 8; // theta
    }
    if (isInSamplingMode()) sizeBytes += 4; // samplingProbability
    sizeBytes += 8 * count_; // keys
    sizeBytes += 8 * count_ * numValues_; // values
    ByteBuffer buffer = ByteBuffer.allocate(sizeBytes).order(ByteOrder.nativeOrder());
    buffer.put(serialVersionUID);
    buffer.put((byte)SerializerDeserializer.SketchType.ArrayOfDoublesQuickSelectSketch.ordinal());
    boolean isBigEndian = buffer.order().equals(ByteOrder.BIG_ENDIAN);
    buffer.put((byte)(
      ((isBigEndian ? 1 : 0) << Flags.IS_BIG_ENDIAN.ordinal()) |
      ((isInSamplingMode() ? 1 : 0) << Flags.IS_IN_SAMPLING_MODE.ordinal()) |
      ((isEmpty_ ? 1 : 0) << Flags.IS_EMPTY.ordinal()) |
      ((count_ > 0 ? 1 : 0) << Flags.HAS_ENTRIES.ordinal())
    ));
    buffer.put((byte)Integer.numberOfTrailingZeros(nomEntries_));
    buffer.put((byte)Integer.numberOfTrailingZeros(keys_.length));
    buffer.put((byte)lgResizeRatio_);
    buffer.put((byte)numValues_);
    if (count_ > 0) {
      buffer.putInt(count_);
      buffer.putLong(theta_);
    }
    if (samplingProbability_ < 1f) buffer.putFloat(samplingProbability_);
    if (count_ > 0) {
      for (int i = 0; i < keys_.length; i++) {
        if (values_[i] != null) {
          buffer.putLong(keys_[i]);
          for (double value: values_[i]) buffer.putDouble(value);
        }
      }
    }
    return buffer;
  }

  // non-public methods below

  // this is a special back door insert for merging
  // not sufficient by itself without keeping track of theta of another sketch
  void merge(long key, double[] values) {
    isEmpty_ = false;
    if (key < theta_) {
      int countBefore = count_;
      int index = findOrInsert(key);
      if (count_ == countBefore) {
        for (int i = 0; i < numValues_; i++) values_[index][i] += values[i];
      } else {
        values_[index] = values.clone();
      }
      rebuildIfNeeded();
    }
  }

  boolean isInSamplingMode() {
    return samplingProbability_ < 1f;
  }

  void setThetaLong(long theta) {
    this.theta_ = theta;
  }

  void setIsEmpty(boolean isEmpty) {
    this.isEmpty_ = isEmpty;
  }

  // assumes that table.length is power of 2
  private int getIndex(long key) {
    return (int) (key & (keys_.length - 1));
  }

  // 7 bits, last zero to make even
  private static final int STRIDE_MASK = 0xfe;
  private static final int getStride(long key) {
    // make odd and independent of index assuming that lower 32 bits are used for index
    return ((int) ((key >> 32) & STRIDE_MASK)) + 1;
  }

  int findOrInsert(long key) {
    int index = getIndex(key);
    while (values_[index] != null) {
      if (keys_[index] == key) return index;
      index = (index + getStride(key)) & (keys_.length - 1);
    }
    keys_[index] = key;
    count_++;
    return index;
  }

  boolean rebuildIfNeeded() {
    if (count_ < rebuildThreshold_) return false;
    if (keys_.length > nomEntries_) {
      updateTheta();
      rebuild();
    } else {
      rebuild(keys_.length * (1 << lgResizeRatio_));
    }
    return true;
  }

  void rebuild() {
    rebuild(keys_.length);
  }

  // doesn't clone the values
  private void insert(long key, double[] values) {
    int index = getIndex(key);
    while (values_[index] != null) {
      index = (index + getStride(key)) & (keys_.length - 1);
    }
    keys_[index] = key;
    values_[index] = values;
    count_++;
  }

  private void updateTheta() {
    long[] keys = new long[count_];
    int i = 0;
    for (int j = 0; j < keys_.length; j++) {
      if (values_[j] != null) keys[i++] = keys_[j];
    }
    theta_ = QuickSelect.select(keys, 0, count_ - 1, nomEntries_);
  }

  private void rebuild(int newSize) {
    long[] oldKeys = keys_;
    double[][] oldValues = values_;
    keys_ = new long[newSize];
    values_ = new double[newSize][];
    count_ = 0;
    for (int i = 0; i < oldKeys.length; i++) {
      if (oldValues[i] != null && oldKeys[i] < theta_) insert(oldKeys[i], oldValues[i]);
    }
    setRebuildThreshold();
  }

  private void setRebuildThreshold() {
    if (keys_.length > nomEntries_) {
      rebuildThreshold_ = (int) (keys_.length * REBUILD_RATIO_AT_TARGET_SIZE);
    } else {
      rebuildThreshold_ = (int) (keys_.length * REBUILD_RATIO_AT_RESIZE);
    }
  }

}
