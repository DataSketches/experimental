package com.yahoo.sketches.tuple;

import static com.yahoo.sketches.Util.ceilingPowerOf2;

import java.nio.ByteOrder;
import java.util.Arrays;

import com.yahoo.sketches.memory.Memory;
import com.yahoo.sketches.memory.MemoryUtil;
import com.yahoo.sketches.memory.NativeMemory;

public class DirectArrayOfDoublesQuickSelectSketch extends ArrayOfDoublesQuickSelectSketch {

  // these values exist only on heap, never serialized
  private Memory mem_;
  // these can be derived from the mem_ contents, but are kept here for performance
  private int keysOffset_;
  private int valuesOffset_;
  
  public DirectArrayOfDoublesQuickSelectSketch(int nomEntries, int numValues, Memory dstMem) {
    this(nomEntries, DEFAULT_LG_RESIZE_FACTOR, 1f, numValues, dstMem);
  }

  public DirectArrayOfDoublesQuickSelectSketch(int nomEntries, float samplingProbability, int numValues, Memory dstMem) {
    this(nomEntries, DEFAULT_LG_RESIZE_FACTOR, samplingProbability, numValues, dstMem);
  }

  public DirectArrayOfDoublesQuickSelectSketch(int nomEntries, int lgResizeFactor, int numValues, Memory dstMem) {
    this(nomEntries, lgResizeFactor, 1f, numValues, dstMem);
  }

  /**
   * Construct a new sketch using the given Memory as its backing store.
   * 
   * @param nomEntries
   * @param numValues
   * @param dstMem
   */
  public DirectArrayOfDoublesQuickSelectSketch(int nomEntries, int lgResizeFactor, float samplingProbability, int numValues, Memory dstMem) {
    mem_ = dstMem;
    int startingCapacity = 1 << Util.startingSubMultiple(
      Integer.numberOfTrailingZeros(ceilingPowerOf2(nomEntries) * 2), // target table size is twice the number of nominal entries
      lgResizeFactor,
      Integer.numberOfTrailingZeros(MIN_NOM_ENTRIES)
    );
    mem_.putByte(SERIAL_VERSION_BYTE, serialVersionUID);
    mem_.putByte(SKETCH_TYPE_BYTE, (byte)SerializerDeserializer.SketchType.ArrayOfDoublesQuickSelectSketch.ordinal());
    boolean isBigEndian = ByteOrder.nativeOrder().equals(ByteOrder.BIG_ENDIAN);
    mem_.putByte(FLAGS_BYTE, (byte)(
      (isBigEndian ? 1 << Flags.IS_BIG_ENDIAN.ordinal() : 0) |
      (samplingProbability < 1f ? 1 << Flags.IS_IN_SAMPLING_MODE.ordinal() : 0) |
      (1 << Flags.IS_EMPTY.ordinal())
    ));
    mem_.putByte(LG_NOM_ENTRIES_BYTE, (byte)Integer.numberOfTrailingZeros(nomEntries));
    mem_.putByte(LG_CUR_CAPACITY_BYTE, (byte)Integer.numberOfTrailingZeros(startingCapacity));
    mem_.putByte(LG_RESIZE_FACTOR_BYTE, (byte)lgResizeFactor);
    numValues_ = numValues;
    mem_.putByte(NUM_VALUES_BYTE, (byte)numValues);
    mem_.putInt(RETAINED_ENTRIES_INT, 0);
    theta_ = (long) (Long.MAX_VALUE * (double) samplingProbability);
    mem_.putLong(THETA_LONG, theta_);
    mem_.putFloat(SAMPLING_P_FLOAT, samplingProbability);
    keysOffset_ = ENTRIES_START;
    valuesOffset_ = keysOffset_ + SIZE_OF_KEY_BYTES * startingCapacity;
    mem_.clear(keysOffset_, SIZE_OF_KEY_BYTES * startingCapacity + SIZE_OF_VALUE_BYTES * startingCapacity * numValues); // clear data area
    mask_ = startingCapacity - 1;
    setRebuildThreshold();
  }

  /**
   * Wraps the given Memory.
   * @param mem <a href="{@docRoot}/resources/dictionary.html#mem">See Memory</a>
   */
  public DirectArrayOfDoublesQuickSelectSketch(Memory mem) {
    mem_ = mem;
    SerializerDeserializer.validateType(mem_.getByte(SKETCH_TYPE_BYTE), SerializerDeserializer.SketchType.ArrayOfDoublesQuickSelectSketch);
    byte version = mem_.getByte(SERIAL_VERSION_BYTE);
    if (version != serialVersionUID) throw new RuntimeException("Serial version mismatch. Expected: " + serialVersionUID + ", actual: " + version);
    keysOffset_ = ENTRIES_START;
    valuesOffset_ = keysOffset_ + SIZE_OF_KEY_BYTES * getCurrentCapacity();
    // to do: make parent take care of its own parts
    numValues_ = mem_.getByte(NUM_VALUES_BYTE);
    mask_ = getCurrentCapacity() - 1;
    theta_ = mem_.getLong(THETA_LONG);
    isEmpty_ = mem_.isAllBitsSet(FLAGS_BYTE, (byte) (1 << Flags.IS_EMPTY.ordinal()));
    setRebuildThreshold();
  }

  @Override
  public int getRetainedEntries() {
    return mem_.getInt(RETAINED_ENTRIES_INT);
  }

  @Override
  public double[][] getValues() {
    double[][] values = new double[getRetainedEntries()][];
    if (!isEmpty()) {
      long keyOffset = keysOffset_;
      long valuesOffset = valuesOffset_;
      int i = 0;
      for (int j = 0; j < getCurrentCapacity(); j++) {
        if (mem_.getLong(keyOffset) != 0) {
          double[] array = new double[getNumValues()];
          mem_.getDoubleArray(valuesOffset, array, 0, getNumValues());
          values[i++] = array;
        }
        keyOffset += SIZE_OF_KEY_BYTES;
        valuesOffset += SIZE_OF_VALUE_BYTES * getNumValues();
      }
    }
    return values;
  }

  @Override
  public byte[] toByteArray() {
    int lengthBytes = valuesOffset_ + SIZE_OF_VALUE_BYTES * getNumValues() * getCurrentCapacity();
    byte[] byteArray = new byte[lengthBytes];
    Memory mem = new NativeMemory(byteArray);
    MemoryUtil.copy(mem_, 0, mem, 0, lengthBytes);
    return byteArray;
  }

  @Override
  public int getNominalEntries() {
    return 1 << mem_.getByte(LG_NOM_ENTRIES_BYTE);
  }

  @Override
  protected long getKey(int index) {
    return mem_.getLong(keysOffset_ + SIZE_OF_KEY_BYTES * index);
  }

  @Override
  protected void setKey(int index, long key) {
    mem_.putLong(keysOffset_ + SIZE_OF_KEY_BYTES * index, key);
  }

  @Override
  protected void incrementCount() {
    int count = mem_.getInt(RETAINED_ENTRIES_INT);
    if (count == 0) mem_.setBits(FLAGS_BYTE, (byte) (1 << Flags.HAS_ENTRIES.ordinal()));
    mem_.putInt(RETAINED_ENTRIES_INT, count + 1);
  }

  @Override
  protected int getCurrentCapacity() {
    return 1 << mem_.getByte(LG_CUR_CAPACITY_BYTE);
  }

  @Override
  protected void setThetaLong(long theta) {
    theta_ = theta;
    mem_.putLong(THETA_LONG, theta_);
  }

  @Override
  protected int getResizeFactor() {
    return 1 << mem_.getByte(LG_RESIZE_FACTOR_BYTE);
  }

  @Override
  // this method copies values regardless of isCopyRequired
  protected void setValues(int index, double[] values, boolean isCopyRequired) {
    long offset = valuesOffset_ + SIZE_OF_VALUE_BYTES * getNumValues() * index;
    for (int i = 0; i < getNumValues(); i++) {
      mem_.putDouble(offset, values[i]);
      offset += SIZE_OF_VALUE_BYTES;
    }
  }

  @Override
  protected void updateValues(int index, double[] values) {
    long offset = valuesOffset_ + SIZE_OF_VALUE_BYTES * getNumValues() * index;
    for (int i = 0; i < numValues_; i++) {
      mem_.putDouble(offset, mem_.getDouble(offset) + values[i]);
      offset += SIZE_OF_VALUE_BYTES;
    }
  }

  @Override
  protected void setNotEmpty() {
    if (isEmpty_) {
      isEmpty_ = false;
      mem_.clearBits(FLAGS_BYTE, (byte) (1 << Flags.IS_EMPTY.ordinal()));
    }
  }

  @Override
  protected boolean isInSamplingMode() {
    return mem_.isAnyBitsSet(FLAGS_BYTE, (byte) (1 << Flags.IS_IN_SAMPLING_MODE.ordinal()));
  }

  // rebuild in the same memory assuming enough space
  @Override
  protected void rebuild(int newCapacity) {
    int currCapacity = getCurrentCapacity();
    int numValues = getNumValues();
    long[] keys = new long[currCapacity];
    double[] values = new double[currCapacity * numValues];
    mem_.getLongArray(keysOffset_, keys, 0, currCapacity);
    mem_.getDoubleArray(valuesOffset_, values, 0, currCapacity * numValues);
    mem_.clear(keysOffset_, SIZE_OF_KEY_BYTES * newCapacity + SIZE_OF_VALUE_BYTES * newCapacity * numValues);
    mem_.putInt(RETAINED_ENTRIES_INT, 0);
    mem_.putByte(LG_CUR_CAPACITY_BYTE, (byte)Integer.numberOfTrailingZeros(newCapacity));
    valuesOffset_ = keysOffset_ + SIZE_OF_KEY_BYTES * newCapacity;
    mask_ = newCapacity - 1;
    for (int i = 0; i < keys.length; i++) {
      if (keys[i] != 0 && keys[i] < theta_) {
        insert(keys[i], Arrays.copyOfRange(values, i * numValues, (i + 1) * numValues));
      }
    }
    setRebuildThreshold();
  }

  @Override
  ArrayOfDoublesSketchIterator iterator() {
    return new DirectArrayOfDoublesSketchIterator(mem_, keysOffset_, getCurrentCapacity(), numValues_);
  }

}
