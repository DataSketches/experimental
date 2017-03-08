/*
 * Copyright 2017, Yahoo! Inc. Licensed under the terms of the
 * Apache License 2.0. See LICENSE file at the project root for terms.
 */

package com.yahoo.memory3;

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
public abstract class Memory {

  //ALLOCATE DIRECT MEMORY
  public static Memory allocateDirect(final long capacityBytes, final MemoryRequest memReq) {
    return AllocateDirect.allocDirect(capacityBytes, memReq);
  }

  public static Memory allocateDirect(final long capacityBytes) {
    return AllocateDirect.allocDirect(capacityBytes, null);
  }

  //BYTE BUFFER

  /**
   * Provides access to the backing store of the given ByteBuffer.
   * If the given <i>ByteBuffer</i> is read-only, the returned <i>Memory</i> will also be a
   * read-only instance.
   * @param byteBuffer the given <i>ByteBuffer</i>
   * @return a <i>Memory</i> object
   */
  public static Memory wrap(final ByteBuffer byteBuffer) {
    return AccessByteBuffer.wrap(byteBuffer);
  }

  //MAP
  @SuppressWarnings("unused")
  public static Memory map(final File file, final long offsetBytes, final long capacityBytes) {
    //-> MapDR  //TODO
    return null;
  }

  //ALLOCATE HEAP VIA AUTOMATIC BYTE ARRAY
  public static Memory allocate(final int capacityBytes) {
    final byte[] unsafeObj = new byte[capacityBytes];
    return new MemoryImpl(unsafeObj, ARRAY_BYTE_BASE_OFFSET, capacityBytes);
  }

  //ACCESS PRIMITIVE ARRAYS

  public static Memory wrap(final boolean[] arr) {
    return new MemoryImpl(arr, ARRAY_BOOLEAN_BASE_OFFSET, arr.length << BOOLEAN_SHIFT);
  }

  public static Memory wrap(final byte[] arr) {
    return new MemoryImpl(arr, ARRAY_BYTE_BASE_OFFSET, arr.length << BYTE_SHIFT);
  }

  public static Memory wrap(final char[] arr) {
    return new MemoryImpl(arr, ARRAY_CHAR_BASE_OFFSET,arr.length << CHAR_SHIFT);
  }

  public static Memory wrap(final short[] arr) {
    return new MemoryImpl(arr, ARRAY_SHORT_BASE_OFFSET, arr.length << SHORT_SHIFT);
  }

  public static Memory wrap(final int[] arr) {
    return new MemoryImpl(arr, ARRAY_INT_BASE_OFFSET, arr.length << INT_SHIFT);
  }

  public static Memory wrap(final long[] arr) {
    return new MemoryImpl(arr, ARRAY_LONG_BASE_OFFSET, arr.length << LONG_SHIFT);
  }

  public static Memory wrap(final float[] arr) {
    return new MemoryImpl(arr, ARRAY_FLOAT_BASE_OFFSET, arr.length << FLOAT_SHIFT);
  }

  public static Memory wrap(final double[] arr) {
    return new MemoryImpl(arr, ARRAY_DOUBLE_BASE_OFFSET, arr.length << DOUBLE_SHIFT);
  }

  //AS READ ONLY
  public abstract Memory asReadOnly();

  //REGIONS
  public abstract Memory region(long offsetBytes, long capacityBytes);

  //END OF CONSTRUCTOR-TYPE METHODS

  //PRIMITIVE GETS
  /**
   * Gets the boolean value at the given offset
   * @param offsetBytes offset bytes relative to this Memory start
   * @return the boolean at the given offset
   */
  public abstract boolean getBoolean(long offsetBytes);

  /**
   * Gets the byte value at the given offset
   * @param offsetBytes offset bytes relative to this Memory start
   * @return the byte at the given offset
   */
  public abstract byte getByte(long offsetBytes);

  /**
   * Gets the char value at the given offset
   * @param offsetBytes offset bytes relative to this Memory start
   * @return the char at the given offset
   */
  public abstract char getChar(long offsetBytes);

  /**
   * Gets the short value at the given offset
   * @param offsetBytes offset bytes relative to this Memory start
   * @return the short at the given offset
   */
  public abstract short getShort(long offsetBytes);

  /**
   * Gets the int value at the given offset
   * @param offsetBytes offset bytes relative to this Memory start
   * @return the int at the given offset
   */
  public abstract int getInt(long offsetBytes);

  /**
   * Gets the long value at the given offset
   * @param offsetBytes offset bytes relative to this Memory start
   * @return the long at the given offset
   */
  public abstract long getLong(long offsetBytes);

  /**
   * Gets the float value at the given offset
   * @param offsetBytes offset bytes relative to this Memory start
   * @return the float at the given offset
   */
  public abstract float getFloat(long offsetBytes);

  /**
   * Gets the double value at the given offset
   * @param offsetBytes offset bytes relative to this Memory start
   * @return the double at the given offset
   */
  public abstract double getDouble(long offsetBytes);

  //PRIMITIVE GET ARRAYS

  /**
   * Gets the boolean array at the given offset
   * @param offsetBytes offset bytes relative to this Memory start
   * @param dstArray The preallocated destination array.
   * @param dstOffset offset in array units
   * @param length number of array units to transfer
   */
  public abstract void getBooleanArray(long offsetBytes, boolean[] dstArray, int dstOffset,
      int length);

  //plus 6 more
  /**
   * Gets the long array at the given offset
   * @param offsetBytes offset bytes relative to this Memory start
   * @param dstArray The preallocated destination array.
   * @param dstOffset offset in array units
   * @param length number of array units to transfer
   */
  public abstract void getLongArray(long offsetBytes, long[] dstArray, int dstOffset, int length);

  //PRIMITIVE PUTS

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

  //PRIMITIVE PUT ARRAYS

  /**
   * Puts the boolean array at the given offset
   * @param offsetBytes offset bytes relative to this Memory start
   * @param srcArray The source array.
   * @param srcOffset offset in array units
   * @param length number of array units to transfer
   */
  public abstract void putBooleanArray(final long offsetBytes, final boolean[] srcArray,
      final int srcOffset, final int length);

  /**
   * Puts the long array at the given offset
   * @param offsetBytes offset bytes relative to this Memory start
   * @param srcArray The source array.
   * @param srcOffset offset in array units
   * @param length number of array units to transfer
   */
  public abstract void putLongArray(final long offsetBytes, final long[] srcArray,
      final int srcOffset, final int length);

  //OTHER

  public abstract MemoryRequest getMemoryRequest();

  public abstract void freeMemory();

}