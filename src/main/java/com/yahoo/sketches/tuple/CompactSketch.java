package com.yahoo.sketches.tuple;

import java.lang.reflect.Array;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

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

  public static final byte serialVersionUID = 2;

  public CompactSketch() {
    theta_ = Long.MAX_VALUE;
  }

  CompactSketch(long[] keys, S[] summaries, long theta) {
    keys_ = keys;
    summaries_ = summaries;
    theta_ = theta;
  }

  /**
   * This is to create an instance of a CompactSketch given a serialized form
   * @param buffer ByteBuffer with serialized CompactSketch
   */
  @SuppressWarnings({"unchecked"})
  public CompactSketch(ByteBuffer buffer) {
    byte version = buffer.get();
    if (version != serialVersionUID) throw new RuntimeException("Serial version mismatch. Expected: " + serialVersionUID + ", actual: " + version);
    SerializerDeserializer.validateType(buffer, SerializerDeserializer.SketchType.CompactSketch);
    byte flags = buffer.get();
    boolean isBigEndian = (flags & (1 << Flags.IS_BIG_ENDIAN.ordinal())) > 0;
    if (isBigEndian ^ buffer.order().equals(ByteOrder.BIG_ENDIAN)) throw new RuntimeException("Byte order mismatch");
    int classNameLength = buffer.get();
    theta_ = Long.MAX_VALUE;
    boolean hasEntries = (flags & (1 << Flags.HAS_ENTRIES.ordinal())) > 0;
    if (hasEntries) {
      int count = buffer.getInt();
      theta_ = buffer.getLong();
      byte[] classNameBuffer = new byte[classNameLength];
      buffer.get(classNameBuffer);
      String className = new String(classNameBuffer);
      keys_ = new long[count];
      for (int i = 0; i < count; i++) {
        long key = buffer.getLong();
        keys_[i] = key;
        S summary = (S) SerializerDeserializer.deserializeFromByteBuffer(buffer, className);
        if (summaries_ == null) summaries_ = (S[]) Array.newInstance(summary.getClass(), count);
        summaries_[i] = summary;
      }
    }
  }

  @Override
  public S[] getSummaries() {
    if (isEmpty()) return null;
    @SuppressWarnings("unchecked")
    S[] summaries = (S[]) Array.newInstance(summaries_.getClass().getComponentType(), summaries_.length);
    for (int i = 0; i < summaries_.length; ++i) summaries[i] = summaries_[i].copy();
    return summaries;
  }

  @Override
  public boolean isEmpty() {
    return (keys_ == null || keys_.length == 0);
  }

  @Override
  public int getRetainedEntries() {
    return keys_ == null ? 0 : keys_.length;
  }

  private static final int MINI_HEADER_SIZE_BYTES =
      1 // version
    + 1 // sketch type
    + 1 // flags
    + 1; // summary class name length

  private static final int HEADER_SIZE_BYTES =
      MINI_HEADER_SIZE_BYTES
    + 4 // count
    + 8; // theta

  private enum Flags { IS_BIG_ENDIAN, HAS_ENTRIES }

  /**
   * @return serialized representation of CompactSketch
   */
  @Override
  public ByteBuffer serializeToByteBuffer() {
    int summariesBytesLength = 0;
    byte[][] summariesBytes = null;
    if (!isEmpty()) {
      summariesBytes = new byte[getRetainedEntries()][];
      for (int i = 0; i < getRetainedEntries(); i++) {
        summariesBytes[i] = summaries_[i].serializeToByteBuffer().array();
        summariesBytesLength += summariesBytes[i].length;
      }
    }

    int headerSizeBytes = isEmpty() ? MINI_HEADER_SIZE_BYTES : HEADER_SIZE_BYTES;
    int sizeBytes = headerSizeBytes + 8 * getRetainedEntries() + summariesBytesLength;
    String summaryClassName = null;
    if (getRetainedEntries() > 0) {
      summaryClassName = summaries_[0].getClass().getName();
      sizeBytes += summaryClassName.length(); 
    }
    ByteBuffer buffer = ByteBuffer.allocate(sizeBytes).order(ByteOrder.nativeOrder());
    buffer.put(serialVersionUID);
    buffer.put((byte)SerializerDeserializer.SketchType.CompactSketch.ordinal());
    boolean isBigEndian = buffer.order().equals(ByteOrder.BIG_ENDIAN);
    buffer.put((byte)(
      ((isBigEndian ? 1 : 0) << Flags.IS_BIG_ENDIAN.ordinal()) |
      ((isEmpty() ? 0 : 1) << Flags.HAS_ENTRIES.ordinal())
    ));
    buffer.put((byte)(summaryClassName == null ? 0 : summaryClassName.length()));
    if (!isEmpty()) {
      buffer.putInt(getRetainedEntries());
      buffer.putLong(theta_);
      buffer.put(summaryClassName.getBytes());
      for (int i = 0; i < getRetainedEntries(); i++) {
        buffer.putLong(keys_[i]);
        buffer.put(summariesBytes[i]);
      }
    }
    return buffer;
  }

}
