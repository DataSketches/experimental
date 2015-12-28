/*
 * Copyright 2015, Yahoo! Inc.
 * Licensed under the terms of the Apache License 2.0. See LICENSE file at the project root for terms.
 */
package com.yahoo.sketches.tuple;

import java.nio.ByteOrder;

import static com.yahoo.sketches.Util.ceilingPowerOf2;
import com.yahoo.sketches.memory.Memory;
import com.yahoo.sketches.memory.NativeMemory;

public class HeapArrayOfDoublesQuickSelectSketch extends ArrayOfDoublesQuickSelectSketch {

  protected long[] keys_;
  protected double[][] values_;
  private int nomEntries_;
  private int lgResizeFactor_;
  private int count_;
  private float samplingProbability_;

  /**
   * This is to create an instance of a QuickSelectSketch with default resize factor.
   * @param nomEntries Nominal number of entries. Forced to the nearest power of 2 greater than given value.
   * @param numValues number of double values to keep for each key
   */
  HeapArrayOfDoublesQuickSelectSketch(int nomEntries, int numValues) {
    this(nomEntries, DEFAULT_LG_RESIZE_FACTOR, numValues);
  }

  /**
   * This is to create an instance of a QuickSelectSketch with default resize factor and a given sampling probability.
   * @param nomEntries Nominal number of entries. Forced to the nearest power of 2 greater than given value.
   * @param samplingProbability <a href="{@docRoot}/resources/dictionary.html#p">See Sampling Probability, <i>p</i></a>
   * @param numValues number of double values to keep for each key
   */
  HeapArrayOfDoublesQuickSelectSketch(int nomEntries, float samplingProbability, int numValues) {
    this(nomEntries, DEFAULT_LG_RESIZE_FACTOR, samplingProbability, numValues);
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
  HeapArrayOfDoublesQuickSelectSketch(int nomEntries, int lgResizeRatio, int numValues) {
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
  HeapArrayOfDoublesQuickSelectSketch(int nomEntries, int lgResizeRatio, float samplingProbability, int numValues) {
    nomEntries_ = ceilingPowerOf2(nomEntries);
    lgResizeFactor_ = lgResizeRatio;
    samplingProbability_ = samplingProbability;
    numValues_ = numValues;
    theta_ = (long) (Long.MAX_VALUE * (double) samplingProbability);
    int startingCapacity = 1 << Util.startingSubMultiple(
      Integer.numberOfTrailingZeros(ceilingPowerOf2(nomEntries) * 2), // target table size is twice the number of nominal entries
      lgResizeRatio,
      Integer.numberOfTrailingZeros(MIN_NOM_ENTRIES)
    );
    keys_ = new long[startingCapacity];
    values_ = new double[startingCapacity][];
    setRebuildThreshold();
    mask_ = startingCapacity - 1;
  }

  /**
   * This is to create an instance given a serialized form
   * @param mem <a href="{@docRoot}/resources/dictionary.html#mem">See Memory</a>
   */
  public HeapArrayOfDoublesQuickSelectSketch(Memory mem) {
    SerializerDeserializer.validateType(mem.getByte(SKETCH_TYPE_BYTE), SerializerDeserializer.SketchType.ArrayOfDoublesQuickSelectSketch);
    byte version = mem.getByte(SERIAL_VERSION_BYTE);
    if (version != serialVersionUID) throw new RuntimeException("Serial version mismatch. Expected: " + serialVersionUID + ", actual: " + version);
    byte flags = mem.getByte(FLAGS_BYTE);
    isEmpty_ = (flags & (1 << Flags.IS_EMPTY.ordinal())) > 0;
    nomEntries_ = 1 << mem.getByte(LG_NOM_ENTRIES_BYTE);
    int currentCapacity = 1 << mem.getByte(LG_CUR_CAPACITY_BYTE);
    lgResizeFactor_ = mem.getByte(LG_RESIZE_FACTOR_BYTE);
    numValues_ = mem.getByte(NUM_VALUES_BYTE);
    count_ = mem.getInt(RETAINED_ENTRIES_INT);
    theta_ = mem.getLong(THETA_LONG);
    samplingProbability_ = mem.getFloat(SAMPLING_P_FLOAT);
    keys_ = new long[currentCapacity];
    values_ = new double[currentCapacity][];
    mask_ = currentCapacity - 1;
    mem.getLongArray(ENTRIES_START, keys_, 0, currentCapacity);
    int offset = ENTRIES_START + SIZE_OF_KEY_BYTES * currentCapacity;
    int sizeOfValues = SIZE_OF_VALUE_BYTES * numValues_;
    for (int i = 0; i < currentCapacity; i++) {
      if (keys_[i] != 0) {
        double[] values = new double[numValues_];
        mem.getDoubleArray(offset, values, 0, numValues_);
        values_[i] = values;
      }
      offset += sizeOfValues;
    }
    setRebuildThreshold();
  }

  @Override
  public double[][] getValues() {
    double[][] values = new double[getRetainedEntries()][];
    if (!isEmpty()) {
      int i = 0;
      for (int j = 0; j < values_.length; j++) {
        if (values_[j] != null) values[i++] = values_[j].clone();
      }
    }
    return values;
  }

  @Override
  public int getRetainedEntries() {
    return count_;
  }

  @Override
  protected long getKey(int index) {
    return keys_[index];
  }

  @Override
  protected void setKey(int index, long key) {
    keys_[index] = key;
  }

  @Override
  protected void incrementCount() {
    count_++;
  }

  @Override
  public int getNominalEntries() {
    return nomEntries_;
  }

  /**
   * @return serialized representation of the sketch
   */
  @Override
  public byte[] toByteArray() {
    int sizeBytes = ENTRIES_START + (SIZE_OF_KEY_BYTES + SIZE_OF_VALUE_BYTES * numValues_) * getCurrentCapacity();
    byte[] byteArray = new byte[sizeBytes];
    Memory mem = new NativeMemory(byteArray); // wrap the byte array to use the putX methods
    mem.putByte(SERIAL_VERSION_BYTE, serialVersionUID);
    mem.putByte(SKETCH_TYPE_BYTE, (byte)SerializerDeserializer.SketchType.ArrayOfDoublesQuickSelectSketch.ordinal());
    boolean isBigEndian = ByteOrder.nativeOrder().equals(ByteOrder.BIG_ENDIAN);
    mem.putByte(FLAGS_BYTE, (byte)(
      ((isBigEndian ? 1 : 0) << Flags.IS_BIG_ENDIAN.ordinal()) |
      ((isInSamplingMode() ? 1 : 0) << Flags.IS_IN_SAMPLING_MODE.ordinal()) |
      ((isEmpty_ ? 1 : 0) << Flags.IS_EMPTY.ordinal()) |
      ((count_ > 0 ? 1 : 0) << Flags.HAS_ENTRIES.ordinal())
    ));
    mem.putByte(LG_NOM_ENTRIES_BYTE, (byte)Integer.numberOfTrailingZeros(nomEntries_));
    mem.putByte(LG_CUR_CAPACITY_BYTE, (byte)Integer.numberOfTrailingZeros(keys_.length));
    mem.putByte(LG_RESIZE_FACTOR_BYTE, (byte)lgResizeFactor_);
    mem.putByte(NUM_VALUES_BYTE, (byte)numValues_);
    mem.putInt(RETAINED_ENTRIES_INT, count_);
    mem.putLong(THETA_LONG, theta_);
    mem.putFloat(SAMPLING_P_FLOAT, samplingProbability_);
    mem.putLongArray(ENTRIES_START, keys_, 0, keys_.length);
    int offset = ENTRIES_START + SIZE_OF_KEY_BYTES * keys_.length;
    int sizeOfValues = SIZE_OF_VALUE_BYTES * numValues_;
    for (int i = 0; i < values_.length; i++) {
      if (values_[i] == null) {
        mem.fill(offset, sizeOfValues, (byte)0);
      } else {
        mem.putDoubleArray(offset, values_[i], 0, numValues_);
      }
      offset += sizeOfValues;
    }
    return byteArray;
  }

  @Override
  protected void setValues(int index, double[] values, boolean isCopyRequired) {
    if (isCopyRequired) {
      values_[index] = values.clone();
    } else {
      values_[index] = values;
    }
  }

  @Override
  protected void updateValues(int index, double[] values) {
    for (int i = 0; i < numValues_; i++) values_[index][i] += values[i];
  }

  @Override
  protected void setNotEmpty() {
    isEmpty_ = false;
  }

  @Override
  protected boolean isInSamplingMode() {
    return samplingProbability_ < 1f;
  }

  @Override
  protected void setThetaLong(long theta) {
    theta_ = theta;
  }

  @Override
  protected int getResizeFactor() {
    return 1 << lgResizeFactor_;
  }

  @Override
  protected int getCurrentCapacity() {
    return keys_.length;
  }

  @Override
  protected void rebuild(int newCapacity) {
    long[] oldKeys = keys_;
    double[][] oldValues = values_;
    keys_ = new long[newCapacity];
    values_ = new double[newCapacity][];
    count_ = 0;
    mask_ = newCapacity - 1;
    for (int i = 0; i < oldKeys.length; i++) {
      if (oldKeys[i] != 0 && oldKeys[i] < theta_) insert(oldKeys[i], oldValues[i]);
    }
    setRebuildThreshold();
  }

  @Override
  ArrayOfDoublesSketchIterator iterator() {
    return new HeapArrayOfDoublesSketchIterator(keys_, values_);
  }

}