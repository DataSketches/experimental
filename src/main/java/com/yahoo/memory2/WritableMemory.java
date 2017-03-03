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

  /**
   * Returns a read-only version of this memory
   * @return a read-only version of this memory
   */
  public abstract Memory asReadOnly();

  //Regions
  public abstract WritableMemory writableRegion(long offsetBytes, long capacityBytes);

  //END OF CONSTRUCTOR-TYPE METHODS

  //PRIMITIVE putXXX() and putXXXArray() //TODO

  /**
   * Puts the boolean value at the given offset
   * @param offsetBytes offset bytes relative to this <i>WritableMemory</i> start
   * @param value the value to put
   */
  public abstract void putBoolean(long offsetBytes, boolean value);

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
   * Puts the byte value at the given offset
   * @param offsetBytes offset bytes relative to this <i>WritableMemory</i> start
   * @param value the value to put
   */
  public abstract void putByte(long offsetBytes, byte value);

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
   * Puts the char value at the given offset
   * @param offsetBytes offset bytes relative to this <i>WritableMemory</i> start
   * @param value the value to put
   */
  public abstract void putChar(long offsetBytes, char value);

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
   * Puts the double value at the given offset
   * @param offsetBytes offset bytes relative to this <i>WritableMemory</i> start
   * @param value the value to put
   */
  public abstract void putDouble(long offsetBytes, double value);

  /**
   * Puts the double array at the given offset
   * @param offsetBytes offset bytes relative to this <i>WritableMemory</i> start
   * @param srcArray The source array.
   * @param srcOffset offset in array units
   * @param length number of array units to transfer
   */
  public abstract void putDoubleArray(long offsetBytes, double[] srcArray,
      final int srcOffset, final int length);

  /**
   * Puts the float value at the given offset
   * @param offsetBytes offset bytes relative to this <i>WritableMemory</i> start
   * @param value the value to put
   */
  public abstract void putFloat(long offsetBytes, float value);

  /**
   * Puts the float array at the given offset
   * @param offsetBytes offset bytes relative to this <i>WritableMemory</i> start
   * @param srcArray The source array.
   * @param srcOffset offset in array units
   * @param length number of array units to transfer
   */
  public abstract void putFloatArray(long offsetBytes, float[] srcArray,
      final int srcOffset, final int length);

  /**
   * Puts the int value at the given offset
   * @param offsetBytes offset bytes relative to this <i>WritableMemory</i> start
   * @param value the value to put
   */
  public abstract void putInt(long offsetBytes, int value);

  /**
   * Puts the int array at the given offset
   * @param offsetBytes offset bytes relative to this <i>WritableMemory</i> start
   * @param srcArray The source array.
   * @param srcOffset offset in array units
   * @param length number of array units to transfer
   */
  public abstract void putIntArray(long offsetBytes, int[] srcArray,
      final int srcOffset, final int length);

  /**
   * Puts the long value at the given offset
   * @param offsetBytes offset bytes relative to this <i>WritableMemory</i> start
   * @param value the value to put
   */
  public abstract void putLong(long offsetBytes, long value);

  /**
   * Puts the long array at the given offset
   * @param offsetBytes offset bytes relative to this <i>WritableMemory</i> start
   * @param srcArray The source array.
   * @param srcOffset offset in array units
   * @param length number of array units to transfer
   */
  public abstract void putLongArray(long offsetBytes, long[] srcArray,
      final int srcOffset, final int length);

  /**
   * Puts the short value at the given offset
   * @param offsetBytes offset bytes relative to this <i>WritableMemory</i> start
   * @param value the value to put
   */
  public abstract void putShort(long offsetBytes, short value);

  /**
   * Puts the short array at the given offset
   * @param offsetBytes offset bytes relative to this <i>WritableMemory</i> start
   * @param srcArray The source array.
   * @param srcOffset offset in array units
   * @param length number of array units to transfer
   */
  public abstract void putShortArray(long offsetBytes, short[] srcArray,
      final int srcOffset, final int length);

  //Atomic Methods //TODO

  /**
   * Atomically adds the given value to the long located at offsetBytes.
   * @param offsetBytes offset bytes relative to this Memory start
   * @param delta the amount to add
   * @return the modified value
   */
  public abstract int addAndGetLong(long offsetBytes, long delta);

  /**
   * Atomically sets the current value at the memory location to the given updated value
   * if and only if the current value {@code ==} the expected value.
   * @param offsetBytes offset bytes relative to this Memory start
   * @param expect the expected value
   * @param update the new value
   * @return {@code true} if successful. False return indicates that
   * the current value at the memory location was not equal to the expected value.
   */
  public abstract boolean compareAndSwapLong(long offsetBytes, long expect, long update);

  /**
   * Atomically exchanges the given value with the current value located at offsetBytes.
   * @param offsetBytes offset bytes relative to this Memory start
   * @param newValue new value
   * @return the previous value
   */
  public abstract long getAndSetLong(long offsetBytes, long newValue);

  //OTHER WRITE METHODS //TODO

  /**
   * Clears all bytes of this Memory to zero
   */
  public abstract void clear();

  /**
   * Clears a portion of this Memory to zero.
   * @param offsetBytes offset bytes relative to this Memory start
   * @param lengthBytes the length in bytes
   */
  public abstract void clear(long offsetBytes, long lengthBytes);

  /**
   * Clears the bits defined by the bitMask
   * @param offsetBytes offset bytes relative to this Memory start.
   * @param bitMask the bits set to one will be cleared
   */
  public abstract void clearBits(long offsetBytes, byte bitMask);

  /**
   * Fills all bytes of this Memory region to the given byte value.
   * @param value the given byte value
   */
  public abstract void fill(byte value);

  /**
   * Fills a portion of this Memory region to the given byte value.
   * @param offsetBytes offset bytes relative to this Memory start
   * @param lengthBytes the length in bytes
   * @param value the given byte value
   */
  public abstract void fill(long offsetBytes, long lengthBytes, byte value);

  //OTHER //TODO

  /**
   * Returns a MemoryRequest or null
   * @return a MemoryRequest or null
   */
  public abstract MemoryRequest getMemoryRequest();

  @Override
  public abstract void close();

}
