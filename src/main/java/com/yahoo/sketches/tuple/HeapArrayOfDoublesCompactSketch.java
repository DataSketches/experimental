package com.yahoo.sketches.tuple;

import java.nio.ByteOrder;

import com.yahoo.sketches.memory.Memory;
import com.yahoo.sketches.memory.NativeMemory;

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
public class HeapArrayOfDoublesCompactSketch extends ArrayOfDoublesCompactSketch {

  protected long[] keys_;
  protected double[][] values_;

  public HeapArrayOfDoublesCompactSketch() {
    theta_ = Long.MAX_VALUE;
  }

  HeapArrayOfDoublesCompactSketch(UpdatableArrayOfDoublesSketch sketch) {
    isEmpty_ = sketch.isEmpty();
    numValues_ = sketch.getNumValues();
    theta_ = sketch.getThetaLong();
    int count = sketch.getRetainedEntries();
    if (count > 0) {
      keys_ = new long[count];
      values_ = new double[count][];
      ArrayOfDoublesSketchIterator it = sketch.iterator();
      int i = 0;
      while (it.next()) {
        keys_[i] = it.getKey();
        values_[i] = it.getValues();
        i++;
      }
    }
  }

  /**
   * This is to create an instance given a serialized form
   * @param mem <a href="{@docRoot}/resources/dictionary.html#mem">See Memory</a>
   */
  public HeapArrayOfDoublesCompactSketch(Memory mem) {
    SerializerDeserializer.validateType(mem.getByte(SKETCH_TYPE_BYTE), SerializerDeserializer.SketchType.ArrayOfDoublesCompactSketch);
    byte version = mem.getByte(SERIAL_VERSION_BYTE);
    if (version != serialVersionUID) throw new RuntimeException("Serial version mismatch. Expected: " + serialVersionUID + ", actual: " + version);
    isEmpty_ = mem.isAllBitsSet(FLAGS_BYTE, (byte) (1 << Flags.IS_EMPTY.ordinal()));
    numValues_ = mem.getByte(NUM_VALUES_BYTE);
    theta_ = mem.getLong(THETA_LONG);
    boolean hasEntries = mem.isAllBitsSet(FLAGS_BYTE, (byte) (1 << Flags.HAS_ENTRIES.ordinal()));
    if (hasEntries) {
      int count = mem.getInt(RETAINED_ENTRIES_INT);
      keys_ = new long[count];
      values_ = new double[count][numValues_];
      mem.getLongArray(ENTRIES_START, keys_, 0, count);
      int offset = ENTRIES_START + SIZE_OF_KEY_BYTES * count;
      for (int i = 0; i < count; i++) {
        mem.getDoubleArray(offset, values_[i], 0, numValues_);
        offset += SIZE_OF_VALUE_BYTES * numValues_;
      }
    }
  }

  @Override
  public boolean isEmpty() {
    return isEmpty_;
  }

  @Override
  public int getRetainedEntries() {
    return keys_ == null ? 0 : keys_.length;
  }

  private static final int HEADER_SIZE_BYTES =
      1 // version
    + 1 // sketch type
    + 1 // flags
    + 1 // numValues
    + 4 // count
    + 8; // theta

  /**
   * @return serialized representation of CompactSketch
   */
  @Override
  public byte[] toByteArray() {
    int count = getRetainedEntries();
    int sizeBytes = HEADER_SIZE_BYTES + SIZE_OF_KEY_BYTES * count + SIZE_OF_VALUE_BYTES * numValues_ * count;
    byte[] bytes = new byte[sizeBytes];
    Memory mem = new NativeMemory(bytes);
    mem.putByte(SERIAL_VERSION_BYTE, serialVersionUID);
    mem.putByte(SKETCH_TYPE_BYTE, (byte)SerializerDeserializer.SketchType.ArrayOfDoublesCompactSketch.ordinal());
    boolean isBigEndian = ByteOrder.nativeOrder().equals(ByteOrder.BIG_ENDIAN);
    mem.putByte(FLAGS_BYTE, (byte) (
      ((isBigEndian ? 1 : 0) << Flags.IS_BIG_ENDIAN.ordinal()) |
      ((isEmpty() ? 1 : 0) << Flags.IS_EMPTY.ordinal()) |
      ((count > 0 ? 1 : 0) << Flags.HAS_ENTRIES.ordinal())
    ));
    mem.putByte(NUM_VALUES_BYTE, (byte) numValues_);
    mem.putInt(RETAINED_ENTRIES_INT, count);
    mem.putLong(THETA_LONG, theta_);
    if (count > 0) {
      mem.putLongArray(ENTRIES_START, keys_, 0, count);
      int offset = ENTRIES_START + SIZE_OF_KEY_BYTES * count;
      for (int i = 0; i < count; i++) {
        mem.putDoubleArray(offset, values_[i], 0, numValues_);
        offset += SIZE_OF_VALUE_BYTES * numValues_;
      }
    }
    return bytes;
  }

  @Override
  public double[][] getValues() {
    double[][] values = new double[getRetainedEntries()][];
    if (!isEmpty()) {
      int i = 0;
      for (int j = 0; j < values_.length; j++) {
        values[i++] = values_[j].clone();
      }
    }
    return values;
  }

  @Override
  ArrayOfDoublesSketchIterator iterator() {
    return new HeapArrayOfDoublesSketchIterator(keys_, values_);
  }

}
