/*
 * Copyright 2015, Yahoo! Inc.
 * Licensed under the terms of the Apache License 2.0. See LICENSE file at the project root for terms.
 */
package com.yahoo.sketches.tuple;

import java.lang.reflect.Array;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import com.yahoo.sketches.Family;

/**
 * CompactSketches are never created directly. They are created as a result of
 * the compact() method of a QuickSelectSketch or as a result of the getResult()
 * method of a set operation like Union, Intersection or AnotB. CompactSketch
 * consists of a compact list (i.e. no intervening spaces) of hash values,
 * corresponding list of Summaries, and a value for theta. The lists may or may
 * not be ordered. CompactSketch is read-only.
 *
 * @param <S> Type of Summary
 */
public class CompactSketch<S extends Summary> extends Sketch<S> {

  public static final byte serialVersionUID = 1;

  private enum Flags { IS_BIG_ENDIAN, IS_EMPTY, HAS_ENTRIES, IS_THETA_INCLUDED }

  public CompactSketch() {
    theta_ = Long.MAX_VALUE;
  }

  CompactSketch(long[] keys, S[] summaries, long theta, boolean isEmpty) {
    keys_ = keys;
    summaries_ = summaries;
    theta_ = theta;
    isEmpty_ = isEmpty;
  }

  /**
   * This is to create an instance of a CompactSketch given a serialized form
   * @param buffer ByteBuffer with serialized CompactSketch
   */
  @SuppressWarnings({"unchecked"})
  public CompactSketch(ByteBuffer buffer) {
    byte preambleLongs = buffer.get();
    byte version = buffer.get();
    byte familyId = buffer.get();
    SerializerDeserializer.validateFamily(familyId, preambleLongs);
    if (version != serialVersionUID) throw new RuntimeException("Serial version mismatch. Expected: " + serialVersionUID + ", actual: " + version);
    SerializerDeserializer.validateType(buffer, SerializerDeserializer.SketchType.CompactSketch);
    byte flags = buffer.get();
    boolean isBigEndian = (flags & (1 << Flags.IS_BIG_ENDIAN.ordinal())) > 0;
    if (isBigEndian ^ buffer.order().equals(ByteOrder.BIG_ENDIAN)) throw new RuntimeException("Byte order mismatch");
    isEmpty_ = (flags & (1 << Flags.IS_EMPTY.ordinal())) > 0;
    boolean isThetaIncluded = (flags & (1 << Flags.IS_THETA_INCLUDED.ordinal())) > 0;
    if (isThetaIncluded) {
      theta_ = buffer.getLong();
    } else {
      theta_ = Long.MAX_VALUE;
    }
    boolean hasEntries = (flags & (1 << Flags.HAS_ENTRIES.ordinal())) > 0;
    if (hasEntries) {
      int classNameLength = buffer.get();
      int count = buffer.getInt();
      byte[] classNameBuffer = new byte[classNameLength];
      buffer.get(classNameBuffer);
      String className = new String(classNameBuffer);
      keys_ = new long[count];
      for (int i = 0; i < count; i++) keys_[i] = buffer.getLong();
      for (int i = 0; i < count; i++) {
        S summary = (S) SerializerDeserializer.deserializeFromByteBuffer(buffer, className);
        if (summaries_ == null) summaries_ = (S[]) Array.newInstance(summary.getClass(), count);
        summaries_[i] = summary;
      }
    }
  }

  @Override
  public S[] getSummaries() {
    if (keys_ == null || keys_.length == 0) return null;
    @SuppressWarnings("unchecked")
    S[] summaries = (S[]) Array.newInstance(summaries_.getClass().getComponentType(), summaries_.length);
    for (int i = 0; i < summaries_.length; ++i) summaries[i] = summaries_[i].copy();
    return summaries;
  }

  @Override
  public int getRetainedEntries() {
    return keys_ == null ? 0 : keys_.length;
  }

  /**
   * @return serialized representation of CompactSketch
   */
  @Override
  public ByteBuffer serializeToByteBuffer() {
    int summariesBytesLength = 0;
    byte[][] summariesBytes = null;
    int count = getRetainedEntries();
    if (count > 0) {
      summariesBytes = new byte[count][];
      for (int i = 0; i < count; i++) {
        summariesBytes[i] = summaries_[i].serializeToByteBuffer().array();
        summariesBytesLength += summariesBytes[i].length;
      }
    }

    int sizeBytes =
        1 // preamble longs
      + 1 // serial version
      + 1 // family id
      + 1 // sketch type
      + 1; // flags
    boolean isThetaIncluded = theta_ < Long.MAX_VALUE;
    if (isThetaIncluded) sizeBytes += 8; // theta
    String summaryClassName = null;
    if (count > 0) {
      summaryClassName = summaries_[0].getClass().getName();
      sizeBytes +=
          1 // summary class name length
        + 4 // count
        + summaryClassName.length() 
        + 8 * count + summariesBytesLength;
    }
    ByteBuffer buffer = ByteBuffer.allocate(sizeBytes).order(ByteOrder.nativeOrder());
    buffer.put((byte) 1);
    buffer.put(serialVersionUID);
    buffer.put((byte) Family.TUPLE.getID());
    buffer.put((byte) SerializerDeserializer.SketchType.CompactSketch.ordinal());
    boolean isBigEndian = buffer.order().equals(ByteOrder.BIG_ENDIAN);
    buffer.put((byte) (
      (isBigEndian ? 1 << Flags.IS_BIG_ENDIAN.ordinal() : 0) |
      (isEmpty_ ? 1 << Flags.IS_EMPTY.ordinal() : 0) |
      (count > 0 ? 1 << Flags.HAS_ENTRIES.ordinal() : 0) |
      (isThetaIncluded ? 1 << Flags.IS_THETA_INCLUDED.ordinal() : 0)
    ));
    if (isThetaIncluded) buffer.putLong(theta_);
    if (count > 0) {
      buffer.put((byte) summaryClassName.length());
      buffer.putInt(getRetainedEntries());
      buffer.put(summaryClassName.getBytes());
      for (int i = 0; i < count; i++) buffer.putLong(keys_[i]);
      for (int i = 0; i < count; i++) buffer.put(summariesBytes[i]);
    }
    return buffer;
  }

}
