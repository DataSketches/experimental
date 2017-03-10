/*
 * Copyright 2017, Yahoo! Inc. Licensed under the terms of the
 * Apache License 2.0. See LICENSE file at the project root for terms.
 */

package com.yahoo.memory4;

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
import java.nio.ByteOrder;

/**
 * @author Lee Rhodes
 */
public abstract class WritableMemory {

  //BYTE BUFFER

  /**
   * Accesses the given ByteBuffer for write operations.
   * @param byteBuf the given ByteBuffer
   * @return the given ByteBuffer for write operations.
   */
  public static WritableMemory wrap(final ByteBuffer byteBuf) {
    if (byteBuf.isReadOnly()) {
      throw new ReadOnlyMemoryException("ByteBuffer is read-only.");
    }
    if (byteBuf.order() != ByteOrder.nativeOrder()) {
      throw new IllegalArgumentException(
          "Memory does not support " + (byteBuf.order().toString()));
    }
    final MemoryState state = new MemoryState();
    state.putByteBuffer(byteBuf);
    return AccessWritableByteBuffer.wrap(state);
  }

  //MAP
  /**
   * Allocates direct memory used to memory map files for write operations
   * (including those &gt; 2GB).
   * @param file the given file to map
   * @return MemoryHandler for managing this map
   * @throws Exception file not found or RuntimeException, etc.
   */
  public static WritableMemoryHandler map(final File file) throws Exception {
    return map(file, 0, file.length());
  }

  /**
   * Allocates direct memory used to memory map files for write operations
   * (including those &gt; 2GB).
   * @param file the given file to map
   * @param fileOffset the position in the given file
   * @param capacity the size of the allocated direct memory
   * @return MemoryHandler for managing this map
   * @throws Exception file not found or RuntimeException, etc.
   */
  public static WritableMemoryHandler map(final File file, final long fileOffset,
      final long capacity) throws Exception {
    final MemoryState state = new MemoryState();
    state.putFile(file);
    state.putFileOffset(fileOffset);
    state.putCapacity(capacity);
    return AllocateDirectWritableMap.map(state);
  }

  //ALLOCATE DIRECT

  /**
   * Allocates and provides access to capacityBytes directly in native (off-heap) memory
   * leveraging the WritableMemory API.
   * The allocated memory will be 8-byte aligned, but may not be page aligned.
   * @param capacityBytes the size of the desired memory in bytes
   * @return WritableMemoryHandler
   */
  public static WritableMemoryHandler allocateDirect(final long capacityBytes) {
    return allocateDirect(capacityBytes, null);
  }

  /**
   * Allocates and provides access to capacityBytes directly in native (off-heap) memory
   * leveraging the WritableMemory API.
   * The allocated memory will be 8-byte aligned, but may not be page aligned.
   * @param capacityBytes the size of the desired memory in bytes
   * @param memReq optional callback
   * @return WritableMemoryHandler
   */
  public static WritableMemoryHandler allocateDirect(final long capacityBytes,
      final MemoryRequest memReq) {
    final MemoryState state = new MemoryState();
    state.putCapacity(capacityBytes);
    state.putMemoryRequest(memReq);
    return (WritableMemoryHandler) AllocateDirect.allocDirect(state);
  }

  //REGIONS

  public abstract WritableMemory region(long offsetBytes, long capacityBytes);

  /**
   * Returns a read-only version of this memory
   * @return a read-only version of this memory
   */
  public abstract Memory asReadOnly();

  //ALLOCATE HEAP VIA AUTOMATIC BYTE ARRAY
  /**
   * Creates on-heap WritableMemory with the given capacity
   * @param capacityBytes the given capacity in bytes
   * @return WritableMemory for write operations
   */
  public static WritableMemory allocate(final int capacityBytes) {
    final MemoryState state = new MemoryState();
    state.putUnsafeObject(new byte[capacityBytes]);
    state.putUnsafeObjectHeader(ARRAY_BYTE_BASE_OFFSET);
    state.putCapacity(capacityBytes);
    return new WritableMemoryImpl(state);
  }

  //ACCESS PRIMITIVE HEAP ARRAYS for write

  /**
   * Wraps the given primitive array for write operations
   * @param arr the given primitive array
   * @return WritableMemory for write operations
   */
  public static WritableMemory wrap(final boolean[] arr) {
    final MemoryState state = new MemoryState();
    state.putUnsafeObject(arr);
    state.putUnsafeObjectHeader(ARRAY_BOOLEAN_BASE_OFFSET);
    state.putCapacity(arr.length << BOOLEAN_SHIFT);
    return new WritableMemoryImpl(state);
  }

  /**
   * Wraps the given primitive array for write operations
   * @param arr the given primitive array
   * @return WritableMemory for write operations
   */
  public static WritableMemory wrap(final byte[] arr) {
    final MemoryState state = new MemoryState();
    state.putUnsafeObject(arr);
    state.putUnsafeObjectHeader(ARRAY_BYTE_BASE_OFFSET);
    state.putCapacity(arr.length << BYTE_SHIFT);
    return new WritableMemoryImpl(state);
  }

  /**
   * Wraps the given primitive array for write operations
   * @param arr the given primitive array
   * @return WritableMemory for write operations
   */
  public static WritableMemory wrap(final char[] arr) {
    final MemoryState state = new MemoryState();
    state.putUnsafeObject(arr);
    state.putUnsafeObjectHeader(ARRAY_CHAR_BASE_OFFSET);
    state.putCapacity(arr.length << CHAR_SHIFT);
    return new WritableMemoryImpl(state);
  }

  /**
   * Wraps the given primitive array for write operations
   * @param arr the given primitive array
   * @return WritableMemory for write operations
   */
  public static WritableMemory wrap(final short[] arr) {
    final MemoryState state = new MemoryState();
    state.putUnsafeObject(arr);
    state.putUnsafeObjectHeader(ARRAY_SHORT_BASE_OFFSET);
    state.putCapacity(arr.length << SHORT_SHIFT);
    return new WritableMemoryImpl(state);
  }

  /**
   * Wraps the given primitive array for write operations
   * @param arr the given primitive array
   * @return WritableMemory for write operations
   */
  public static WritableMemory wrap(final int[] arr) {
    final MemoryState state = new MemoryState();
    state.putUnsafeObject(arr);
    state.putUnsafeObjectHeader(ARRAY_INT_BASE_OFFSET);
    state.putCapacity(arr.length << INT_SHIFT);
    return new WritableMemoryImpl(state);
  }

  /**
   * Wraps the given primitive array for write operations
   * @param arr the given primitive array
   * @return WritableMemory for write operations
   */
  public static WritableMemory wrap(final long[] arr) {
    final MemoryState state = new MemoryState();
    state.putUnsafeObject(arr);
    state.putUnsafeObjectHeader(ARRAY_LONG_BASE_OFFSET);
    state.putCapacity(arr.length << LONG_SHIFT);
    return new WritableMemoryImpl(state);
  }

  /**
   * Wraps the given primitive array for write operations
   * @param arr the given primitive array
   * @return WritableMemory for write operations
   */
  public static WritableMemory wrap(final float[] arr) {
    final MemoryState state = new MemoryState();
    state.putUnsafeObject(arr);
    state.putUnsafeObjectHeader(ARRAY_FLOAT_BASE_OFFSET);
    state.putCapacity(arr.length << FLOAT_SHIFT);
    return new WritableMemoryImpl(state);
  }

  /**
   * Wraps the given primitive array for write operations
   * @param arr the given primitive array
   * @return WritableMemory for write operations
   */
  public static WritableMemory writableWrap(final double[] arr) {
    final MemoryState state = new MemoryState();
    state.putUnsafeObject(arr);
    state.putUnsafeObjectHeader(ARRAY_DOUBLE_BASE_OFFSET);
    state.putCapacity(arr.length << DOUBLE_SHIFT);
    return new WritableMemoryImpl(state);
  }
  //END OF CONSTRUCTOR-TYPE METHODS

  //PRIMITIVE getXXX() and getXXXArray() //XXX

  /**
   * Gets the boolean value at the given offset
   * @param offsetBytes offset bytes relative to this Memory start
   * @return the boolean at the given offset
   */
  public abstract boolean getBoolean(long offsetBytes);

  /**
   * Gets the boolean array at the given offset
   * @param offsetBytes offset bytes relative to this Memory start
   * @param dstArray The preallocated destination array.
   * @param dstOffset offset in array units
   * @param length number of array units to transfer
   */
  public abstract void getBooleanArray(long offsetBytes, boolean[] dstArray, int dstOffset,
      int length);

  /**
   * Gets the byte value at the given offset
   * @param offsetBytes offset bytes relative to this Memory start
   * @return the byte at the given offset
   */
  public abstract byte getByte(long offsetBytes);

  /**
   * Gets the byte array at the given offset
   * @param offsetBytes offset bytes relative to this Memory start
   * @param dstArray The preallocated destination array.
   * @param dstOffset offset in array units
   * @param length number of array units to transfer
   */
  public abstract void getByteArray(long offsetBytes, byte[] dstArray, int dstOffset,
      int length);

  /**
   * Gets the char value at the given offset
   * @param offsetBytes offset bytes relative to this Memory start
   * @return the char at the given offset
   */
  public abstract char getChar(long offsetBytes);

  /**
   * Gets the char array at the given offset
   * @param offsetBytes offset bytes relative to this Memory start
   * @param dstArray The preallocated destination array.
   * @param dstOffset offset in array units
   * @param length number of array units to transfer
   */
  public abstract void getCharArray(long offsetBytes, char[] dstArray, int dstOffset,
      int length);

  /**
   * Gets the double value at the given offset
   * @param offsetBytes offset bytes relative to this Memory start
   * @return the double at the given offset
   */
  public abstract double getDouble(long offsetBytes);

  /**
   * Gets the double array at the given offset
   * @param offsetBytes offset bytes relative to this Memory start
   * @param dstArray The preallocated destination array.
   * @param dstOffset offset in array units
   * @param length number of array units to transfer
   */
  public abstract void getDoubleArray(long offsetBytes, double[] dstArray, int dstOffset,
      int length);

  /**
   * Gets the float value at the given offset
   * @param offsetBytes offset bytes relative to this Memory start
   * @return the float at the given offset
   */
  public abstract float getFloat(long offsetBytes);

  /**
   * Gets the float array at the given offset
   * @param offsetBytes offset bytes relative to this Memory start
   * @param dstArray The preallocated destination array.
   * @param dstOffset offset in array units
   * @param length number of array units to transfer
   */
  public abstract void getFloatArray(long offsetBytes, float[] dstArray, int dstOffset,
      int length);

  /**
   * Gets the int value at the given offset
   * @param offsetBytes offset bytes relative to this Memory start
   * @return the int at the given offset
   */
  public abstract int getInt(long offsetBytes);

  /**
   * Gets the int array at the given offset
   * @param offsetBytes offset bytes relative to this Memory start
   * @param dstArray The preallocated destination array.
   * @param dstOffset offset in array units
   * @param length number of array units to transfer
   */
  public abstract void getIntArray(long offsetBytes, int[] dstArray, int dstOffset,
      int length);

  /**
   * Gets the long value at the given offset
   * @param offsetBytes offset bytes relative to this Memory start
   * @return the long at the given offset
   */
  public abstract long getLong(long offsetBytes);

  /**
   * Gets the long array at the given offset
   * @param offsetBytes offset bytes relative to this Memory start
   * @param dstArray The preallocated destination array.
   * @param dstOffset offset in array units
   * @param length number of array units to transfer
   */
  public abstract void getLongArray(long offsetBytes, long[] dstArray, int dstOffset, int length);

  /**
   * Gets the short value at the given offset
   * @param offsetBytes offset bytes relative to this Memory start
   * @return the short at the given offset
   */
  public abstract short getShort(long offsetBytes);

  /**
   * Gets the short array at the given offset
   * @param offsetBytes offset bytes relative to this Memory start
   * @param dstArray The preallocated destination array.
   * @param dstOffset offset in array units
   * @param length number of array units to transfer
   */
  public abstract void getShortArray(long offsetBytes, short[] dstArray, int dstOffset,
      int length);

  //OTHER PRIMITIVE READ METHODS: copy, isYYYY(), areYYYY() //XXX

  /**
   * Copies bytes from a source range of this Memory to a destination range of the given Memory
   * using the same low-level system copy function as found in
   * {@link java.lang.System#arraycopy(Object, int, Object, int, int)}.
   * @param srcOffsetBytes the source offset for this Memory
   * @param destination the destination Memory, which may not be Read-Only.
   * @param dstOffsetBytes the destintaion offset
   * @param lengthBytes the number of bytes to copy
   */
  public abstract void copyTo(long srcOffsetBytes, WritableMemory destination, long dstOffsetBytes,
      long lengthBytes);

  /**
   * Returns true if all bits defined by the bitMask are clear
   * @param offsetBytes offset bytes relative to this Memory start
   * @param bitMask bits set to one will be checked
   * @return true if all bits defined by the bitMask are clear
   */
  public abstract boolean isAllBitsClear(long offsetBytes, byte bitMask);

  /**
   * Returns true if all bits defined by the bitMask are set
   * @param offsetBytes offset bytes relative to this Memory start
   * @param bitMask bits set to one will be checked
   * @return true if all bits defined by the bitMask are set
   */
  public abstract boolean isAllBitsSet(long offsetBytes, byte bitMask);

  /**
   * Returns true if any bits defined by the bitMask are clear
   * @param offsetBytes offset bytes relative to this Memory start
   * @param bitMask bits set to one will be checked
   * @return true if any bits defined by the bitMask are clear
   */
  public abstract boolean isAnyBitsClear(long offsetBytes, byte bitMask);

  /**
   * Returns true if any bits defined by the bitMask are set
   * @param offsetBytes offset bytes relative to this Memory start
   * @param bitMask bits set to one will be checked
   * @return true if any bits defined by the bitMask are set
   */
  public abstract boolean isAnyBitsSet(long offsetBytes, byte bitMask);

  //OTHER READ METHODS //XXX

  /**
   * Gets the capacity of this Memory in bytes
   * @return the capacity of this Memory in bytes
   */
  public abstract long getCapacity();

  /**
   * Returns the cumulative offset in bytes of this Memory including the given offsetBytes.
   *
   * @param offsetBytes the given offset in bytes
   * @return the cumulative offset in bytes of this Memory including the given offsetBytes.
   */
  public abstract long getCumulativeOffset(final long offsetBytes);

  /**
   * Returns true if this Memory is backed by an on-heap primitive array
   * @return true if this Memory is backed by an on-heap primitive array
   */
  public abstract boolean hasArray();

  /**
   * Returns true if this Memory is backed by a ByteBuffer
   * @return true if this Memory is backed by a ByteBuffer
   */
  public abstract boolean hasByteBuffer();

  /**
   * Returns true if the backing memory is direct (off-heap) memory.
   * @return true if the backing memory is direct (off-heap) memory.
   */
  public abstract boolean isDirect();

  /**
   * Returns true if the backing Memory is read only
   * @return true if the backing Memory is read only
   */
  public abstract boolean isReadOnly();

  /**
   * Returns true if this Memory is valid() and has not been closed.
   * @return true if this Memory is valid() and has not been closed.
   */
  public abstract boolean isValid();

  /**
   * Returns a formatted hex string of a range of this Memory.
   * Used primarily for testing.
   * @param header descriptive header
   * @param offsetBytes offset bytes relative to this Memory start
   * @param lengthBytes number of bytes to convert to a hex string
   * @return a formatted hex string in a human readable array
   */
  public abstract String toHexString(String header, long offsetBytes, int lengthBytes);


  //PRIMITIVE putXXX() and putXXXArray() //XXX

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

  //Atomic Methods //XXX

  /**
   * Atomically adds the given value to the long located at offsetBytes.
   * @param offsetBytes offset bytes relative to this Memory start
   * @param delta the amount to add
   * @return the modified value
   */
  public abstract long getAndAddLong(long offsetBytes, long delta);

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

  //OTHER WRITE METHODS //XXX

  abstract Object getArray();

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

  //OTHER //XXX

  /**
   * Returns a MemoryRequest or null
   * @return a MemoryRequest or null
   */
  public abstract MemoryRequest getMemoryRequest();

}
