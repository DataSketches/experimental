/*
 * Copyright 2015, Yahoo! Inc.
 * Licensed under the terms of the Apache License 2.0. See LICENSE file at the project root for terms.
 */
package com.yahoo.sketches.tuple;

import java.nio.ByteBuffer;
import static com.yahoo.sketches.Util.DEFAULT_UPDATE_SEED;
import com.yahoo.sketches.hash.MurmurHash3;

/**
 * This is an equivalent to com.yahoo.sketches.theta.HeapQuickSelectSketch with
 * addition of an array of double values associated with every unique entry
 * in the sketch. Keys are presented to a sketch along with a number of values
 * When an entry is inserted into a sketch, the input values are stored.
 * When a duplicate key is presented to a sketch then the input values are added
 * to the ones stored.
 */

public class UpdatableArrayOfDoublesQuickSelectSketch extends ArrayOfDoublesQuickSelectSketch {

  /**
   * This is to create an instance of a QuickSelectSketch with default resize factor.
   * @param nomEntries Nominal number of entries. Forced to the nearest power of 2 greater than given value.
   * @param numValues number of double values to keep for each key
   */
  public UpdatableArrayOfDoublesQuickSelectSketch(int nomEntries, int numValues) {
    super(nomEntries, numValues);
  }

  /**
   * This is to create an instance of a QuickSelectSketch with default resize factor and a given sampling probability.
   * @param nomEntries Nominal number of entries. Forced to the nearest power of 2 greater than given value.
   * @param samplingProbability <a href="{@docRoot}/resources/dictionary.html#p">See Sampling Probability, <i>p</i></a>
   * @param numValues number of double values to keep for each key
   */
  public UpdatableArrayOfDoublesQuickSelectSketch(int nomEntries, float samplingProbability, int numValues) {
    super(nomEntries, samplingProbability, numValues);
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
  public UpdatableArrayOfDoublesQuickSelectSketch(int nomEntries, int lgResizeRatio, int numValues) {
    super(nomEntries, lgResizeRatio, 1f, numValues);
  }

  /**
   * This is to create an instance of a QuickSelectSketch with default resize factor and a given sampling probability.
   * @param nomEntries Nominal number of entries. Forced to the nearest power of 2 greater than given value.
   * @param samplingProbability <a href="{@docRoot}/resources/dictionary.html#p">See Sampling Probability, <i>p</i></a>
   * @param numValues number of double values to keep for each key
   */
  public UpdatableArrayOfDoublesQuickSelectSketch(int nomEntries, int lgResizeRatio, float samplingProbability, int numValues) {
    super(nomEntries, lgResizeRatio, samplingProbability, numValues);
  }

  public UpdatableArrayOfDoublesQuickSelectSketch(ByteBuffer buffer) {
    super(buffer);
  }

  /**
   * Updates this sketch with a long key and double values.
   * The values will be stored or added to the ones associated with the key 
   * 
   * @param key The given long key
   * @param values The given values
   */
  public void update(long key, double[] values) {
    update(Util.longToLongArray(key), values);
  }

  /**
   * Updates this sketch with a double key and double values.
   * The values will be stored or added to the ones associated with the key 
   * 
   * @param key The given double key
   * @param value The given values
   */
  public void update(double key, double[] values) {
    update(Util.doubleToLongArray(key), values);
  }

  /**
   * Updates this sketch with a String key and double values.
   * The values will be stored or added to the ones associated with the key 
   * 
   * @param key The given String key
   * @param value The given values
   */
  public void update(String key, double[] values) {
    update(Util.stringToByteArray(key), values);
  }

  /**
   * Updates this sketch with a byte[] key and double values.
   * The values will be stored or added to the ones associated with the key 
   * 
   * @param key The given byte[] key
   * @param value The given values
   */
  public void update(byte[] key, double[] values) {
    if (key == null || key.length == 0) return;
    insertOrIgnore(MurmurHash3.hash(key, DEFAULT_UPDATE_SEED)[0] >>> 1, values);
  }

  /**
   * Updates this sketch with a int[] key and double values.
   * The values will be stored or added to the ones associated with the key 
   * 
   * @param key The given int[] key
   * @param value The given values
   */
  public void update(int[] key, double[] values) {
    if (key == null || key.length == 0) return;
    insertOrIgnore(MurmurHash3.hash(key, DEFAULT_UPDATE_SEED)[0] >>> 1, values);
  }

  /**
   * Updates this sketch with a long[] key and double values.
   * The values will be stored or added to the ones associated with the key 
   * 
   * @param key The given long[] key
   * @param value The given values
   */
  public void update(long[] key, double[] values) {
    if (key == null || key.length == 0) return;
    insertOrIgnore(MurmurHash3.hash(key, DEFAULT_UPDATE_SEED)[0] >>> 1, values);
  }

  // TODO: check input length
  private void insertOrIgnore(long key, double[] values) {
    setIsEmpty(false);
    if (key >= getThetaLong()) return;
    int countBefore = getRetainedEntries();
    int index = findOrInsert(key);
    if (getRetainedEntries() > countBefore) {
      values_[index] = values.clone();
    } else {
      for (int i = 0; i < numValues_; i++) values_[index][i] += values[i];
    }
    rebuildIfNeeded();
  }

}
