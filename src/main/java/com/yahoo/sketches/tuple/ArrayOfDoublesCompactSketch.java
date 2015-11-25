package com.yahoo.sketches.tuple;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * CompactSketches are never created directly. They are created as a result of
 * the compact() method of a QuickSelectSketch or as a result of the getResult()
 * method of a set operation like Union, Intersection or AnotB. CompactSketch
 * consists of a compact list (i.e. no intervening spaces) of hash values,
 * corresponding list of double values, and a value for theta. The lists may or may
 * not be ordered. CompactSketch is read-only.
 *
 * @param <S> Type of Summary
 */
public class ArrayOfDoublesCompactSketch extends ArrayOfDoublesSketch {

  public static final byte serialVersionUID = 1;

  public ArrayOfDoublesCompactSketch() {
    theta_ = Long.MAX_VALUE;
  }

  // doesn't clone neither keys nor values
  ArrayOfDoublesCompactSketch(long[] keys, double[][] values, long theta) {
    keys_ = keys;
    values_ = values;
    theta_ = theta;
  }

  /**
   * This is to create an instance of a CompactSketch given a serialized form
   * @param buffer ByteBuffer with serialized CompactSketch
   */
  public ArrayOfDoublesCompactSketch(ByteBuffer buffer) {
    byte version = buffer.get();
    if (version != serialVersionUID) throw new RuntimeException("Serial version mismatch. Expected: " + serialVersionUID + ", actual: " + version);
    SerializerDeserializer.validateType(buffer, SerializerDeserializer.SketchType.ArrayOfDoublesCompactSketch);
    byte flags = buffer.get();
    boolean isBigEndian = (flags & (1 << Flags.IS_BIG_ENDIAN.ordinal())) > 0;
    if (isBigEndian ^ buffer.order().equals(ByteOrder.BIG_ENDIAN)) throw new RuntimeException("Byte order mismatch");
    theta_ = Long.MAX_VALUE;
    boolean hasEntries = (flags & (1 << Flags.HAS_ENTRIES.ordinal())) > 0;
    if (hasEntries) {
      int count = buffer.getInt();
      theta_ = buffer.getLong();
      int numValues = buffer.get();
      keys_ = new long[count];
      values_ = new double[count][numValues];
      for (int i = 0; i < count; i++) {
        long key = buffer.getLong();
        keys_[i] = key;
        for (int j = 0; j < numValues; j++) values_[i][j] = buffer.getDouble();
      }
    }
  }

  @Override
  public double[][] getValues() {
    if (isEmpty()) return null;
    double[][] values = new double[values_.length][];
    for (int i = 0; i < values_.length; i++) values[i] = values_[i].clone();
    return values;
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
    + 1; // flags

  private static final int HEADER_SIZE_BYTES =
      MINI_HEADER_SIZE_BYTES
    + 4 // count
    + 8 // theta
    + 1; // number of values per key

  private enum Flags { IS_BIG_ENDIAN, HAS_ENTRIES }

  /**
   * @return serialized representation of CompactSketch
   */
  @Override
  public ByteBuffer serializeToByteBuffer() {
    int headerSizeBytes = isEmpty() ? MINI_HEADER_SIZE_BYTES : HEADER_SIZE_BYTES;
    int sizeBytes = headerSizeBytes + 8 * getRetainedEntries();
    if (!isEmpty()) {
      sizeBytes += 8 * values_[0].length * getRetainedEntries();
    }
    ByteBuffer buffer = ByteBuffer.allocate(sizeBytes).order(ByteOrder.nativeOrder());
    buffer.put(serialVersionUID);
    buffer.put((byte)SerializerDeserializer.SketchType.ArrayOfDoublesCompactSketch.ordinal());
    boolean isBigEndian = buffer.order().equals(ByteOrder.BIG_ENDIAN);
    buffer.put((byte)(
      ((isBigEndian ? 1 : 0) << Flags.IS_BIG_ENDIAN.ordinal()) |
      ((isEmpty() ? 0 : 1) << Flags.HAS_ENTRIES.ordinal())
    ));
    if (!isEmpty()) {
      buffer.putInt(getRetainedEntries());
      buffer.putLong(theta_);
      buffer.put((byte)values_[0].length);
      for (int i = 0; i < getRetainedEntries(); i++) {
        buffer.putLong(keys_[i]);
        for (double value: values_[i]) buffer.putDouble(value);
      }
    }
    return buffer;
  }

}
