/*
 * Copyright 2017, Yahoo! Inc. Licensed under the terms of the
 * Apache License 2.0. See LICENSE file at the project root for terms.
 */

package com.yahoo.memory2;

import static com.yahoo.memory.UnsafeUtil.ARRAY_BOOLEAN_BASE_OFFSET;
import static com.yahoo.memory.UnsafeUtil.ARRAY_BOOLEAN_INDEX_SCALE;
import static com.yahoo.memory.UnsafeUtil.ARRAY_BYTE_BASE_OFFSET;
import static com.yahoo.memory.UnsafeUtil.ARRAY_BYTE_INDEX_SCALE;
import static com.yahoo.memory.UnsafeUtil.ARRAY_CHAR_INDEX_SCALE;
import static com.yahoo.memory.UnsafeUtil.ARRAY_DOUBLE_INDEX_SCALE;
import static com.yahoo.memory.UnsafeUtil.ARRAY_FLOAT_INDEX_SCALE;
import static com.yahoo.memory.UnsafeUtil.ARRAY_INT_INDEX_SCALE;
import static com.yahoo.memory.UnsafeUtil.ARRAY_LONG_BASE_OFFSET;
import static com.yahoo.memory.UnsafeUtil.ARRAY_LONG_INDEX_SCALE;
import static com.yahoo.memory.UnsafeUtil.ARRAY_SHORT_INDEX_SCALE;
import static com.yahoo.memory.UnsafeUtil.BOOLEAN_SHIFT;
import static com.yahoo.memory.UnsafeUtil.LONG_SHIFT;
import static com.yahoo.memory.UnsafeUtil.assertBounds;
import static com.yahoo.memory.UnsafeUtil.unsafe;

import java.nio.ByteBuffer;

//@SuppressWarnings("unused")
class WritableMemoryImpl extends WritableMemory {
  long nativeBaseOffset;
  Object memObj;
  long memObjHeader;
  ByteBuffer byteBuf;
  long cumBaseOffset;
  long regionOffset;
  long capacity;
  MemoryRequest memReq = null;

  /**
   * @param regionOffset blah
   * @param capacity blah
   * @param cumBaseOffset blah
   */
  WritableMemoryImpl(final long nativeBaseOffset, final Object memObj, final long memObjHeader,
      final ByteBuffer byteBuf, final long regionOffset, final long capacity,
      final MemoryRequest memReq) {
    this.nativeBaseOffset = nativeBaseOffset;
    this.memObj = memObj;
    this.memObjHeader = memObjHeader;
    this.byteBuf = byteBuf;
    this.regionOffset = regionOffset;
    this.cumBaseOffset = (memObj == null)
        ? nativeBaseOffset + regionOffset
        : memObjHeader + regionOffset;
    this.capacity = capacity;
    this.memReq = memReq;
  }

  static WritableMemoryImpl allocateArray(final int capacity, final MemoryRequest memReq) {
    final byte[] memObj = new byte[capacity];
    return new WritableMemoryImpl(0L, memObj, ARRAY_BYTE_BASE_OFFSET, null, 0L,
        capacity, memReq);
  }

  @Override
  public Memory asReadOnly() {
    return new MemoryImpl(nativeBaseOffset, memObj, memObjHeader, byteBuf, regionOffset, capacity);
  }

  //ByteBuffer
  /**
   * Provides access to the backing store of the given ByteBuffer.
   * If the given <i>ByteBuffer</i> is read-only, the returned <i>Memory</i> will also be a
   * read-only instance.
   * @param byteBuffer the given <i>ByteBuffer</i>
   * @return a <i>Memory</i> object
   */
  public static WritableMemory writableWrap(final ByteBuffer byteBuffer) {
    final long nativeBaseAddress;  //includes the slice() offset for direct.
    final Object memObj;
    final long memObjHeader;
    final ByteBuffer byteBuf = byteBuffer;
    final long regionOffset; //the slice() offset for heap.
    final long capacity = byteBuffer.capacity();

    final boolean readOnly = byteBuffer.isReadOnly();
    final boolean direct = byteBuffer.isDirect();

    if (readOnly) {
      throw new IllegalArgumentException("Provided ByteBuffer is Read-only.");
    }

    //WRITABLE
    else {

      //WRITABLE-DIRECT - converted to read only
      if (direct) {
        //address() is already adjusted for direct slices, so regionOffset = 0
        nativeBaseAddress = ((sun.nio.ch.DirectBuffer) byteBuf).address();
        memObj = null;
        memObjHeader = 0L;
        regionOffset = 0L;

        final WritableMemoryImpl nmr = new WritableMemoryImpl(
            nativeBaseAddress, memObj, memObjHeader, byteBuffer, regionOffset, capacity, null);
        return nmr;
      }

      //WRITABLE-HEAP
      nativeBaseAddress = 0L;
      regionOffset = byteBuf.arrayOffset() * ARRAY_BYTE_INDEX_SCALE;
      memObjHeader = ARRAY_BYTE_BASE_OFFSET;
      memObj = byteBuf.array();

      final WritableMemoryImpl nmr = new WritableMemoryImpl(
          nativeBaseAddress, memObj, memObjHeader, byteBuffer, regionOffset, capacity, null);
      return nmr;
    }
  }

  //Map

  //Primitive Gets

  @Override
  public boolean getBoolean(final long offsetBytes) {
    assertBounds(offsetBytes, ARRAY_BOOLEAN_INDEX_SCALE, capacity);
    return unsafe.getBoolean(memObj, cumBaseOffset + offsetBytes);
  }

  @Override
  public byte getByte(final long offsetBytes) {
    assertBounds(offsetBytes, ARRAY_BYTE_INDEX_SCALE, capacity);
    return unsafe.getByte(memObj, cumBaseOffset + offsetBytes);
  }

  @Override
  public char getChar(final long offsetBytes) {
    assertBounds(offsetBytes, ARRAY_CHAR_INDEX_SCALE, capacity);
    return unsafe.getChar(memObj, cumBaseOffset + offsetBytes);
  }

  @Override
  public short getShort(final long offsetBytes) {
    assertBounds(offsetBytes, ARRAY_SHORT_INDEX_SCALE, capacity);
    return unsafe.getShort(memObj, cumBaseOffset + offsetBytes);
  }

  @Override
  public int getInt(final long offsetBytes) {
    assertBounds(offsetBytes, ARRAY_INT_INDEX_SCALE, capacity);
    return unsafe.getInt(memObj, cumBaseOffset + offsetBytes);
  }

  @Override
  public long getLong(final long offsetBytes) {
    assertBounds(offsetBytes, ARRAY_LONG_INDEX_SCALE, capacity);
    return unsafe.getLong(memObj, cumBaseOffset + offsetBytes);
  }

  @Override
  public float getFloat(final long offsetBytes) {
    assertBounds(offsetBytes, ARRAY_FLOAT_INDEX_SCALE, capacity);
    return unsafe.getFloat(memObj, cumBaseOffset + offsetBytes);
  }

  @Override
  public double getDouble(final long offsetBytes) {
    assertBounds(offsetBytes, ARRAY_DOUBLE_INDEX_SCALE, capacity);
    return unsafe.getDouble(memObj, cumBaseOffset + offsetBytes);
  }

  //Primitive Get Arrays

  @Override
  public void getBooleanArray(final long offsetBytes, final boolean[] dstArray, final int dstOffset,
      final int length) {
    final long copyBytes = length << BOOLEAN_SHIFT;
    assertBounds(offsetBytes, copyBytes, capacity);
    assertBounds(dstOffset, length, dstArray.length);
    unsafe.copyMemory(
      memObj,
      cumBaseOffset,
      dstArray,
      ARRAY_BOOLEAN_BASE_OFFSET + (dstOffset << BOOLEAN_SHIFT),
      copyBytes);
  }

  @Override
  public void getLongArray(final long offsetBytes, final long[] dstArray, final int dstOffset,
      final int length) {
    final long copyBytes = length << LONG_SHIFT;
    assertBounds(offsetBytes, copyBytes, capacity);
    assertBounds(dstOffset, length, dstArray.length);
    unsafe.copyMemory(
      memObj,
      cumBaseOffset,
      dstArray,
      ARRAY_LONG_BASE_OFFSET + (dstOffset << LONG_SHIFT),
      copyBytes);
  }



  //Primitive Puts

  @Override
  public void putBoolean(final long offsetBytes, final boolean value) {
    assertBounds(offsetBytes, ARRAY_BOOLEAN_INDEX_SCALE, capacity);
    unsafe.putBoolean(memObj, cumBaseOffset + offsetBytes, value);
  }

  @Override
  public void putByte(final long offsetBytes, final byte value) {
    assertBounds(offsetBytes, ARRAY_BYTE_INDEX_SCALE, capacity);
    unsafe.putByte(memObj, cumBaseOffset + offsetBytes, value);
  }

  @Override
  public void putChar(final long offsetBytes, final char value) {
    assertBounds(offsetBytes, ARRAY_CHAR_INDEX_SCALE, capacity);
    unsafe.putChar(memObj, cumBaseOffset + offsetBytes, value);
  }

  @Override
  public void putInt(final long offsetBytes, final int value) {
    assertBounds(offsetBytes, ARRAY_INT_INDEX_SCALE, capacity);
    unsafe.putInt(memObj, cumBaseOffset + offsetBytes, value);
  }

  @Override
  public void putLong(final long offsetBytes, final long value) {
    assertBounds(offsetBytes, ARRAY_LONG_INDEX_SCALE, capacity);
    unsafe.putLong(memObj, cumBaseOffset + offsetBytes, value);
  }

  @Override
  public void putFloat(final long offsetBytes, final float value) {
    assertBounds(offsetBytes, ARRAY_FLOAT_INDEX_SCALE, capacity);
    unsafe.putFloat(memObj, cumBaseOffset + offsetBytes, value);
  }

  @Override
  public void putDouble(final long offsetBytes, final double value) {
    assertBounds(offsetBytes, ARRAY_DOUBLE_INDEX_SCALE, capacity);
    unsafe.putDouble(memObj, cumBaseOffset + offsetBytes, value);
  }

  //Primitive Put Arrays



  //Plus a number of convenience write methods not listed
  // e.g., clean, fill, MemoryRequest, etc.

  @Override
  public MemoryRequest getMemoryRequest() {
    return memReq;
  }

  /**
   * Optional freeMemory blah, blah
   */
  @Override
  public void freeMemory() {
    nativeBaseOffset = 0L;
    memObj = null;
    memObjHeader = 0L;
    byteBuf = null;
    cumBaseOffset = 0L;
    regionOffset = 0L;
    capacity = 0L;
    memReq = null;
  }

}
