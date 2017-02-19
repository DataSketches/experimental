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
  long nativeBaseOffset = 0;
  Object unsafeObj = null;
  long unsafeObjHeader = 0;
  ByteBuffer byteBuf = null;
  long cumBaseOffset = 0;
  long regionOffset = 0;
  long capacity = 0;
  MemoryRequest memReq = null;

  //Constructor for heap array access and auto byte[] allocation
  WritableMemoryImpl(final Object unsafeObj, final long unsafeObjHeader, final long capacity) {
    assert unsafeObj != null;
    assert unsafeObjHeader > 0;
    this.unsafeObj = unsafeObj;
    this.unsafeObjHeader = unsafeObjHeader;
    this.capacity = capacity;
    this.cumBaseOffset = unsafeObjHeader;
  }

  //Constructor for ByteBuffer direct
  WritableMemoryImpl(final long nativeBaseOffset, final ByteBuffer byteBuf, final long capacity) {
    this.nativeBaseOffset = nativeBaseOffset; //already adjusted for slices
    this.byteBuf = byteBuf;
    this.capacity = capacity;
    this.cumBaseOffset = nativeBaseOffset;
  }

  //Constructor for ByteBuffer heap
  WritableMemoryImpl(final Object unsafeObj, final long unsafeObjHeader, final ByteBuffer byteBuf,
      final long regionOffset, final long capacity) {
    assert unsafeObj != null;
    assert unsafeObjHeader > 0;
    this.unsafeObj = unsafeObj;
    this.unsafeObjHeader = unsafeObjHeader;
    this.byteBuf = byteBuf;
    this.regionOffset = regionOffset;
    this.capacity = capacity;
    this.cumBaseOffset = regionOffset + unsafeObjHeader;
  }

  //Constructor for Regions
  WritableMemoryImpl(final long nativeBaseOffset, final Object unsafeObj, final long unsafeObjHeader,
      final ByteBuffer byteBuf, final long regionOffset, final long capacity,
      final MemoryRequest memReq) {
    this.nativeBaseOffset = nativeBaseOffset;
    this.unsafeObj = unsafeObj;
    this.unsafeObjHeader = unsafeObjHeader;
    this.byteBuf = byteBuf;
    this.regionOffset = regionOffset;
    this.capacity = capacity;
    this.memReq = memReq;
    this.cumBaseOffset = regionOffset + ((unsafeObj == null) ? nativeBaseOffset : unsafeObjHeader);
  }

  //Constructor for Direct native allocation
  WritableMemoryImpl(final long nativeBaseOffset, final long capacity, final MemoryRequest memReq) {
    this.nativeBaseOffset = nativeBaseOffset;
    this.capacity = capacity;
    this.memReq = memReq;
    this.cumBaseOffset = nativeBaseOffset;
  }

  //To automatically allocate a byte[]
  static WritableMemoryImpl allocateArray(final int capacity) {
    final byte[] unsafeObj = new byte[capacity];
    return new WritableMemoryImpl(unsafeObj, ARRAY_BYTE_BASE_OFFSET, capacity);
  }

  @Override
  public Memory asReadOnly() {
    //    return new MemoryImpl(nativeBaseOffset, unsafeObj, unsafeObjHeader, byteBuf, regionOffset,
    //        capacity);
    return null; //TODO
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
    final Object unsafeObj;
    final long unsafeObjHeader;
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
        return new WritableMemoryImpl(nativeBaseAddress, byteBuffer, capacity);
      }

      //WRITABLE-HEAP
      unsafeObj = byteBuf.array();
      unsafeObjHeader = ARRAY_BYTE_BASE_OFFSET;
      regionOffset = byteBuf.arrayOffset() * ARRAY_BYTE_INDEX_SCALE;
      return new WritableMemoryImpl(unsafeObj, unsafeObjHeader, byteBuffer, regionOffset, capacity);
    }
  }

  //Map

  //Primitive Gets

  @Override
  public boolean getBoolean(final long offsetBytes) {
    assertBounds(offsetBytes, ARRAY_BOOLEAN_INDEX_SCALE, capacity);
    return unsafe.getBoolean(unsafeObj, cumBaseOffset + offsetBytes);
  }

  @Override
  public byte getByte(final long offsetBytes) {
    assertBounds(offsetBytes, ARRAY_BYTE_INDEX_SCALE, capacity);
    return unsafe.getByte(unsafeObj, cumBaseOffset + offsetBytes);
  }

  @Override
  public char getChar(final long offsetBytes) {
    assertBounds(offsetBytes, ARRAY_CHAR_INDEX_SCALE, capacity);
    return unsafe.getChar(unsafeObj, cumBaseOffset + offsetBytes);
  }

  @Override
  public short getShort(final long offsetBytes) {
    assertBounds(offsetBytes, ARRAY_SHORT_INDEX_SCALE, capacity);
    return unsafe.getShort(unsafeObj, cumBaseOffset + offsetBytes);
  }

  @Override
  public int getInt(final long offsetBytes) {
    assertBounds(offsetBytes, ARRAY_INT_INDEX_SCALE, capacity);
    return unsafe.getInt(unsafeObj, cumBaseOffset + offsetBytes);
  }

  @Override
  public long getLong(final long offsetBytes) {
    assertBounds(offsetBytes, ARRAY_LONG_INDEX_SCALE, capacity);
    return unsafe.getLong(unsafeObj, cumBaseOffset + offsetBytes);
  }

  @Override
  public float getFloat(final long offsetBytes) {
    assertBounds(offsetBytes, ARRAY_FLOAT_INDEX_SCALE, capacity);
    return unsafe.getFloat(unsafeObj, cumBaseOffset + offsetBytes);
  }

  @Override
  public double getDouble(final long offsetBytes) {
    assertBounds(offsetBytes, ARRAY_DOUBLE_INDEX_SCALE, capacity);
    return unsafe.getDouble(unsafeObj, cumBaseOffset + offsetBytes);
  }

  //Primitive Get Arrays

  @Override
  public void getBooleanArray(final long offsetBytes, final boolean[] dstArray, final int dstOffset,
      final int length) {
    final long copyBytes = length << BOOLEAN_SHIFT;
    assertBounds(offsetBytes, copyBytes, capacity);
    assertBounds(dstOffset, length, dstArray.length);
    unsafe.copyMemory(
      unsafeObj,
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
      unsafeObj,
      cumBaseOffset,
      dstArray,
      ARRAY_LONG_BASE_OFFSET + (dstOffset << LONG_SHIFT),
      copyBytes);
  }

  //Primitive Puts

  @Override
  public void putBoolean(final long offsetBytes, final boolean value) {
    assertBounds(offsetBytes, ARRAY_BOOLEAN_INDEX_SCALE, capacity);
    unsafe.putBoolean(unsafeObj, cumBaseOffset + offsetBytes, value);
  }

  @Override
  public void putByte(final long offsetBytes, final byte value) {
    assertBounds(offsetBytes, ARRAY_BYTE_INDEX_SCALE, capacity);
    unsafe.putByte(unsafeObj, cumBaseOffset + offsetBytes, value);
  }

  @Override
  public void putChar(final long offsetBytes, final char value) {
    assertBounds(offsetBytes, ARRAY_CHAR_INDEX_SCALE, capacity);
    unsafe.putChar(unsafeObj, cumBaseOffset + offsetBytes, value);
  }

  @Override
  public void putInt(final long offsetBytes, final int value) {
    assertBounds(offsetBytes, ARRAY_INT_INDEX_SCALE, capacity);
    unsafe.putInt(unsafeObj, cumBaseOffset + offsetBytes, value);
  }

  @Override
  public void putLong(final long offsetBytes, final long value) {
    assertBounds(offsetBytes, ARRAY_LONG_INDEX_SCALE, capacity);
    unsafe.putLong(unsafeObj, cumBaseOffset + offsetBytes, value);
  }

  @Override
  public void putFloat(final long offsetBytes, final float value) {
    assertBounds(offsetBytes, ARRAY_FLOAT_INDEX_SCALE, capacity);
    unsafe.putFloat(unsafeObj, cumBaseOffset + offsetBytes, value);
  }

  @Override
  public void putDouble(final long offsetBytes, final double value) {
    assertBounds(offsetBytes, ARRAY_DOUBLE_INDEX_SCALE, capacity);
    unsafe.putDouble(unsafeObj, cumBaseOffset + offsetBytes, value);
  }

  //Primitive Put Arrays

  @Override
  public void putBooleanArray(final long offsetBytes, final boolean[] srcArray, final int srcOffset,
      final int length) {
    final long copyBytes = length << BOOLEAN_SHIFT;
    assertBounds(srcOffset, length, srcArray.length);
    assertBounds(offsetBytes, copyBytes, capacity);
    unsafe.copyMemory(
      srcArray,
      ARRAY_BOOLEAN_BASE_OFFSET + (srcOffset << BOOLEAN_SHIFT),
      unsafeObj,
      cumBaseOffset,
      copyBytes
      );
  }

  @Override
  public void putLongArray(final long offsetBytes, final long[] srcArray, final int srcOffset,
      final int length) {
    final long copyBytes = length << LONG_SHIFT;
    assertBounds(srcOffset, length, srcArray.length);
    assertBounds(offsetBytes, copyBytes, capacity);
    unsafe.copyMemory(
      srcArray,
      ARRAY_LONG_BASE_OFFSET + (srcOffset << LONG_SHIFT),
      unsafeObj,
      cumBaseOffset,
      copyBytes
      );
  }

  //Plus a number of convenience write methods not listed
  // e.g., clean, fill, MemoryRequest, etc.

  //Regions

  @Override
  public Memory region(final long offsetBytes, final long capacityBytes) {
    //    final long newRegionOffset = this.regionOffset + offsetBytes;
    //    final long newCapacity = capacityBytes;
    //    return new MemoryImpl(nativeBaseOffset, unsafeObj, unsafeObjHeader, byteBuf,
    //        newRegionOffset, newCapacity);
    return null; //TODO
  }

  @Override
  public WritableMemory writableRegion(final long offsetBytes, final long capacityBytes) {
    assert offsetBytes + capacityBytes <= capacity
        : "newOff + newCap: " + (offsetBytes + capacityBytes) + ", origCap: " + capacity;
    final long newRegionOffset = this.regionOffset + offsetBytes;
    final long newCapacity = capacityBytes;
    return new WritableMemoryImpl(nativeBaseOffset, unsafeObj, unsafeObjHeader, byteBuf,
        newRegionOffset, newCapacity, memReq);
  }

  @Override
  public MemoryRequest getMemoryRequest() {
    return memReq;
  }

  /**
   * Optional freeMemory blah, blah
   */
  @Override
  public void freeMemory() {
  //    nativeBaseOffset = 0L;
  //    unsafeObj = null;
  //    unsafeObjHeader = 0L;
  //    byteBuf = null;
  //    cumBaseOffset = 0L;
  //    regionOffset = 0L;
  //    capacity = 0L;
  //    memReq = null;
  }

  @Override
  boolean isValid() { return true; }

}
