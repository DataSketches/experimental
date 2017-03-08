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
import static com.yahoo.memory4.UnsafeUtil.LS;
import static com.yahoo.memory4.UnsafeUtil.assertBounds;
import static com.yahoo.memory4.UnsafeUtil.unsafe;

import java.io.File;
import java.nio.ByteBuffer;

public abstract class Memory {

  //BYTE BUFFER
  /**
   * Accesses the given ByteBuffer for read-only operations.
   * @param byteBuf the given ByteBuffer
   * @return the given ByteBuffer for read-only operations.
   */
  public static MemoryHandler wrap(final ByteBuffer byteBuf) {
    final MemoryState state = new MemoryState();
    state.putByteBuffer(byteBuf);
    return (MemoryHandler) AccessByteBuffer.wrap(state);
  }

  //MAP
  /**
   * Allocates direct memory used to memory map files for read operations
   * (including those &gt; 2GB).
   * @param file the given file to map
   * @return MemoryHandler for managing this map
   * @throws Exception file not found or RuntimeException, etc.
   */
  public static MemoryHandler map(final File file) throws Exception {
    return map(file, 0, file.length());
  }

  /**
   * Allocates direct memory used to memory map files for read operations
   * (including those &gt; 2GB).
   * @param file the given file to map
   * @param fileOffset the position in the given file
   * @param capacity the size of the allocated direct memory
   * @return MemoryHandler for managing this map
   * @throws Exception file not found or RuntimeException, etc.
   */
  public static MemoryHandler map(final File file, final long fileOffset,
      final long capacity) throws Exception {
    final MemoryState state = new MemoryState();
    state.putFile(file);
    state.putFileOffset(fileOffset);
    state.putCapacity(capacity);
    return AllocateDirectMap.map(state);
  }

  //REGIONS

  /**
   * Returns a read only region of this Memory.
   * @param offsetBytes the starting offset
   * @param capacityBytes the capacity of the region
   * @return a read only region of this Memory
   */
  public abstract Memory region(long offsetBytes, long capacityBytes);

  //ACCESS PRIMITIVE HEAP ARRAYS for readOnly

  /**
   * Wraps the given primitive array for read operations
   * @param arr the given primitive array
   * @return Memory for read operations
   */
  public static Memory wrap(final boolean[] arr) {
    final MemoryState state = new MemoryState();
    state.putUnsafeObject(arr);
    state.putUnsafeObjectHeader(ARRAY_BOOLEAN_BASE_OFFSET);
    state.putCapacity(arr.length << BOOLEAN_SHIFT);
    state.setResourceReadOnly();
    return new MemoryImpl(state);
  }

  /**
   * Wraps the given primitive array for read operations
   * @param arr the given primitive array
   * @return Memory for read operations
   */
  public static Memory wrap(final byte[] arr) {
    final MemoryState state = new MemoryState();
    state.putUnsafeObject(arr);
    state.putUnsafeObjectHeader(ARRAY_BYTE_BASE_OFFSET);
    state.putCapacity(arr.length << BYTE_SHIFT);
    state.setResourceReadOnly();
    return new MemoryImpl(state);
  }

  /**
   * Wraps the given primitive array for read operations
   * @param arr the given primitive array
   * @return Memory for read operations
   */
  public static Memory wrap(final char[] arr) {
    final MemoryState state = new MemoryState();
    state.putUnsafeObject(arr);
    state.putUnsafeObjectHeader(ARRAY_CHAR_BASE_OFFSET);
    state.putCapacity(arr.length << CHAR_SHIFT);
    state.setResourceReadOnly();
    return new MemoryImpl(state);
  }

  /**
   * Wraps the given primitive array for read operations
   * @param arr the given primitive array
   * @return Memory for read operations
   */
  public static Memory wrap(final short[] arr) {
    final MemoryState state = new MemoryState();
    state.putUnsafeObject(arr);
    state.putUnsafeObjectHeader(ARRAY_SHORT_BASE_OFFSET);
    state.putCapacity(arr.length << SHORT_SHIFT);
    state.setResourceReadOnly();
    return new MemoryImpl(state);
  }

  /**
   * Wraps the given primitive array for read operations
   * @param arr the given primitive array
   * @return Memory for read operations
   */
  public static Memory wrap(final int[] arr) {
    final MemoryState state = new MemoryState();
    state.putUnsafeObject(arr);
    state.putUnsafeObjectHeader(ARRAY_INT_BASE_OFFSET);
    state.putCapacity(arr.length << INT_SHIFT);
    state.setResourceReadOnly();
    return new MemoryImpl(state);
  }

  /**
   * Wraps the given primitive array for read operations
   * @param arr the given primitive array
   * @return Memory for read operations
   */
  public static Memory wrap(final long[] arr) {
    final MemoryState state = new MemoryState();
    state.putUnsafeObject(arr);
    state.putUnsafeObjectHeader(ARRAY_LONG_BASE_OFFSET);
    state.putCapacity(arr.length << LONG_SHIFT);
    state.setResourceReadOnly();
    return new MemoryImpl(state);
  }

  /**
   * Wraps the given primitive array for read operations
   * @param arr the given primitive array
   * @return Memory for read operations
   */
  public static Memory wrap(final float[] arr) {
    final MemoryState state = new MemoryState();
    state.putUnsafeObject(arr);
    state.putUnsafeObjectHeader(ARRAY_FLOAT_BASE_OFFSET);
    state.putCapacity(arr.length << FLOAT_SHIFT);
    state.setResourceReadOnly();
    return new MemoryImpl(state);
  }

  /**
   * Wraps the given primitive array for read operations
   * @param arr the given primitive array
   * @return Memory for read operations
   */
  public static Memory wrap(final double[] arr) {
    final MemoryState state = new MemoryState();
    state.putUnsafeObject(arr);
    state.putUnsafeObjectHeader(ARRAY_DOUBLE_BASE_OFFSET);
    state.putCapacity(arr.length << DOUBLE_SHIFT);
    state.setResourceReadOnly();
    return new MemoryImpl(state);
  }

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
  public abstract void copy(long srcOffsetBytes, WritableMemory destination, long dstOffsetBytes,
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

  /**
   * Returns a formatted hex string of an area of this Memory.
   * Used primarily for testing.
   * @param header a descriptive header
   * @param offsetBytes offset bytes relative to the Memory start
   * @param lengthBytes number of bytes to convert to a hex string
   * @return a formatted hex string in a human readable array
   */
  static String toHex(final String header, final long offsetBytes, final int lengthBytes,
      final MemoryState state) {
    assertBounds(offsetBytes, lengthBytes, state.getCapacity());
    final StringBuilder sb = new StringBuilder();
    final Object uObj = state.getUnsafeObject();
    final String uObjStr = (uObj == null) ? "null"
        : uObj.getClass().getSimpleName() + ", " + uObj.hashCode();
    final ByteBuffer bb = state.getByteBuffer();
    final String bbStr = (bb == null) ? "null"
        : bb.getClass().getSimpleName() + ", " + bb.hashCode();
    final MemoryRequest memReq = state.getMemoryRequest();
    final String memReqStr = (memReq == null) ? "null"
        : memReq.getClass().getSimpleName() + ", " + memReq.hashCode();
    final long cumBaseOffset = state.getCumBaseOffset();
    sb.append(header).append(LS);
    sb.append("NativeBaseOffset    : ").append(state.getNativeBaseOffset()).append(LS);
    sb.append("UnsafeObj           : ").append(uObjStr).append(LS);
    sb.append("UnsafeObjHeader     : ").append(state.getUnsafeObjectHeader()).append(LS);
    sb.append("ByteBuf             : ").append(bbStr).append(LS);
    sb.append("RegionOffset        : ").append(state.getRegionOffset()).append(LS);
    sb.append("Capacity            : ").append(state.getCapacity()).append(LS);
    sb.append("CumBaseOffset       : ").append(cumBaseOffset).append(LS);
    sb.append("MemReq              : ").append(memReqStr).append(LS);
    sb.append("Valid               : ").append(state.isValid()).append(LS);
    sb.append("Read Only           : ").append(state.isResourceReadOnly()).append(LS);
    sb.append("Memory, littleEndian:  0  1  2  3  4  5  6  7");
    long j = offsetBytes;
    final StringBuilder sb2 = new StringBuilder();
    for (long i = 0; i < lengthBytes; i++) {
      final int b = unsafe.getByte(uObj, cumBaseOffset + i) & 0XFF;
      if ((i != 0) && ((i % 8) == 0)) {
        sb.append(String.format("%n%20s: ", j)).append(sb2);
        j += 8;
        sb2.setLength(0);
      }
      sb2.append(String.format("%02x ", b));
    }
    sb.append(String.format("%n%20s: ", j)).append(sb2).append(LS);
    return sb.toString();
  }


}