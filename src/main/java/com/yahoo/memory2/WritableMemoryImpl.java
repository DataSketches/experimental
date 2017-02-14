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

@SuppressWarnings("unused")
class WritableMemoryImpl extends WritableMemory {
  private long nativeBaseOffset;
  private final Object memObj;
  private final long objectOffset;
  private final long cumBaseOffset;
  private long arrayOffset;
  private final long capacity;
  private MemoryRequest memReq = null;

  /**
   * @param cumBaseOffset blah
   * @param arrayOffset blah
   * @param capacity blah
   */
  WritableMemoryImpl(final long nativeBaseOffset, final Object memObj, final long objectOffset,
      final long arrayOffset, final long capacity, final MemoryRequest memReq) {
    this.nativeBaseOffset = nativeBaseOffset;
    this.memObj = memObj;
    this.objectOffset = objectOffset;
    this.cumBaseOffset = (memObj == null)
        ? nativeBaseOffset + arrayOffset
        : objectOffset + arrayOffset;
    this.arrayOffset = arrayOffset;
    this.capacity = capacity;
    this.memReq = memReq;
  }

  static WritableMemoryImpl allocateArray(
      final int capacity, final MemoryRequest memReq) {
    final byte[] memObj = new byte[capacity];
    return new WritableMemoryImpl(0L, memObj, ARRAY_BYTE_BASE_OFFSET, 0L,
        capacity, memReq);
  }

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
    return unsafe.getLong(null, cumBaseOffset + offsetBytes);
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
    memReq = null;
  }

}
