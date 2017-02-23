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
public abstract class Memory {

  //BYTE BUFFER

  /**
   * Provides read-only access to the backing store of the given ByteBuffer.
   * Even if the given <i>ByteBuffer</i> is writable, the returned <i>Memory</i> will still be a
   * read-only instance.
   * @param byteBuffer the given <i>ByteBuffer</i>
   * @return a <i>Memory</i> object
   */
  public static Memory wrap(final ByteBuffer byteBuffer) {
    return AccessByteBuffer.writableWrap(byteBuffer, true);
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
  //@SuppressWarnings("unused")
  public static Memory map(final File file, final long offsetBytes, final long capacityBytes) {
    //-> MapDR  //TODO
    return null;
  }

  //ACCESS PRIMITIVE ARRAYS

  public static Memory wrap(final boolean[] arr) {
    return new WritableMemoryImpl(
        arr, ARRAY_BOOLEAN_BASE_OFFSET, arr.length << BOOLEAN_SHIFT, true);
  }

  public static Memory wrap(final byte[] arr) {
    return new WritableMemoryImpl(
        arr, ARRAY_BYTE_BASE_OFFSET, arr.length << BYTE_SHIFT, true);
  }

  public static Memory wrap(final char[] arr) {
    return new WritableMemoryImpl(
        arr, ARRAY_CHAR_BASE_OFFSET,arr.length << CHAR_SHIFT, true);
  }

  public static Memory wrap(final short[] arr) {
    return new WritableMemoryImpl(
        arr, ARRAY_SHORT_BASE_OFFSET, arr.length << SHORT_SHIFT, true);
  }

  public static Memory wrap(final int[] arr) {
    return new WritableMemoryImpl(
        arr, ARRAY_INT_BASE_OFFSET, arr.length << INT_SHIFT, true);
  }

  public static Memory wrap(final long[] arr) {
    return new WritableMemoryImpl(
        arr, ARRAY_LONG_BASE_OFFSET, arr.length << LONG_SHIFT, true);
  }

  public static Memory wrap(final float[] arr) {
    return new WritableMemoryImpl(
        arr, ARRAY_FLOAT_BASE_OFFSET, arr.length << FLOAT_SHIFT, true);
  }

  public static Memory wrap(final double[] arr) {
    return new WritableMemoryImpl(
        arr, ARRAY_DOUBLE_BASE_OFFSET, arr.length << DOUBLE_SHIFT, true);
  }

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

  //Primitive Get Arrays

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

  //Plus a number of convenience read methods not listed


  public abstract boolean isValid(); //TODO Can elimiate this

}
