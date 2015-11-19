/*
 * Copyright 2015, Yahoo! Inc.
 * Licensed under the terms of the Apache License 2.0. See LICENSE file at the project root for terms.
 */
package com.yahoo.sketches.tuple;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static com.yahoo.sketches.Util.ceilingPowerOf2;

import com.yahoo.sketches.QuickSelect;

public class QuickSelectSketch<S extends Summary> extends Sketch<S> {

  public static final byte serialVersionUID = 2;

  private static final int MIN_NOM_ENTRIES = 32;
  private static final int DEFAULT_LG_RESIZE_RATIO = 3;
  private static final double REBUILD_RATIO_AT_RESIZE = 0.5;
  private static final double REBUILD_RATIO_AT_TARGET_SIZE = 15.0 / 16.0;
  private int nomEntries_;
  private int lgResizeRatio_;
  private int count_;
  private final SummaryFactory<S> summaryFactory_;
  private float samplingProbability_;
  private boolean isEmpty_ = true;
  private Entry<S>[] table_;
  private int rebuildThreshold_;


  /**
   * This is to create an instance of a QuickSelectSketch with default resize factor.
   * @param nomEntries Nominal number of entries. Forced to the nearest power of 2 greater than given value.
   * @param summaryFactory An instance of a SummaryFactory.
   */
  QuickSelectSketch(int nomEntries, SummaryFactory<S> summaryFactory) {
    this(nomEntries, DEFAULT_LG_RESIZE_RATIO, summaryFactory);
  }

  /**
   * This is to create an instance of a QuickSelectSketch with default resize factor and a given sampling probability.
   * @param nomEntries Nominal number of entries. Forced to the nearest power of 2 greater than given value.
   * @param samplingProbability <a href="{@docRoot}/resources/dictionary.html#p">See Sampling Probability, <i>p</i></a>
   * @param summaryFactory An instance of a SummaryFactory.
   */
  QuickSelectSketch(int nomEntries, float samplingProbability, SummaryFactory<S> summaryFactory) {
    this(nomEntries, DEFAULT_LG_RESIZE_RATIO, samplingProbability, summaryFactory);
  }

  /**
   * This is to create an instance of a QuickSelectSketch with custom resize factor
   * @param nomEntries Nominal number of entries. Forced to the nearest power of 2 greater than given value.
   * @param lgResizeRatio log2(resizeRatio) - value from 0 to 3:
   * 0 - no resizing (max size allocated),
   * 1 - double internal hash table each time it reaches a threshold
   * 2 - grow four times
   * 3 - grow eight times (default) 
   * @param summaryFactory An instance of a SummaryFactory.
   */
  QuickSelectSketch(int nomEntries, int lgResizeRatio, SummaryFactory<S> summaryFactory) {
    this(nomEntries, lgResizeRatio, 1f, summaryFactory);
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
   * @param summaryFactory An instance of a SummaryFactory.
   */
  @SuppressWarnings("unchecked")
  QuickSelectSketch(int nomEntries, int lgResizeRatio, float samplingProbability, SummaryFactory<S> summaryFactory) {
    this.nomEntries_ = ceilingPowerOf2(nomEntries);
    this.lgResizeRatio_ = lgResizeRatio;
    this.samplingProbability_ = samplingProbability;
    this.summaryFactory_ = summaryFactory;
    this.theta_ = (long) (Long.MAX_VALUE * (double) samplingProbability);
    int startingSize = 1 << Util.startingSubMultiple(
      Integer.numberOfTrailingZeros(ceilingPowerOf2(nomEntries) * 2), // target table size is twice the number of nominal entries
      lgResizeRatio,
      Integer.numberOfTrailingZeros(MIN_NOM_ENTRIES)
    );
    this.table_ = (Entry<S>[]) java.lang.reflect.Array.newInstance((new Entry<S>(0, null)).getClass(), startingSize);
    setRebuildThreshold();
  }

  /**
   * This is to create an instance of a QuickSelectSketch given a serialized form
   * @param buffer ByteBuffer with serialized QukckSelectSketch
   */
  @SuppressWarnings("unchecked")
  public QuickSelectSketch(ByteBuffer buffer) {
    byte version = buffer.get();
    if (version != serialVersionUID) throw new RuntimeException("Serial version mismatch. Expected: " + serialVersionUID + ", actual: " + version);
    SerializerDeserializer.validateType(buffer, SerializerDeserializer.SketchType.QuickSelectSketch);
    byte flags = buffer.get();
    boolean isBigEndian = (flags & (1 << Flags.IS_BIG_ENDIAN.ordinal())) > 0;
    if (isBigEndian ^ buffer.order().equals(ByteOrder.BIG_ENDIAN)) throw new RuntimeException("Byte order mismatch");
    nomEntries_ = 1 << buffer.get();
    int currentCapacity = 1 << buffer.get();
    lgResizeRatio_ = buffer.get();
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
    summaryFactory_ = (SummaryFactory<S>) SerializerDeserializer.deserializeFromByteBuffer(buffer);
    theta_ = (long) (Long.MAX_VALUE * (double) samplingProbability_);
    table_ = (Entry<S>[]) java.lang.reflect.Array.newInstance((new Entry<S>(0, null)).getClass(), currentCapacity);
    for (int i = 0; i < count; i++) {
      long key = buffer.getLong();
      S summary = summaryFactory_.deserializeSummaryFromByteBuffer(buffer);
      insert(new Entry<S>(key, summary));
    }
    if (thetaLong != null) setThetaLong(thetaLong);
    setIsEmpty((flags & (1 << Flags.IS_EMPTY.ordinal())) > 0);
    setRebuildThreshold();
  }

  @Override
  public S[] getSummaries() {
    @SuppressWarnings("unchecked")
    S[] summaries = (S[]) java.lang.reflect.Array.newInstance(summaryFactory_.newSummary().getClass(), count_);
    int i = 0;
    for (int j = 0; j < table_.length; j++) {
      if (table_[j] != null) summaries[i++] = table_[j].summary_.copy(); // TODO: should we copy summaries?
    }
    return summaries;
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
      rebuild(table_.length);
    }
  }

  public CompactSketch<S> compact() {
    trim();
    return new CompactSketch<S>(getEntries(), theta_); // TODO: should we copy summaries here?
  }

  private enum Flags { IS_BIG_ENDIAN, IS_IN_SAMPLING_MODE, IS_EMPTY, HAS_ENTRIES }

  /**
   *
   */
  public ByteBuffer serializeToByteBuffer() {
    byte[] summaryFactoryBytes = SerializerDeserializer.serializeToByteBuffer(summaryFactory_).array();
    byte[][] summariesBytes = null;
    int summariesBytesLength = 0;
    if (count_ > 0) {
      summariesBytes = new byte[count_][];
      int i = 0;
      for (int j = 0; j < table_.length; j++) {
        if (table_[j] != null) {
          summariesBytes[i] = table_[j].summary_.serializeToByteBuffer().array();
          summariesBytesLength += summariesBytes[i].length;
          i++;
        }
      }
    }

    int sizeBytes = 
        1 // version
      + 1 // sketch type
      + 1 // flags
      + 1 // log2(nomEntries)
      + 1 // log2(currentCapacity)
      + 1; // log2(resizeRatio)
    if (count_ > 0) {
      sizeBytes +=
          4 // count
        + 8; // theta
    }
    if (isInSamplingMode()) sizeBytes += 4; // samplingProbability
    sizeBytes += 8 * count_ + summaryFactoryBytes.length + summariesBytesLength;
    ByteBuffer buffer = ByteBuffer.allocate(sizeBytes).order(ByteOrder.nativeOrder());
    buffer.put(serialVersionUID);
    buffer.put((byte)SerializerDeserializer.SketchType.QuickSelectSketch.ordinal());
    boolean isBigEndian = buffer.order().equals(ByteOrder.BIG_ENDIAN);
    buffer.put((byte)(
      ((isBigEndian ? 1 : 0) << Flags.IS_BIG_ENDIAN.ordinal()) |
      ((isInSamplingMode() ? 1 : 0) << Flags.IS_IN_SAMPLING_MODE.ordinal()) |
      ((isEmpty_ ? 1 : 0) << Flags.IS_EMPTY.ordinal()) |
      ((count_ > 0 ? 1 : 0) << Flags.HAS_ENTRIES.ordinal())
    ));
    buffer.put((byte)Integer.numberOfTrailingZeros(nomEntries_));
    buffer.put((byte)Integer.numberOfTrailingZeros(table_.length));
    buffer.put((byte)lgResizeRatio_);
    if (count_ > 0) {
      buffer.putInt(count_);
      buffer.putLong(theta_);
    }
    if (samplingProbability_ < 1f) buffer.putFloat(samplingProbability_);
    buffer.put(summaryFactoryBytes);
    if (count_ > 0) {
      int i = 0;
      for (int j = 0; j < table_.length; j++) {
        if (table_[j] != null) {
          buffer.putLong(table_[j].key_);
          buffer.put(summariesBytes[i]);
          i++;
        }
      }
    }
    return buffer;
  }


  // non-public methods below

  @Override
  // keep in mind that entries returned by this method are not copies, but the same objects, which the sketch holds
  Entry<S>[] getEntries() {
    @SuppressWarnings({"unchecked"})
    Entry<S>[] entries = (Entry<S>[]) java.lang.reflect.Array.newInstance(table_.getClass().getComponentType(), count_);
    int i = 0;
    for (int j = 0; j < table_.length; j++) {
      if (table_[j] != null) entries[i++] = table_[j];
    }
    return entries;
  }

  void merge(long key, S summary) {
    isEmpty_ = false;
    if (key < theta_) {
      int countBefore = count_;
      Entry<S> thisNode = findOrInsert(key);
      if (count_ == countBefore) {
        thisNode.summary_ = summaryFactory_.getSummarySetOperations().union(thisNode.summary_, summary);
      } else if (!rebuildIfNeeded() || thisNode.key_ < theta_) { // node is still there after rebuild
        thisNode.summary_ = summary.copy();
      }
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

  SummaryFactory<S> getSummaryFactory() {
    return summaryFactory_;
  }

  // assumes that table.length is power of 2
  private int getIndex(long key) {
    return (int) (key & (table_.length - 1));
  }

  // 7 bits, last zero to make even
  private static final int STRIDE_MASK = 0xfe;
  private static final int getStride(long key) {
    // make odd and independent of index assuming that lower 32 bits are used for index
    return ((int) ((key >> 32) & STRIDE_MASK)) + 1;
  }

  Entry<S> findOrInsert(long key) {
    int index = getIndex(key);
    while (table_[index] != null) {
      if (table_[index].key_ == key) return table_[index];
      index = (index + getStride(key)) & (table_.length - 1);
    }
    table_[index] = new Entry<S>(key);
    count_++;
    return table_[index];
  }

  boolean rebuildIfNeeded() {
    if (count_ < rebuildThreshold_) return false;
    if (table_.length > nomEntries_) {
      updateTheta();
      rebuild(table_.length);
    } else {
      rebuild(table_.length * (1 << lgResizeRatio_));
    }
    return true;
  }

  private void insert(Entry<S> node) {
    int index = getIndex(node.key_);
    while (table_[index] != null) {
      index = (index + getStride(node.key_)) & (table_.length - 1);
    }
    table_[index] = node;
    count_++;
  }

  private void updateTheta() {
    long[] keys = new long[count_];
    int i = 0;
    for (int j = 0; j < table_.length; j++) {
      if (table_[j] != null) keys[i++] = table_[j].key_;
    }
    theta_ = QuickSelect.select(keys, 0, count_ - 1, nomEntries_);
  }

  @SuppressWarnings({"unchecked"})
  private void rebuild(int newSize) {
    Entry<S>[] oldTable = table_;
    table_ = (Entry<S>[]) java.lang.reflect.Array.newInstance(oldTable.getClass().getComponentType(), newSize);
    count_ = 0;
    for (int i = 0; i < oldTable.length; i++) {
      if (oldTable[i] != null && oldTable[i].key_ < theta_) insert(oldTable[i]);
    }
    setRebuildThreshold();
  }

  private void setRebuildThreshold() {
    if (table_.length > nomEntries_) {
      rebuildThreshold_ = (int) (table_.length * REBUILD_RATIO_AT_TARGET_SIZE);
    } else {
      rebuildThreshold_ = (int) (table_.length * REBUILD_RATIO_AT_RESIZE);
    }
  }

}
