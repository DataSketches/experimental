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

//@SuppressWarnings("unused")
public abstract class WritableMemory extends Memory {

  //ALLOCATE DIRECT WRITABLE MEMORY
  public static WritableMemory allocateDirect(
      final long capacityBytes, final MemoryRequest memReq) {
    return AllocateDirect.allocDirect(capacityBytes, memReq);
  }

  public static WritableMemory allocateDirect(final long capacityBytes) {
    return AllocateDirect.allocDirect(capacityBytes, null);
  }

  //BYTE BUFFER
  /**
   * Provides writable access to the backing store of the given ByteBuffer.
   * If the given <i>ByteBuffer</i> is read-only, and writes are attempted, exceptions
   * will be thrown.
   * @param byteBuffer the given <i>ByteBuffer</i>
   * @return a <i>Memory</i> object
   */
  public static WritableMemory writableWrap(final ByteBuffer byteBuffer) {
    if (byteBuffer.isReadOnly()) {
      throw new IllegalArgumentException("Given ByteBuffer is Read-Only.");
    }
    return AccessByteBuffer.writableWrap(byteBuffer, false);
  }

  //MAP
  /**
   * Provides read-only access to the backing store of the given MemoryMappedFile.
   * Even if the given <i>File</i> is writable, the returned <i>Memory</i> will still be a
   * read-only instance.
   * @param file the given <i>File</i>
   * @param offsetBytes offset into the file in bytes.
   * @param capacityBytes the capacity of the memory-mapped buffer space in bytes.
   * @return a <i>Memory</i> object
   */
  public static WritableMemory WritableMap(final File file, final long offsetBytes,
      final long capacityBytes) {
    //-> MapDW
    return null;
  }

  //ALLOCATE HEAP VIA AUTOMATIC BYTE ARRAY
  public static WritableMemory allocate(final int capacityBytes) {
    final byte[] unsafeObj = new byte[capacityBytes];
    return new WritableMemoryImpl(unsafeObj, ARRAY_BYTE_BASE_OFFSET, capacityBytes, false);
  }

  //ACCESS PRIMITIVE ARRAYS for write

  public static WritableMemory writableWrap(final boolean[] arr) {
    return new WritableMemoryImpl(
        arr, ARRAY_BOOLEAN_BASE_OFFSET, arr.length << BOOLEAN_SHIFT, false);
  }

  public static WritableMemory writableWrap(final byte[] arr) {
    return new WritableMemoryImpl(
        arr, ARRAY_BYTE_BASE_OFFSET, arr.length << BYTE_SHIFT, false);
  }

  public static WritableMemory writableWrap(final char[] arr) {
    return new WritableMemoryImpl(
        arr, ARRAY_CHAR_BASE_OFFSET, arr.length << CHAR_SHIFT, false);
  }

  public static WritableMemory writableWrap(final short[] arr) {
    return new WritableMemoryImpl(
        arr, ARRAY_SHORT_BASE_OFFSET, arr.length << SHORT_SHIFT, false);
  }

  public static WritableMemory writableWrap(final int[] arr) {
    return new WritableMemoryImpl(
        arr, ARRAY_INT_BASE_OFFSET, arr.length << INT_SHIFT, false);
  }

  public static WritableMemory writableWrap(final long[] arr) {
    return new WritableMemoryImpl(
        arr, ARRAY_LONG_BASE_OFFSET, arr.length << LONG_SHIFT, false);
  }

  public static WritableMemory writableWrap(final float[] arr) {
    return new WritableMemoryImpl(
        arr, ARRAY_FLOAT_BASE_OFFSET, arr.length << FLOAT_SHIFT, false);
  }

  public static WritableMemory writableWrap(final double[] arr) {
    return new WritableMemoryImpl(
        arr, ARRAY_DOUBLE_BASE_OFFSET, arr.length << DOUBLE_SHIFT, false);
  }

  public abstract Memory asReadOnly();

  //Regions
  public abstract WritableMemory writableRegion(long offsetBytes, long capacityBytes);


  //END OF CONSTRUCTOR-TYPE METHODS

  //Primitive Puts

  /**
   * Puts the boolean value at the given offset
   * @param offsetBytes offset bytes relative to this Memory start
   * @param value the value to put
   */
  public abstract void putBoolean(long offsetBytes, boolean value);

  /**
   * Puts the byte value at the given offset
   * @param offsetBytes offset bytes relative to this Memory start
   * @param value the value to put
   */
  public abstract void putByte(long offsetBytes, byte value);

  /**
   * Puts the char value at the given offset
   * @param offsetBytes offset bytes relative to this Memory start
   * @param value the value to put
   */
  public abstract void putChar(long offsetBytes, char value);

  /**
   * Puts the int value at the given offset
   * @param offsetBytes offset bytes relative to this Memory start
   * @param value the value to put
   */
  public abstract void putInt(long offsetBytes, int value);

  /**
   * Puts the long value at the given offset
   * @param offsetBytes offset bytes relative to this Memory start
   * @param value the value to put
   */
  public abstract void putLong(long offsetBytes, long value);

  /**
   * Puts the float value at the given offset
   * @param offsetBytes offset bytes relative to this Memory start
   * @param value the value to put
   */
  public abstract void putFloat(long offsetBytes, float value);

  /**
   * Puts the double value at the given offset
   * @param offsetBytes offset bytes relative to this Memory start
   * @param value the value to put
   */
  public abstract void putDouble(long offsetBytes, double value);

  //Primitive Put Arrays

  /**
   * Puts the boolean array at the given offset
   * @param offsetBytes offset bytes relative to this Memory start
   * @param srcArray The source array.
   * @param srcOffset offset in array units
   * @param length number of array units to transfer
   */
  public abstract void putBooleanArray(long offsetBytes, boolean[] srcArray, int srcOffset,
      int length);

  /**
   * Puts the long array at the given offset
   * @param offsetBytes offset bytes relative to this Memory start
   * @param srcArray The source array.
   * @param srcOffset offset in array units
   * @param length number of array units to transfer
   */
  public abstract void putLongArray(long offsetBytes, long[] srcArray,
      final int srcOffset, final int length);

  //Plus a number of convenience write methods not listed
  // e.g., clean, fill, MemoryRequest, etc.

  //OTHER

  public abstract MemoryRequest getMemoryRequest();

  public abstract void freeMemory();
}
