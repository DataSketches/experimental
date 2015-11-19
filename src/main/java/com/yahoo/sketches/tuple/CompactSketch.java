package com.yahoo.sketches.tuple;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class CompactSketch<S extends Summary> extends Sketch<S> {

  public static final byte serialVersionUID = 2;

  private Entry<S>[] entries_;

  public CompactSketch() {
    theta_ = Long.MAX_VALUE;
  }

  CompactSketch(Entry<S>[] entries, long theta) {
    this.entries_ = entries;
    this.theta_ = theta;
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
      entries_ = (Entry<S>[]) java.lang.reflect.Array.newInstance(new Entry<S>(0, null).getClass(), count);
      for (int i = 0; i < count; i++) {
        long key = buffer.getLong();
        S summary = (S) SerializerDeserializer.deserializeFromByteBuffer(buffer, className);
        entries_[i] = new Entry<S>(key, summary);
      }
    }
  }

  @Override
  public S[] getSummaries() {
    if (isEmpty()) return null;
    @SuppressWarnings("unchecked")
    S[] summaries = (S[]) java.lang.reflect.Array.newInstance(entries_[0].getSummary().getClass(), entries_.length);
    for (int i = 0; i < entries_.length; ++i) summaries[i] = entries_[i].getSummary().copy(); // TODO: should we copy summaries?
    return summaries;
  }

  @Override
  //keep in mind that entries returned by this method are not copies, but the same objects, which the sketch holds
  Entry<S>[] getEntries() {
    return entries_;
  }

  @Override
  public boolean isEmpty() {
    return (entries_ == null || entries_.length == 0);
  }

  @Override
  public int getRetainedEntries() {
    return entries_ == null ? 0 : entries_.length;
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

  @Override
  public ByteBuffer serializeToByteBuffer() {
    int summariesBytesLength = 0;
    byte[][] summariesBytes = null;
    if (!isEmpty()) {
      summariesBytes = new byte[getRetainedEntries()][];
      for (int i = 0; i < getRetainedEntries(); i++) {
        summariesBytes[i] = entries_[i].summary_.serializeToByteBuffer().array();
        summariesBytesLength += summariesBytes[i].length;
      }
    }

    int headerSizeBytes = isEmpty() ? MINI_HEADER_SIZE_BYTES : HEADER_SIZE_BYTES;
    int sizeBytes = headerSizeBytes + 8 * getRetainedEntries() + summariesBytesLength;
    String summaryClassName = null;
    if (getRetainedEntries() > 0) {
      summaryClassName = entries_[0].getSummary().getClass().getName();
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
        buffer.putLong(entries_[i].key_);
        buffer.put(summariesBytes[i]);
      }
    }
    return buffer;
  }

}
