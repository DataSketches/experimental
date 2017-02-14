/*
 * Copyright 2017, Yahoo! Inc. Licensed under the terms of the
 * Apache License 2.0. See LICENSE file at the project root for terms.
 */

package com.yahoo.memory2;

import static com.yahoo.memory.UnsafeUtil.ARRAY_BOOLEAN_BASE_OFFSET;
import static com.yahoo.memory.UnsafeUtil.ARRAY_BYTE_BASE_OFFSET;
import static com.yahoo.memory.UnsafeUtil.ARRAY_CHAR_BASE_OFFSET;
import static com.yahoo.memory.UnsafeUtil.ARRAY_DOUBLE_BASE_OFFSET;
import static com.yahoo.memory.UnsafeUtil.ARRAY_FLOAT_BASE_OFFSET;
import static com.yahoo.memory.UnsafeUtil.ARRAY_INT_BASE_OFFSET;
import static com.yahoo.memory.UnsafeUtil.ARRAY_LONG_BASE_OFFSET;
import static com.yahoo.memory.UnsafeUtil.ARRAY_SHORT_BASE_OFFSET;
import static com.yahoo.memory.UnsafeUtil.BOOLEAN_SHIFT;
import static com.yahoo.memory.UnsafeUtil.BYTE_SHIFT;
import static com.yahoo.memory.UnsafeUtil.CHAR_SHIFT;
import static com.yahoo.memory.UnsafeUtil.DOUBLE_SHIFT;
import static com.yahoo.memory.UnsafeUtil.FLOAT_SHIFT;
import static com.yahoo.memory.UnsafeUtil.INT_SHIFT;
import static com.yahoo.memory.UnsafeUtil.LONG_SHIFT;
import static com.yahoo.memory.UnsafeUtil.SHORT_SHIFT;

import java.io.File;
import java.nio.ByteBuffer;

@SuppressWarnings("unused")
public abstract class WritableMemory {

  //ACCESS PRIMITIVE ARRAYS

  public static Memory wrap(final boolean[] arr) {
    return new MemoryROImpl(0L, arr, ARRAY_BOOLEAN_BASE_OFFSET, 0L, arr.length << BOOLEAN_SHIFT);
  }

  public static Memory wrap(final byte[] arr) {
    return new MemoryROImpl(0L, arr, ARRAY_BYTE_BASE_OFFSET, 0L, arr.length << BYTE_SHIFT);
  }

  public static Memory wrap(final char[] arr) {
    return new MemoryROImpl(0L, arr, ARRAY_CHAR_BASE_OFFSET, 0L, arr.length << CHAR_SHIFT);
  }

  public static Memory wrap(final short[] arr) {
    return new MemoryROImpl(0L, arr, ARRAY_SHORT_BASE_OFFSET, 0L, arr.length << SHORT_SHIFT);
  }

  public static Memory wrap(final int[] arr) {
    return new MemoryROImpl(0L, arr, ARRAY_INT_BASE_OFFSET, 0L, arr.length << INT_SHIFT);
  }

  public static Memory wrap(final long[] arr) {
    return new MemoryROImpl(0L, arr, ARRAY_LONG_BASE_OFFSET, 0L, arr.length << LONG_SHIFT);
  }

  public static Memory wrap(final float[] arr) {
    return new MemoryROImpl(0L, arr, ARRAY_FLOAT_BASE_OFFSET, 0L, arr.length << FLOAT_SHIFT);
  }

  public static Memory wrap(final double[] arr) {
    return new MemoryROImpl(0L, arr, ARRAY_DOUBLE_BASE_OFFSET, 0L, arr.length << DOUBLE_SHIFT);
  }


  //Allocations using native memory and heap

  //ALLOCATE DIRECT
  public static WritableMemory allocateDirect(final long capacity, final MemoryRequest memReq) {
    return WritableDirect.allocDirect(capacity, memReq);
  }

  //ALLOCATE HEAP VIA AUTOMATIC BYTE ARRAY
  public static WritableMemory allocate(final int capacity, final MemoryRequest memReq) {
    return WritableMemoryImpl.allocateArray(capacity, memReq);
  }

  //ByteBuffer

  /**
   * @param bb blah
   * @return blah
   */
  public static WritableMemory wrap(final ByteBuffer bb) { //could end up RO or W
    //if BB is RO Direct -> MemoryBBDR //throws exception at runtime
    //if BB is RO Heap -> MemoryBBHR  //throws exception at runtime
    //if BB is W Direct -> MemoryBBDW
    //if BB is W Heap -> MemoryBBHW
    return null;
  }

  //Map

  public static WritableMemory mapWritable(final File file, final long offset,
      final long capacity) {
    //-> MapDW
    return null;
  }

  //Primitive Gets

  /**
   * Gets the boolean value at the given offset
   * @param offsetBytes offset bytes relative to this Memory start
   * @return the boolean at the given offset
   */
  public abstract boolean getBoolean(final long offsetBytes);

  /**
   * Gets the byte value at the given offset
   * @param offsetBytes offset bytes relative to this Memory start
   * @return the byte at the given offset
   */
  public abstract byte getByte(final long offsetBytes);

  /**
   * Gets the char value at the given offset
   * @param offsetBytes offset bytes relative to this Memory start
   * @return the char at the given offset
   */
  public abstract char getChar(final long offsetBytes);

  /**
   * Gets the short value at the given offset
   * @param offsetBytes offset bytes relative to this Memory start
   * @return the short at the given offset
   */
  public abstract short getShort(final long offsetBytes);

  /**
   * Gets the int value at the given offset
   * @param offsetBytes offset bytes relative to this Memory start
   * @return the int at the given offset
   */
  public abstract int getInt(final long offsetBytes);

  /**
   * Gets the long value at the given offset
   * @param offsetBytes offset bytes relative to this Memory start
   * @return the long at the given offset
   */
  public abstract long getLong(final long offsetBytes);

  /**
   * Gets the float value at the given offset
   * @param offsetBytes offset bytes relative to this Memory start
   * @return the float at the given offset
   */
  public abstract float getFloat(final long offsetBytes);

  /**
   * Gets the double value at the given offset
   * @param offsetBytes offset bytes relative to this Memory start
   * @return the double at the given offset
   */
  public abstract double getDouble(final long offsetBytes);

  //Primitive Get Arrays

  /**
   * Gets the boolean array at the given offset
   * @param offsetBytes offset bytes relative to this Memory start
   * @param dstArray The preallocated destination array.
   * @param dstOffset offset in array units
   * @param length number of array units to transfer
   */
  public abstract void getBooleanArray(long offsetBytes, boolean[] dstArray, int dstOffset, int length);

  /**
   * Gets the long array at the given offset
   * @param offsetBytes offset bytes relative to this Memory start
   * @param dstArray The preallocated destination array.
   * @param dstOffset offset in array units
   * @param length number of array units to transfer
   */
  public abstract void getLongArray(long offsetBytes, long[] dstArray, int dstOffset, int length);

  //Primitive Puts

  /**
   * Puts the boolean value at the given offset
   * @param offsetBytes offset bytes relative to this Memory start
   * @param value the value to put
   */
  public abstract void putBoolean(final long offsetBytes, final boolean value);

  /**
   * Puts the byte value at the given offset
   * @param offsetBytes offset bytes relative to this Memory start
   * @param value the value to put
   */
  public abstract void putByte(final long offsetBytes, final byte value);

  /**
   * Puts the char value at the given offset
   * @param offsetBytes offset bytes relative to this Memory start
   * @param value the value to put
   */
  public abstract void putChar(final long offsetBytes, final char value);

  /**
   * Puts the int value at the given offset
   * @param offsetBytes offset bytes relative to this Memory start
   * @param value the value to put
   */
  public abstract void putInt(final long offsetBytes, final int value);

  /**
   * Puts the long value at the given offset
   * @param offsetBytes offset bytes relative to this Memory start
   * @param value the value to put
   */
  public abstract void putLong(final long offsetBytes, final long value);

  /**
   * Puts the float value at the given offset
   * @param offsetBytes offset bytes relative to this Memory start
   * @param value the value to put
   */
  public abstract void putFloat(final long offsetBytes, final float value);

  /**
   * Puts the double value at the given offset
   * @param offsetBytes offset bytes relative to this Memory start
   * @param value the value to put
   */
  public abstract void putDouble(final long offsetBytes, final double value);

  //Plus a number of convenience write methods not listed
  // e.g., clean, fill, MemoryRequest, etc.

  public abstract MemoryRequest getMemoryRequest();

  public void freeMemory() {

  }

}
