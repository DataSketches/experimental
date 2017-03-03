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
public abstract class WritableMemory extends Memory implements AutoCloseable {

  //ALLOCATE DIRECT WRITABLE MEMORY
  public static WritableMemory allocateDirect(
      final long capacityBytes,
      final MemoryRequest memReq) {
    return AllocateDirect.allocDirect(capacityBytes, memReq);
  }

  public static WritableMemory allocateDirect(final long capacityBytes) {
    return AllocateDirect.allocDirect(capacityBytes, null);
  }

  //BYTE BUFFER
  /**
   * Provides writable access to the backing store of the given ByteBuffer.
   * If the given <i>ByteBuffer</i> is read-only and if asserts are enabled, any write operation
   * will throw an assertion error.
   * @param byteBuffer the given <i>ByteBuffer</i>
   * @return a <i>WritableMemory</i> object
   */
  public static WritableMemory writableWrap(final ByteBuffer byteBuffer) {
    return AccessByteBuffer.writableWrap(byteBuffer, false);
  }

  //MAP
  /**
   * Provides writable, memory-mapped access to the newly allocated native backing store for the
   * given File. If the given <i>File</i> is read-only, any write operation will throw an
   * exception or an assertion error, if asserts are enabled.
   * @param file the given <i>File</i>
   * @param offsetBytes offset into the file in bytes.
   * @param capacityBytes the capacity of the memory-mapped buffer space in bytes.
   * @return a <i>WritableMemory</i> object
   * @throws Exception file not found or RuntimeException, etc.
   */
  public static WritableMemory writableMap(final File file, final long offsetBytes,
      final long capacityBytes) throws Exception {
    return AllocateDirectMap.getInstance(file, offsetBytes, capacityBytes);
  }

  /**
   * Applies only to mapped files. Otherwise is a no-op.
   * Loads content into physical memory. This method makes a best effort to ensure that, when it
   * returns, this buffer's content is resident in physical memory. Invoking this method may cause
   * some number of page faults and I/O operations to occur.
   *
   * @see <a href="https://docs.oracle.com/javase/8/docs/api/java/nio/MappedByteBuffer.html#load--">
   * java/nio/MappedByteBuffer.load</a>
   */
  public abstract void load();

  /**
   * Applies only to mapped files. Otherwise always returns false.
   * Tells whether or not the content is resident in physical memory. A return value of true implies
   * that it is highly likely that all of the data in this buffer is resident in physical memory and
   * may therefore be accessed without incurring any virtual-memory page faults or I/O operations. A
   * return value of false does not necessarily imply that the content is not resident in physical
   * memory. The returned value is a hint, rather than a guarantee, because the underlying operating
   * system may have paged out some of the buffer's data by the time that an invocation of this
   * method returns.
   *
   * @return true if loaded
   *
   * @see <a href=
   * "https://docs.oracle.com/javase/8/docs/api/java/nio/MappedByteBuffer.html#isLoaded--"> java
   * /nio/MappedByteBuffer.isLoaded</a>
   */
  public abstract boolean isLoaded();

  /**
   * Applies only to mapped files. Otherwise is a no-op.
   * Forces any changes made to this content to be written to the storage device containing the
   * mapped file.
   *
   * <p>
   * If the file mapped into this buffer resides on a local storage device then when this method
   * returns it is guaranteed that all changes made to the buffer since it was created, or since
   * this method was last invoked, will have been written to that device.
   * </p>
   *
   * <p>
   * If the file does not reside on a local device then no such guarantee is made.
   * </p>
   *
   * <p>
   * If this buffer was not mapped in read/write mode
   * (java.nio.channels.FileChannel.MapMode.READ_WRITE) then invoking this method has no effect.
   * </p>
   *
   * @see <a href=
   * "https://docs.oracle.com/javase/8/docs/api/java/nio/MappedByteBuffer.html#force--"> java/
   * nio/MappedByteBuffer.force</a>
   */
  public abstract void force();

  //END OF MAP

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
   * @param offsetBytes offset bytes relative to this <i>WritableMemory</i> start
   * @param value the value to put
   */
  public abstract void putBoolean(long offsetBytes, boolean value);

  /**
   * Puts the byte value at the given offset
   * @param offsetBytes offset bytes relative to this <i>WritableMemory</i> start
   * @param value the value to put
   */
  public abstract void putByte(long offsetBytes, byte value);

  /**
   * Puts the char value at the given offset
   * @param offsetBytes offset bytes relative to this <i>WritableMemory</i> start
   * @param value the value to put
   */
  public abstract void putChar(long offsetBytes, char value);

  /**
   * Puts the int value at the given offset
   * @param offsetBytes offset bytes relative to this <i>WritableMemory</i> start
   * @param value the value to put
   */
  public abstract void putInt(long offsetBytes, int value);

  /**
   * Puts the long value at the given offset
   * @param offsetBytes offset bytes relative to this <i>WritableMemory</i> start
   * @param value the value to put
   */
  public abstract void putLong(long offsetBytes, long value);

  /**
   * Puts the float value at the given offset
   * @param offsetBytes offset bytes relative to this <i>WritableMemory</i> start
   * @param value the value to put
   */
  public abstract void putFloat(long offsetBytes, float value);

  /**
   * Puts the double value at the given offset
   * @param offsetBytes offset bytes relative to this <i>WritableMemory</i> start
   * @param value the value to put
   */
  public abstract void putDouble(long offsetBytes, double value);

  //Primitive Put Arrays

  /**
   * Puts the boolean array at the given offset
   * @param offsetBytes offset bytes relative to this <i>WritableMemory</i> start
   * @param srcArray The source array.
   * @param srcOffset offset in array units
   * @param length number of array units to transfer
   */
  public abstract void putBooleanArray(long offsetBytes, boolean[] srcArray, int srcOffset,
      int length);

  /**
   * Puts the byte array at the given offset
   * @param offsetBytes offset bytes relative to this <i>WritableMemory</i> start
   * @param srcArray The source array.
   * @param srcOffset offset in array units
   * @param length number of array units to transfer
   */
  public abstract void putByteArray(long offsetBytes, byte[] srcArray, int srcOffset,
      int length);

  /**
   * Puts the char array at the given offset
   * @param offsetBytes offset bytes relative to this <i>WritableMemory</i> start
   * @param srcArray The source array.
   * @param srcOffset offset in array units
   * @param length number of array units to transfer
   */
  public abstract void putCharArray(long offsetBytes, char[] srcArray, int srcOffset,
      int length);

  /**
   * Puts the long array at the given offset
   * @param offsetBytes offset bytes relative to this <i>WritableMemory</i> start
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

  @Override
  public abstract void close();

}
