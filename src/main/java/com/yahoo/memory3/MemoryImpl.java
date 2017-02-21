/*
 * Copyright 2017, Yahoo! Inc. Licensed under the terms of the
 * Apache License 2.0. See LICENSE file at the project root for terms.
 */

package com.yahoo.memory3;

import static com.yahoo.memory.UnsafeUtil.ARRAY_BOOLEAN_BASE_OFFSET;
import static com.yahoo.memory.UnsafeUtil.ARRAY_BOOLEAN_INDEX_SCALE;
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
import java.util.concurrent.atomic.AtomicBoolean;

@SuppressWarnings("unused")
class MemoryImpl extends Memory {
  final long nativeBaseOffset;
  final Object unsafeObj;
  final long unsafeObjHeader;
  final ByteBuffer byteBuf;
  final long regionOffset;
  final long capacity;
  final long cumBaseOffset;
  final MemoryRequest memReq;
  final AtomicBoolean valid;

  //Constructor for heap array access and auto byte[] allocation
  MemoryImpl(final Object unsafeObj, final long unsafeObjHeader, final long capacity) {
    this(0L, unsafeObj, unsafeObjHeader, null, 0L, capacity, null);
  }

  //Constructor for ByteBuffer direct
  MemoryImpl(final long nativeBaseOffset, final ByteBuffer byteBuf, final long capacity) {
    this(nativeBaseOffset, null, 0L, byteBuf, 0L, capacity, null);
  }

  //Constructor for ByteBuffer heap
  MemoryImpl(final Object unsafeObj, final long unsafeObjHeader, final ByteBuffer byteBuf,
      final long regionOffset, final long capacity) {
    this(0L, unsafeObj, unsafeObjHeader, byteBuf, regionOffset, capacity, null);
  }

  //Constructor for the above. Sets valid to true for Heap and ByteBuffer cases
  MemoryImpl(final long nativeBaseOffset, final Object unsafeObj, final long unsafeObjHeader,
      final ByteBuffer byteBuf, final long regionOffset, final long capacity,
      final MemoryRequest memReq) {
    this(nativeBaseOffset, unsafeObj, unsafeObjHeader, byteBuf, regionOffset, capacity, null,
        new AtomicBoolean(true));
  }

  //Constructor for Regions, AllocateDirect
  MemoryImpl(final long nativeBaseOffset, final Object unsafeObj, final long unsafeObjHeader,
      final ByteBuffer byteBuf, final long regionOffset, final long capacity,
      final MemoryRequest memReq, final AtomicBoolean valid) {
    this.nativeBaseOffset = nativeBaseOffset;
    this.unsafeObj = unsafeObj;
    this.unsafeObjHeader = unsafeObjHeader;
    this.byteBuf = byteBuf;
    this.regionOffset = regionOffset;
    this.capacity = capacity;
    this.cumBaseOffset = regionOffset + ((unsafeObj == null) ? nativeBaseOffset : unsafeObjHeader);
    this.memReq = memReq;
    this.valid = valid;
    assert ((unsafeObj == null) ^ (unsafeObjHeader > 0));
    assert valid.get();
  }

  @Override
  public Memory asReadOnly() {
    return null; //TODO
  }

  //REGIONS

  @Override
  public Memory region(final long offsetBytes, final long capacityBytes) {
    assert offsetBytes + capacityBytes <= capacity
      : "newOff + newCap: " + (offsetBytes + capacityBytes) + ", origCap: " + capacity;
    final long newRegionOffset = this.regionOffset + offsetBytes;
    final long newCapacity = capacityBytes;
    //TODO if RO set memReq = null
    final MemoryRequest myMemReq = memReq;
    return new MemoryImpl(nativeBaseOffset, unsafeObj, unsafeObjHeader, byteBuf,
        newRegionOffset, newCapacity, myMemReq, valid);
  }

  //END OF CONSTRUCTOR-TYPE METHODS

  //PRIMITIVE GETS

  @Override
  public boolean getBoolean(final long offsetBytes) {
    checkValid();
    assertBounds(offsetBytes, ARRAY_BOOLEAN_INDEX_SCALE, capacity);
    return unsafe.getBoolean(unsafeObj, cumBaseOffset + offsetBytes);
  }

  @Override
  public byte getByte(final long offsetBytes) {
    checkValid();
    assertBounds(offsetBytes, ARRAY_BYTE_INDEX_SCALE, capacity);
    return unsafe.getByte(unsafeObj, cumBaseOffset + offsetBytes);
  }

  @Override
  public char getChar(final long offsetBytes) {
    checkValid();
    assertBounds(offsetBytes, ARRAY_CHAR_INDEX_SCALE, capacity);
    return unsafe.getChar(unsafeObj, cumBaseOffset + offsetBytes);
  }

  @Override
  public short getShort(final long offsetBytes) {
    checkValid();
    assertBounds(offsetBytes, ARRAY_SHORT_INDEX_SCALE, capacity);
    return unsafe.getShort(unsafeObj, cumBaseOffset + offsetBytes);
  }

  @Override
  public int getInt(final long offsetBytes) {
    checkValid();
    assertBounds(offsetBytes, ARRAY_INT_INDEX_SCALE, capacity);
    return unsafe.getInt(unsafeObj, cumBaseOffset + offsetBytes);
  }

  @Override
  public long getLong(final long offsetBytes) {
    checkValid();
    assertBounds(offsetBytes, ARRAY_LONG_INDEX_SCALE, capacity);
    return unsafe.getLong(unsafeObj, cumBaseOffset + offsetBytes);
  }

  @Override
  public float getFloat(final long offsetBytes) {
    checkValid();
    assertBounds(offsetBytes, ARRAY_FLOAT_INDEX_SCALE, capacity);
    return unsafe.getFloat(unsafeObj, cumBaseOffset + offsetBytes);
  }

  @Override
  public double getDouble(final long offsetBytes) {
    checkValid();
    assertBounds(offsetBytes, ARRAY_DOUBLE_INDEX_SCALE, capacity);
    return unsafe.getDouble(unsafeObj, cumBaseOffset + offsetBytes);
  }

  //PRIMITIVE GET ARRAYS

  @Override
  public void getBooleanArray(final long offsetBytes, final boolean[] dstArray, final int dstOffset,
      final int length) {
    checkValid();
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

  //plus 6 more
  @Override
  public void getLongArray(final long offsetBytes, final long[] dstArray, final int dstOffset,
      final int length) {
    checkValid();
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

  //PRIMITIVE PUTS

  /**
   * Puts the boolean value at the given offset
   * @param offsetBytes offset bytes relative to this Memory start
   * @param value the value to put
   */
  @Override
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
  @Override
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
  @Override
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
  @Override
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
  @Override
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
  @Override
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
  @Override
  public void putDouble(final long offsetBytes, final double value) {
    checkValid();
    assertBounds(offsetBytes, ARRAY_DOUBLE_INDEX_SCALE, capacity);
    unsafe.putDouble(unsafeObj, cumBaseOffset + offsetBytes, value);
  }

  //PRIMITIVE PUT ARRAYS

  /**
   * Puts the boolean array at the given offset
   * @param offsetBytes offset bytes relative to this Memory start
   * @param srcArray The source array.
   * @param srcOffset offset in array units
   * @param length number of array units to transfer
   */
  @Override
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
  @Override
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

  //OTHER

  @Override
  public MemoryRequest getMemoryRequest() {
    return memReq;
  }

  @Override
  public void freeMemory() {}

  boolean isValid() { return valid.get(); }

  final void checkValid() {
    assert isValid() : "Memory not valid.";
  }

}
