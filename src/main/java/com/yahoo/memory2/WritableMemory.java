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
public abstract class WritableMemory extends Memory {

  public abstract Memory asReadOnly();

  //ACCESS PRIMITIVE ARRAYS for write

  public static WritableMemory writableWrap(final boolean[] arr) {
    return new WritableMemoryImpl(0L, arr, ARRAY_BOOLEAN_BASE_OFFSET, null, 0L,
        arr.length << BOOLEAN_SHIFT, null);
  }

  public static WritableMemory writableWrap(final byte[] arr) {
    return new WritableMemoryImpl(0L, arr, ARRAY_BYTE_BASE_OFFSET, null, 0L,
        arr.length << BYTE_SHIFT, null);
  }

  public static WritableMemory writableWrap(final char[] arr) {
    return new WritableMemoryImpl(0L, arr, ARRAY_CHAR_BASE_OFFSET, null, 0L,
        arr.length << CHAR_SHIFT, null);
  }

  public static WritableMemory writableWrap(final short[] arr) {
    return new WritableMemoryImpl(0L, arr, ARRAY_SHORT_BASE_OFFSET, null, 0L,
        arr.length << SHORT_SHIFT, null);
  }

  public static WritableMemory writableWrap(final int[] arr) {
    return new WritableMemoryImpl(0L, arr, ARRAY_INT_BASE_OFFSET, null, 0L,
        arr.length << INT_SHIFT, null);
  }

  public static WritableMemory writableWrap(final long[] arr) {
    return new WritableMemoryImpl(0L, arr, ARRAY_LONG_BASE_OFFSET, null, 0L,
        arr.length << LONG_SHIFT, null);
  }

  public static WritableMemory writableWrap(final float[] arr) {
    return new WritableMemoryImpl(0L, arr, ARRAY_FLOAT_BASE_OFFSET, null, 0L,
        arr.length << FLOAT_SHIFT, null);
  }

  public static WritableMemory writableWrap(final double[] arr) {
    return new WritableMemoryImpl(0L, arr, ARRAY_DOUBLE_BASE_OFFSET, null, 0L,
        arr.length << DOUBLE_SHIFT, null);
  }


  //Allocations using native memory and heap

  //ALLOCATE DIRECT
  public static WritableMemory allocateDirect(final long capacity, final MemoryRequest memReq) {
    return AllocateDirect.allocDirect(capacity, memReq);
  }

  //ALLOCATE HEAP VIA AUTOMATIC BYTE ARRAY
  public static WritableMemory allocate(final int capacity, final MemoryRequest memReq) {
    return WritableMemoryImpl.allocateArray(capacity, memReq);
  }

  //ByteBuffer

  /**
   * @param byteBuf blah
   * @return blah
   */
  public static WritableMemory writableWrap(final ByteBuffer byteBuf) {
    return WritableMemoryImpl.writableWrap(byteBuf);
  }

  //Map

  public static WritableMemory WritableMap(final File file, final long offset,
      final long capacity) {
    //-> MapDW
    return null;
  }

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

  //Primitive Put Arrays


  //Plus a number of convenience write methods not listed
  // e.g., clean, fill, MemoryRequest, etc.

  public abstract MemoryRequest getMemoryRequest();

  public void freeMemory() {}
}
