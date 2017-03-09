/*
 * Copyright 2017, Yahoo! Inc. Licensed under the terms of the
 * Apache License 2.0. See LICENSE file at the project root for terms.
 */

package com.yahoo.memory4;

import static com.yahoo.memory4.UnsafeUtil.ARRAY_BOOLEAN_BASE_OFFSET;
import static com.yahoo.memory4.UnsafeUtil.ARRAY_BOOLEAN_INDEX_SCALE;
import static com.yahoo.memory4.UnsafeUtil.ARRAY_BYTE_BASE_OFFSET;
import static com.yahoo.memory4.UnsafeUtil.ARRAY_BYTE_INDEX_SCALE;
import static com.yahoo.memory4.UnsafeUtil.ARRAY_CHAR_BASE_OFFSET;
import static com.yahoo.memory4.UnsafeUtil.ARRAY_CHAR_INDEX_SCALE;
import static com.yahoo.memory4.UnsafeUtil.ARRAY_DOUBLE_BASE_OFFSET;
import static com.yahoo.memory4.UnsafeUtil.ARRAY_DOUBLE_INDEX_SCALE;
import static com.yahoo.memory4.UnsafeUtil.ARRAY_FLOAT_BASE_OFFSET;
import static com.yahoo.memory4.UnsafeUtil.ARRAY_FLOAT_INDEX_SCALE;
import static com.yahoo.memory4.UnsafeUtil.ARRAY_INT_BASE_OFFSET;
import static com.yahoo.memory4.UnsafeUtil.ARRAY_INT_INDEX_SCALE;
import static com.yahoo.memory4.UnsafeUtil.ARRAY_LONG_BASE_OFFSET;
import static com.yahoo.memory4.UnsafeUtil.ARRAY_LONG_INDEX_SCALE;
import static com.yahoo.memory4.UnsafeUtil.ARRAY_SHORT_BASE_OFFSET;
import static com.yahoo.memory4.UnsafeUtil.ARRAY_SHORT_INDEX_SCALE;
import static com.yahoo.memory4.UnsafeUtil.BOOLEAN_SHIFT;
import static com.yahoo.memory4.UnsafeUtil.BYTE_SHIFT;
import static com.yahoo.memory4.UnsafeUtil.CHAR_SHIFT;
import static com.yahoo.memory4.UnsafeUtil.DOUBLE_SHIFT;
import static com.yahoo.memory4.UnsafeUtil.FLOAT_SHIFT;
import static com.yahoo.memory4.UnsafeUtil.INT_SHIFT;
import static com.yahoo.memory4.UnsafeUtil.LONG_SHIFT;
import static com.yahoo.memory4.UnsafeUtil.LS;
import static com.yahoo.memory4.UnsafeUtil.SHORT_SHIFT;
import static com.yahoo.memory4.UnsafeUtil.UNSAFE_COPY_THRESHOLD;
import static com.yahoo.memory4.UnsafeUtil.assertBounds;
import static com.yahoo.memory4.UnsafeUtil.unsafe;

/**
 * @author Lee Rhodes
 */
class MemoryImpl extends Memory {
  final MemoryState state;
  final Object unsafeObj; //Array objects are held here.
  final long unsafeObjHeader; //Heap ByteBuffer includes the slice() offset here.
  final long capacity;
  final long cumBaseOffset; //Holds the cum offset to the start of data.

  //Handles arrays
  MemoryImpl(final MemoryState state) {
    this.state = state;
    this.unsafeObj = state.getUnsafeObject();
    this.unsafeObjHeader = state.getUnsafeObjectHeader();
    this.capacity = state.getCapacity();
    this.cumBaseOffset = state.getCumBaseOffset();
  }

  //REGIONS

  @Override
  public Memory region(final long offsetBytes, final long capacityBytes) {
    assert offsetBytes + capacityBytes <= capacity
        : "newOff + newCap: " + (offsetBytes + capacityBytes) + ", origCap: " + this.capacity;
    final MemoryState newState = this.state.copy();
    newState.putRegionOffset(newState.getRegionOffset() + offsetBytes);
    newState.putCapacity(capacityBytes);
    return new MemoryImpl(newState);
  }

  //PRIMITIVE getXXX() and getXXXArray() //XXX

  @Override
  public boolean getBoolean(final long offsetBytes) {
    checkValid();
    assertBounds(offsetBytes, ARRAY_BOOLEAN_INDEX_SCALE, capacity);
    return unsafe.getBoolean(unsafeObj, cumBaseOffset + offsetBytes);
  }

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

  @Override
  public byte getByte(final long offsetBytes) {
    checkValid();
    assertBounds(offsetBytes, ARRAY_BYTE_INDEX_SCALE, capacity);
    return unsafe.getByte(unsafeObj, cumBaseOffset + offsetBytes);
  }

  @Override
  public void getByteArray(final long offsetBytes, final byte[] dstArray, final int dstOffset,
      final int length) {
    checkValid();
    final long copyBytes = length << BYTE_SHIFT;
    assertBounds(offsetBytes, copyBytes, capacity);
    assertBounds(dstOffset, length, dstArray.length);
    unsafe.copyMemory(
      unsafeObj,
      cumBaseOffset,
      dstArray,
      ARRAY_BYTE_BASE_OFFSET + (dstOffset << BYTE_SHIFT),
      copyBytes);
  }

  @Override
  public char getChar(final long offsetBytes) {
    checkValid();
    assertBounds(offsetBytes, ARRAY_CHAR_INDEX_SCALE, capacity);
    return unsafe.getChar(unsafeObj, cumBaseOffset + offsetBytes);
  }

  @Override
  public void getCharArray(final long offsetBytes, final char[] dstArray, final int dstOffset,
      final int length) {
    checkValid();
    final long copyBytes = length << CHAR_SHIFT;
    assertBounds(offsetBytes, copyBytes, capacity);
    assertBounds(dstOffset, length, dstArray.length);
    unsafe.copyMemory(
      unsafeObj,
      cumBaseOffset,
      dstArray,
      ARRAY_CHAR_BASE_OFFSET + (dstOffset << CHAR_SHIFT),
      copyBytes);
  }

  @Override
  public double getDouble(final long offsetBytes) {
    checkValid();
    assertBounds(offsetBytes, ARRAY_DOUBLE_INDEX_SCALE, capacity);
    return unsafe.getDouble(unsafeObj, cumBaseOffset + offsetBytes);
  }

  @Override
  public void getDoubleArray(final long offsetBytes, final double[] dstArray, final int dstOffset,
      final int length) {
    checkValid();
    final long copyBytes = length << DOUBLE_SHIFT;
    assertBounds(offsetBytes, copyBytes, capacity);
    assertBounds(dstOffset, length, dstArray.length);
    unsafe.copyMemory(
      unsafeObj,
      cumBaseOffset,
      dstArray,
      ARRAY_DOUBLE_BASE_OFFSET + (dstOffset << DOUBLE_SHIFT),
      copyBytes);
  }

  @Override
  public float getFloat(final long offsetBytes) {
    checkValid();
    assertBounds(offsetBytes, ARRAY_FLOAT_INDEX_SCALE, capacity);
    return unsafe.getFloat(unsafeObj, cumBaseOffset + offsetBytes);
  }

  @Override
  public void getFloatArray(final long offsetBytes, final float[] dstArray, final int dstOffset,
      final int length) {
    checkValid();
    final long copyBytes = length << FLOAT_SHIFT;
    assertBounds(offsetBytes, copyBytes, capacity);
    assertBounds(dstOffset, length, dstArray.length);
    unsafe.copyMemory(
      unsafeObj,
      cumBaseOffset,
      dstArray,
      ARRAY_FLOAT_BASE_OFFSET + (dstOffset << FLOAT_SHIFT),
      copyBytes);
  }

  @Override
  public int getInt(final long offsetBytes) {
    checkValid();
    assertBounds(offsetBytes, ARRAY_INT_INDEX_SCALE, capacity);
    return unsafe.getInt(unsafeObj, cumBaseOffset + offsetBytes);
  }

  @Override
  public void getIntArray(final long offsetBytes, final int[] dstArray, final int dstOffset,
      final int length) {
    checkValid();
    final long copyBytes = length << INT_SHIFT;
    assertBounds(offsetBytes, copyBytes, capacity);
    assertBounds(dstOffset, length, dstArray.length);
    unsafe.copyMemory(
      unsafeObj,
      cumBaseOffset,
      dstArray,
      ARRAY_INT_BASE_OFFSET + (dstOffset << INT_SHIFT),
      copyBytes);
  }

  @Override
  public long getLong(final long offsetBytes) {
    checkValid();
    assertBounds(offsetBytes, ARRAY_LONG_INDEX_SCALE, capacity);
    return unsafe.getLong(unsafeObj, cumBaseOffset + offsetBytes);
  }

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

  @Override
  public short getShort(final long offsetBytes) {
    checkValid();
    assertBounds(offsetBytes, ARRAY_SHORT_INDEX_SCALE, capacity);
    return unsafe.getShort(unsafeObj, cumBaseOffset + offsetBytes);
  }

  @Override
  public void getShortArray(final long offsetBytes, final short[] dstArray, final int dstOffset,
      final int length) {
    checkValid();
    final long copyBytes = length << SHORT_SHIFT;
    assertBounds(offsetBytes, copyBytes, capacity);
    assertBounds(dstOffset, length, dstArray.length);
    unsafe.copyMemory(
      unsafeObj,
      cumBaseOffset,
      dstArray,
      ARRAY_SHORT_BASE_OFFSET + (dstOffset << SHORT_SHIFT),
      copyBytes);
  }

  //OTHER PRIMITIVE READ METHODS: copy, isYYYY(), areYYYY() //XXX

  @Override
  public void copyTo(final long srcOffsetBytes, final WritableMemory destination,
      final long dstOffsetBytes, final long lengthBytes) {

    assertBounds(srcOffsetBytes, lengthBytes, this.capacity);
    assertBounds(dstOffsetBytes, lengthBytes, destination.getCapacity());

    long srcAdd = this.getCumulativeOffset(srcOffsetBytes);
    long dstAdd = destination.getCumulativeOffset(dstOffsetBytes);
    final Object srcParent = (this.isDirect()) ? null : unsafeObj;
    final Object dstParent = (destination.isDirect()) ? null : destination.getArray();
    long lenBytes = lengthBytes;

    while (lenBytes > 0) {
      final long chunkBytes = (lenBytes > UNSAFE_COPY_THRESHOLD) ? UNSAFE_COPY_THRESHOLD : lenBytes;
      unsafe.copyMemory(srcParent, srcAdd, dstParent, dstAdd, lenBytes);
      lenBytes -= chunkBytes;
      srcAdd += chunkBytes;
      dstAdd += chunkBytes;
    }
  }

  @Override
  public boolean isAllBitsClear(final long offsetBytes, final byte bitMask) {
    assertBounds(offsetBytes, ARRAY_BYTE_INDEX_SCALE, capacity);
    final int value = ~unsafe.getByte(unsafeObj, cumBaseOffset + offsetBytes) & bitMask & 0XFF;
    return value == bitMask;
  }

  @Override
  public boolean isAllBitsSet(final long offsetBytes, final byte bitMask) {
    assertBounds(offsetBytes, ARRAY_BYTE_INDEX_SCALE, capacity);
    final int value = unsafe.getByte(unsafeObj, cumBaseOffset + offsetBytes) & bitMask & 0XFF;
    return value == bitMask;
  }

  @Override
  public boolean isAnyBitsClear(final long offsetBytes, final byte bitMask) {
    assertBounds(offsetBytes, ARRAY_BYTE_INDEX_SCALE, capacity);
    final int value = ~unsafe.getByte(unsafeObj, cumBaseOffset + offsetBytes) & bitMask & 0XFF;
    return value != 0;
  }

  @Override
  public boolean isAnyBitsSet(final long offsetBytes, final byte bitMask) {
    assertBounds(offsetBytes, ARRAY_BYTE_INDEX_SCALE, capacity);
    final int value = unsafe.getByte(unsafeObj, cumBaseOffset + offsetBytes) & bitMask & 0XFF;
    return value != 0;
  }

  //OTHER READ METHODS //XXX

  @Override
  public long getCapacity() {
    return capacity;
  }

  @Override
  public long getCumulativeOffset(final long offsetBytes) {
    return cumBaseOffset;
  }

  @Override
  public boolean hasArray() {
    return unsafeObj != null;
  }

  @Override
  public boolean hasByteBuffer() {
    return state.getByteBuffer() != null;
  }

  @Override
  public boolean isDirect() {
    return state.isDirect();
  }

  @Override
  public boolean isReadOnly() { //TODO may not need this
    return state.isResourceReadOnly();
  }

  @Override
  public boolean isValid() {
    return state.isValid();
  }

  @Override
  public String toHexString(final String header, final long offsetBytes, final int lengthBytes) {
    checkValid();
    final StringBuilder sb = new StringBuilder();
    sb.append(header).append(LS);
    final String s1 = String.format("(..., %d, %d)", offsetBytes, lengthBytes);
    sb.append(this.getClass().getSimpleName()).append(".toHexString")
      .append(s1).append(", hash: ").append(this.hashCode()).append(LS);

    return Memory.toHex(sb.toString(), offsetBytes, lengthBytes, this.state);
  }

  //RESTRICTED
  private final void checkValid() { //applies to both readable and writable
    assert this.state.isValid() : "Memory not valid.";
  }



}
