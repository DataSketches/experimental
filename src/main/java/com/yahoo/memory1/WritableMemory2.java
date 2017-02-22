/*
 * Copyright 2017, Yahoo! Inc. Licensed under the terms of the
 * Apache License 2.0. See LICENSE file at the project root for terms.
 */

package com.yahoo.memory1;

import static com.yahoo.memory.UnsafeUtil.ARRAY_BOOLEAN_BASE_OFFSET;
import static com.yahoo.memory.UnsafeUtil.ARRAY_BOOLEAN_INDEX_SCALE;
import static com.yahoo.memory.UnsafeUtil.ARRAY_BYTE_BASE_OFFSET;
import static com.yahoo.memory.UnsafeUtil.ARRAY_BYTE_INDEX_SCALE;
import static com.yahoo.memory.UnsafeUtil.ARRAY_CHAR_BASE_OFFSET;
import static com.yahoo.memory.UnsafeUtil.ARRAY_CHAR_INDEX_SCALE;
import static com.yahoo.memory.UnsafeUtil.ARRAY_DOUBLE_BASE_OFFSET;
import static com.yahoo.memory.UnsafeUtil.ARRAY_DOUBLE_INDEX_SCALE;
import static com.yahoo.memory.UnsafeUtil.ARRAY_FLOAT_BASE_OFFSET;
import static com.yahoo.memory.UnsafeUtil.ARRAY_FLOAT_INDEX_SCALE;
import static com.yahoo.memory.UnsafeUtil.ARRAY_INT_BASE_OFFSET;
import static com.yahoo.memory.UnsafeUtil.ARRAY_INT_INDEX_SCALE;
import static com.yahoo.memory.UnsafeUtil.ARRAY_LONG_BASE_OFFSET;
import static com.yahoo.memory.UnsafeUtil.ARRAY_LONG_INDEX_SCALE;
import static com.yahoo.memory.UnsafeUtil.ARRAY_SHORT_BASE_OFFSET;
import static com.yahoo.memory.UnsafeUtil.BOOLEAN_SHIFT;
import static com.yahoo.memory.UnsafeUtil.BYTE_SHIFT;
import static com.yahoo.memory.UnsafeUtil.CHAR_SHIFT;
import static com.yahoo.memory.UnsafeUtil.DOUBLE_SHIFT;
import static com.yahoo.memory.UnsafeUtil.FLOAT_SHIFT;
import static com.yahoo.memory.UnsafeUtil.INT_SHIFT;
import static com.yahoo.memory.UnsafeUtil.LONG_SHIFT;
import static com.yahoo.memory.UnsafeUtil.SHORT_SHIFT;
import static com.yahoo.memory.UnsafeUtil.assertBounds;
import static com.yahoo.memory.UnsafeUtil.unsafe;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;

public class WritableMemory2 extends MemoryImpl {
  MemoryRequest memReq = null;

  //Constructor for heap array access and auto byte[] allocation
  WritableMemory2(final Object unsafeObj, final long unsafeObjHeader, final long capacity) {
    super(unsafeObj, unsafeObjHeader, capacity);
  }

  //Constructor for ByteBuffer direct
  WritableMemory2(final long nativeBaseOffset, final ByteBuffer byteBuf, final long capacity) {
    super(nativeBaseOffset, byteBuf, capacity);
  }

  //Constructor for ByteBuffer heap
  WritableMemory2(final Object unsafeObj, final long unsafeObjHeader, final ByteBuffer byteBuf,
      final long regionOffset, final long capacity) {
    super(unsafeObj, unsafeObjHeader, byteBuf, regionOffset, capacity);
  }

  //Constructor for regions: everything
  WritableMemory2(final long nativeBaseOffset, final Object unsafeObj, final long unsafeObjHeader,
      final ByteBuffer byteBuf, final long regionOffset, final long capacity,
      final MemoryRequest memReq, final AtomicBoolean valid) {
    super(nativeBaseOffset, unsafeObj, unsafeObjHeader, byteBuf, regionOffset, capacity, valid);
    this.memReq = memReq;
  }

  //Constructor for direct native allocation
  WritableMemory2(final long nativeBaseOffset, final long capacity, final MemoryRequest memReq) {
    this(nativeBaseOffset, null, capacity);
    this.memReq = memReq;
  }

  public Memory asReadOnly() {
    return new MemoryImpl(nativeBaseOffset, unsafeObj, unsafeObjHeader, byteBuf, regionOffset,
        capacity, valid);
  }

  //ACCESS PRIMITIVE ARRAYS for write

  public static WritableMemory2 writableWrap(final boolean[] arr) {
    return new WritableMemory2(arr, ARRAY_BOOLEAN_BASE_OFFSET, arr.length << BOOLEAN_SHIFT);
  }

  public static WritableMemory2 writableWrap(final byte[] arr) {
    return new WritableMemory2(arr, ARRAY_BYTE_BASE_OFFSET, arr.length << BYTE_SHIFT);
  }

  public static WritableMemory2 writableWrap(final char[] arr) {
    return new WritableMemory2(arr, ARRAY_CHAR_BASE_OFFSET, arr.length << CHAR_SHIFT);
  }

  public static WritableMemory2 writableWrap(final short[] arr) {
    return new WritableMemory2(arr, ARRAY_SHORT_BASE_OFFSET, arr.length << SHORT_SHIFT);
  }

  public static WritableMemory2 writableWrap(final int[] arr) {
    return new WritableMemory2(arr, ARRAY_INT_BASE_OFFSET, arr.length << INT_SHIFT);
  }

  public static WritableMemory2 writableWrap(final long[] arr) {
    return new WritableMemory2(arr, ARRAY_LONG_BASE_OFFSET, arr.length << LONG_SHIFT);
  }

  public static WritableMemory2 writableWrap(final float[] arr) {
    return new WritableMemory2(arr, ARRAY_FLOAT_BASE_OFFSET, arr.length << FLOAT_SHIFT);
  }

  public static WritableMemory2 writableWrap(final double[] arr) {
    return new WritableMemory2(arr, ARRAY_DOUBLE_BASE_OFFSET, arr.length << DOUBLE_SHIFT);
  }

  //Allocations using native memory and heap

  //ALLOCATE DIRECT memory
  public static WritableMemory2 allocateDirect(final long capacity, final MemoryRequest memReq) {
    return AllocateDirect.allocDirect(capacity, memReq);
  }

  public static WritableMemory2 allocateDirect(final long capacity) {
    return AllocateDirect.allocDirect(capacity, null);
  }

  //ALLOCATE HEAP VIA AUTOMATIC BYTE ARRAY
  public static WritableMemory2 allocate(final int capacity) {
    final byte[] unsafeObj = new byte[capacity];
    return new WritableMemory2(unsafeObj, ARRAY_BYTE_BASE_OFFSET, capacity);
  }


  //ByteBuffer
  /**
   * Provides access to the backing store of the given ByteBuffer.
   * If the given <i>ByteBuffer</i> is read-only, the returned <i>Memory</i> will also be a
   * read-only instance.
   * @param byteBuffer the given <i>ByteBuffer</i>
   * @return a <i>Memory</i> object
   */
  public static WritableMemory2 writableWrap(final ByteBuffer byteBuffer) {
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
        return new WritableMemory2(nativeBaseAddress, byteBuffer, capacity);
      }

      //WRITABLE-HEAP
      unsafeObj = byteBuf.array();
      unsafeObjHeader = ARRAY_BYTE_BASE_OFFSET;
      regionOffset = byteBuf.arrayOffset() * ARRAY_BYTE_INDEX_SCALE;
      return new WritableMemory2(unsafeObj, unsafeObjHeader, byteBuffer, regionOffset, capacity);
    }
  }

  //Map

  @SuppressWarnings("unused")
  public static WritableMemory2 writableMap(final File file, final long offset, final long capacity) {
    //-> MapDW
    return null;
  }

  //Primitive Puts

  /**
   * Puts the boolean value at the given offset
   * @param offsetBytes offset bytes relative to this Memory start
   * @param value the value to put
   */
  public void putBoolean(final long offsetBytes, final boolean value) {
    checkValid();
    assertBounds(offsetBytes, ARRAY_BOOLEAN_INDEX_SCALE, capacity);
    unsafe.putBoolean(unsafeObj, cumBaseOffset + offsetBytes, value);
  }

  /**
   * Puts the byte value at the given offset
   * @param offsetBytes offset bytes relative to this Memory start
   * @param value the value to put
   */
  public void putByte(final long offsetBytes, final byte value) {
    checkValid();
    assertBounds(offsetBytes, ARRAY_BYTE_INDEX_SCALE, capacity);
    unsafe.putByte(unsafeObj, cumBaseOffset + offsetBytes, value);
  }

  /**
   * Puts the char value at the given offset
   * @param offsetBytes offset bytes relative to this Memory start
   * @param value the value to put
   */
  public void putChar(final long offsetBytes, final char value) {
    checkValid();
    assertBounds(offsetBytes, ARRAY_CHAR_INDEX_SCALE, capacity);
    unsafe.putChar(unsafeObj, cumBaseOffset + offsetBytes, value);
  }

  /**
   * Puts the int value at the given offset
   * @param offsetBytes offset bytes relative to this Memory start
   * @param value the value to put
   */
  public void putInt(final long offsetBytes, final int value) {
    checkValid();
    assertBounds(offsetBytes, ARRAY_INT_INDEX_SCALE, capacity);
    unsafe.putInt(unsafeObj, cumBaseOffset + offsetBytes, value);
  }

  /**
   * Puts the long value at the given offset
   * @param offsetBytes offset bytes relative to this Memory start
   * @param value the value to put
   */
  public void putLong(final long offsetBytes, final long value) {
    checkValid();
    assertBounds(offsetBytes, ARRAY_LONG_INDEX_SCALE, capacity);
    unsafe.putLong(unsafeObj, cumBaseOffset + offsetBytes, value);
  }

  /**
   * Puts the float value at the given offset
   * @param offsetBytes offset bytes relative to this Memory start
   * @param value the value to put
   */
  public void putFloat(final long offsetBytes, final float value) {
    checkValid();
    assertBounds(offsetBytes, ARRAY_FLOAT_INDEX_SCALE, capacity);
    unsafe.putFloat(unsafeObj, cumBaseOffset + offsetBytes, value);
  }

  /**
   * Puts the double value at the given offset
   * @param offsetBytes offset bytes relative to this Memory start
   * @param value the value to put
   */
  public void putDouble(final long offsetBytes, final double value) {
    checkValid();
    assertBounds(offsetBytes, ARRAY_DOUBLE_INDEX_SCALE, capacity);
    unsafe.putDouble(unsafeObj, cumBaseOffset + offsetBytes, value);
  }

  //Primitive Put Arrays

  /**
   * Puts the boolean array at the given offset
   * @param offsetBytes offset bytes relative to this Memory start
   * @param srcArray The source array.
   * @param srcOffset offset in array units
   * @param length number of array units to transfer
   */
  public void putBooleanArray(final long offsetBytes, final boolean[] srcArray, final int srcOffset,
      final int length) {
    checkValid();
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

  /**
   * Puts the long array at the given offset
   * @param offsetBytes offset bytes relative to this Memory start
   * @param srcArray The source array.
   * @param srcOffset offset in array units
   * @param length number of array units to transfer
   */
  public void putLongArray(final long offsetBytes, final long[] srcArray, final int srcOffset,
      final int length) {
    checkValid();
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
  /**
   * blah
   * @param offsetBytes blah
   * @param capacityBytes blah
   * @return blah
   */
  public WritableMemory2 writableRegion(final long offsetBytes, final long capacityBytes) {
    assert offsetBytes + capacityBytes <= capacity
        : "newOff + newCap: " + (offsetBytes + capacityBytes) + ", origCap: " + capacity;
    final long newRegionOffset = this.regionOffset + offsetBytes;
    final long newCapacity = capacityBytes;
    return new WritableMemory2(nativeBaseOffset, unsafeObj, unsafeObjHeader, byteBuf,
        newRegionOffset, newCapacity, memReq, valid);
  }

  public MemoryRequest getMemoryRequest() {
    return memReq;
  }

  /**
   * blah
   */
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
}
