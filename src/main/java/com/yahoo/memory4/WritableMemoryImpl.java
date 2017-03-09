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
import static com.yahoo.memory4.UnsafeUtil.checkOverlap;
import static com.yahoo.memory4.UnsafeUtil.unsafe;

/**
 * @author Lee Rhodes
 */
class WritableMemoryImpl extends WritableMemory {
  final MemoryState state;
  final Object unsafeObj; //Array objects are held here.
  final long unsafeObjHeader; //Heap ByteBuffer includes the slice() offset here.
  final long capacity;
  final long cumBaseOffset; //Holds the cum offset to the start of data.

  WritableMemoryImpl(final MemoryState state) {
    this.state = state;
    this.unsafeObj = state.getUnsafeObject();
    this.unsafeObjHeader = state.getUnsafeObjectHeader();
    this.capacity = state.getCapacity();
    this.cumBaseOffset = state.getCumBaseOffset();
  }

  //REGIONS
  @Override
  public WritableMemory region(final long offsetBytes, final long capacityBytes) {
    assert offsetBytes + capacityBytes <= capacity
        : "newOff + newCap: " + (offsetBytes + capacityBytes) + ", origCap: " + this.capacity;
    final MemoryState newState = this.state.copy();
    newState.putRegionOffset(newState.getRegionOffset() + offsetBytes);
    newState.putCapacity(capacityBytes);
    return new WritableMemoryImpl(newState);
  }

  //AS READ ONLY

  @Override
  public Memory asReadOnly() {
    final MemoryState newState = this.state.copy();
    newState.setResourceReadOnly();
    return new MemoryImpl(newState);
  }

  ///PRIMITIVE getXXX() and getXXXArray() //XXX

  @Override
  public boolean getBoolean(final long offsetBytes) {
    checkValid();
    assertBounds(offsetBytes, ARRAY_BOOLEAN_INDEX_SCALE, this.capacity);
    return unsafe.getBoolean(this.unsafeObj, this.cumBaseOffset + offsetBytes);
  }

  @Override
  public void getBooleanArray(final long offsetBytes, final boolean[] dstArray, final int dstOffset,
      final int length) {
    checkValid();
    final long copyBytes = length << BOOLEAN_SHIFT;
    assertBounds(offsetBytes, copyBytes, this.capacity);
    assertBounds(dstOffset, length, dstArray.length);
    unsafe.copyMemory(
      this.unsafeObj,
      this.cumBaseOffset,
      dstArray,
      ARRAY_BOOLEAN_BASE_OFFSET + (dstOffset << BOOLEAN_SHIFT),
      copyBytes);
  }

  @Override
  public byte getByte(final long offsetBytes) {
    checkValid();
    assertBounds(offsetBytes, ARRAY_BYTE_INDEX_SCALE, capacity);
    return unsafe.getByte(this.unsafeObj, this.cumBaseOffset + offsetBytes);
  }

  @Override
  public void getByteArray(final long offsetBytes, final byte[] dstArray, final int dstOffset,
      final int length) {
    checkValid();
    final long copyBytes = length << BYTE_SHIFT;
    assertBounds(offsetBytes, copyBytes, this.capacity);
    assertBounds(dstOffset, length, dstArray.length);
    unsafe.copyMemory(
      this.unsafeObj,
      this.cumBaseOffset,
      dstArray,
      ARRAY_BYTE_BASE_OFFSET + (dstOffset << BYTE_SHIFT),
      copyBytes);
  }

  @Override
  public char getChar(final long offsetBytes) {
    checkValid();
    assertBounds(offsetBytes, ARRAY_CHAR_INDEX_SCALE, this.capacity);
    return unsafe.getChar(this.unsafeObj, this.cumBaseOffset + offsetBytes);
  }

  @Override
  public void getCharArray(final long offsetBytes, final char[] dstArray, final int dstOffset,
      final int length) {
    checkValid();
    final long copyBytes = length << CHAR_SHIFT;
    assertBounds(offsetBytes, copyBytes, this.capacity);
    assertBounds(dstOffset, length, dstArray.length);
    unsafe.copyMemory(
      this.unsafeObj,
      this.cumBaseOffset,
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
    assertBounds(offsetBytes, copyBytes, this.capacity);
    assertBounds(dstOffset, length, dstArray.length);
    unsafe.copyMemory(
      this.unsafeObj,
      this.cumBaseOffset,
      dstArray,
      ARRAY_DOUBLE_BASE_OFFSET + (dstOffset << DOUBLE_SHIFT),
      copyBytes);
  }

  @Override
  public float getFloat(final long offsetBytes) {
    checkValid();
    assertBounds(offsetBytes, ARRAY_FLOAT_INDEX_SCALE, this.capacity);
    return unsafe.getFloat(this.unsafeObj, this.cumBaseOffset + offsetBytes);
  }

  @Override
  public void getFloatArray(final long offsetBytes, final float[] dstArray, final int dstOffset,
      final int length) {
    checkValid();
    final long copyBytes = length << FLOAT_SHIFT;
    assertBounds(offsetBytes, copyBytes, this.capacity);
    assertBounds(dstOffset, length, dstArray.length);
    unsafe.copyMemory(
      this.unsafeObj,
      this.cumBaseOffset,
      dstArray,
      ARRAY_FLOAT_BASE_OFFSET + (dstOffset << FLOAT_SHIFT),
      copyBytes);
  }

  @Override
  public int getInt(final long offsetBytes) {
    checkValid();
    assertBounds(offsetBytes, ARRAY_INT_INDEX_SCALE, this.capacity);
    return unsafe.getInt(this.unsafeObj, this.cumBaseOffset + offsetBytes);
  }

  @Override
  public void getIntArray(final long offsetBytes, final int[] dstArray, final int dstOffset,
      final int length) {
    checkValid();
    final long copyBytes = length << INT_SHIFT;
    assertBounds(offsetBytes, copyBytes, this.capacity);
    assertBounds(dstOffset, length, dstArray.length);
    unsafe.copyMemory(
      this.unsafeObj,
      this.cumBaseOffset,
      dstArray,
      ARRAY_INT_BASE_OFFSET + (dstOffset << INT_SHIFT),
      copyBytes);
  }

  @Override
  public long getLong(final long offsetBytes) {
    checkValid();
    assertBounds(offsetBytes, ARRAY_LONG_INDEX_SCALE, this.capacity);
    return unsafe.getLong(this.unsafeObj, this.cumBaseOffset + offsetBytes);
  }

  @Override
  public void getLongArray(final long offsetBytes, final long[] dstArray, final int dstOffset,
      final int length) {
    checkValid();
    final long copyBytes = length << LONG_SHIFT;
    assertBounds(offsetBytes, copyBytes, this.capacity);
    assertBounds(dstOffset, length, dstArray.length);
    unsafe.copyMemory(
      this.unsafeObj,
      this.cumBaseOffset,
      dstArray,
      ARRAY_LONG_BASE_OFFSET + (dstOffset << LONG_SHIFT),
      copyBytes);
  }

  @Override
  public short getShort(final long offsetBytes) {
    checkValid();
    assertBounds(offsetBytes, ARRAY_SHORT_INDEX_SCALE, this.capacity);
    return unsafe.getShort(this.unsafeObj, this.cumBaseOffset + offsetBytes);
  }

  @Override
  public void getShortArray(final long offsetBytes, final short[] dstArray, final int dstOffset,
      final int length) {
    checkValid();
    final long copyBytes = length << SHORT_SHIFT;
    assertBounds(offsetBytes, copyBytes, this.capacity);
    assertBounds(dstOffset, length, dstArray.length);
    unsafe.copyMemory(
      this.unsafeObj,
      this.cumBaseOffset,
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
    assert (this == destination) ? checkOverlap(srcOffsetBytes, dstOffsetBytes, lengthBytes) : true ;

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

  //OTHER READ METHODS

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
  public boolean isReadOnly() {
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

  //RESTRICTED READ AND WRITE

  private final void checkValid() { //applies to both readable and writable
    assert this.state.isValid() : "Memory not valid.";
  }

  //ALL METHODS BELOW ONLY APPLY TO WRITABLE
  //PRIMITIVE putXXX() and putXXXArray() implementations //XXX

  @Override
  public void putBoolean(final long offsetBytes, final boolean value) {
    checkValid();
    assertBounds(offsetBytes, ARRAY_BOOLEAN_INDEX_SCALE, this.capacity);
    unsafe.putBoolean(this.unsafeObj, this.cumBaseOffset + offsetBytes, value);
  }

  @Override
  public void putBooleanArray(final long offsetBytes, final boolean[] srcArray, final int srcOffset,
      final int length) {
    checkValid();
    final long copyBytes = length << BOOLEAN_SHIFT;
    assertBounds(srcOffset, length, srcArray.length);
    assertBounds(offsetBytes, copyBytes, this.capacity);
    unsafe.copyMemory(
      srcArray,
      ARRAY_BOOLEAN_BASE_OFFSET + (srcOffset << BOOLEAN_SHIFT),
      this.unsafeObj,
      this.cumBaseOffset,
      copyBytes
      );
  }

  @Override
  public void putByte(final long offsetBytes, final byte value) {
    checkValid();
    assertBounds(offsetBytes, ARRAY_BYTE_INDEX_SCALE, this.capacity);
    unsafe.putByte(this.unsafeObj, this.cumBaseOffset + offsetBytes, value);
  }

  @Override
  public void putByteArray(final long offsetBytes, final byte[] srcArray, final int srcOffset,
      final int length) {
    checkValid();
    final long copyBytes = length << BYTE_SHIFT;
    assertBounds(srcOffset, length, srcArray.length);
    assertBounds(offsetBytes, copyBytes, this.capacity);
    unsafe.copyMemory(
      srcArray,
      ARRAY_BYTE_BASE_OFFSET + (srcOffset << BYTE_SHIFT),
      this.unsafeObj,
      cumBaseOffset,
      copyBytes
      );
  }

  @Override
  public void putChar(final long offsetBytes, final char value) {
    checkValid();
    assertBounds(offsetBytes, ARRAY_CHAR_INDEX_SCALE, this.capacity);
    unsafe.putChar(this.unsafeObj, this.cumBaseOffset + offsetBytes, value);
  }

  @Override
  public void putCharArray(final long offsetBytes, final char[] srcArray, final int srcOffset,
      final int length) {
    checkValid();
    final long copyBytes = length << CHAR_SHIFT;
    assertBounds(srcOffset, length, srcArray.length);
    assertBounds(offsetBytes, copyBytes, this.capacity);
    unsafe.copyMemory(
      srcArray,
      ARRAY_CHAR_BASE_OFFSET + (srcOffset << CHAR_SHIFT),
      this.unsafeObj,
      this.cumBaseOffset,
      copyBytes
      );
  }

  @Override
  public void putDouble(final long offsetBytes, final double value) {
    checkValid();
    assertBounds(offsetBytes, ARRAY_DOUBLE_INDEX_SCALE, this.capacity);
    unsafe.putDouble(this.unsafeObj, this.cumBaseOffset + offsetBytes, value);
  }

  @Override
  public void putDoubleArray(final long offsetBytes, final double[] srcArray, final int srcOffset,
      final int length) {
    checkValid();
    final long copyBytes = length << DOUBLE_SHIFT;
    assertBounds(srcOffset, length, srcArray.length);
    assertBounds(offsetBytes, copyBytes, this.capacity);
    unsafe.copyMemory(
      srcArray,
      ARRAY_DOUBLE_BASE_OFFSET + (srcOffset << DOUBLE_SHIFT),
      this.unsafeObj,
      this.cumBaseOffset,
      copyBytes
      );
  }

  @Override
  public void putFloat(final long offsetBytes, final float value) {
    checkValid();
    assertBounds(offsetBytes, ARRAY_FLOAT_INDEX_SCALE, this.capacity);
    unsafe.putFloat(this.unsafeObj, this.cumBaseOffset + offsetBytes, value);
  }

  @Override
  public void putFloatArray(final long offsetBytes, final float[] srcArray, final int srcOffset,
      final int length) {
    checkValid();
    final long copyBytes = length << FLOAT_SHIFT;
    assertBounds(srcOffset, length, srcArray.length);
    assertBounds(offsetBytes, copyBytes, this.capacity);
    unsafe.copyMemory(
      srcArray,
      ARRAY_FLOAT_BASE_OFFSET + (srcOffset << FLOAT_SHIFT),
      this.unsafeObj,
      this.cumBaseOffset,
      copyBytes
      );
  }

  @Override
  public void putInt(final long offsetBytes, final int value) {
    checkValid();
    assertBounds(offsetBytes, ARRAY_INT_INDEX_SCALE, this.capacity);
    unsafe.putInt(this.unsafeObj, this.cumBaseOffset + offsetBytes, value);
  }

  @Override
  public void putIntArray(final long offsetBytes, final int[] srcArray, final int srcOffset,
      final int length) {
    checkValid();
    final long copyBytes = length << INT_SHIFT;
    assertBounds(srcOffset, length, srcArray.length);
    assertBounds(offsetBytes, copyBytes, this.capacity);
    unsafe.copyMemory(
      srcArray,
      ARRAY_INT_BASE_OFFSET + (srcOffset << INT_SHIFT),
      this.unsafeObj,
      this.cumBaseOffset,
      copyBytes
      );
  }

  @Override
  public void putLong(final long offsetBytes, final long value) {
    checkValid();
    assertBounds(offsetBytes, ARRAY_LONG_INDEX_SCALE, this.capacity);
    unsafe.putLong(this.unsafeObj, this.cumBaseOffset + offsetBytes, value);
  }

  @Override
  public void putLongArray(final long offsetBytes, final long[] srcArray, final int srcOffset,
      final int length) {
    checkValid();
    final long copyBytes = length << LONG_SHIFT;
    assertBounds(srcOffset, length, srcArray.length);
    assertBounds(offsetBytes, copyBytes, this.capacity);
    unsafe.copyMemory(
      srcArray,
      ARRAY_LONG_BASE_OFFSET + (srcOffset << LONG_SHIFT),
      this.unsafeObj,
      this.cumBaseOffset,
      copyBytes
      );
  }

  @Override
  public void putShort(final long offsetBytes, final short value) {
    checkValid();
    assertBounds(offsetBytes, ARRAY_SHORT_INDEX_SCALE, this.capacity);
    unsafe.putLong(this.unsafeObj, this.cumBaseOffset + offsetBytes, value);
  }

  @Override
  public void putShortArray(final long offsetBytes, final short[] srcArray, final int srcOffset,
      final int length) {
    checkValid();
    final long copyBytes = length << SHORT_SHIFT;
    assertBounds(srcOffset, length, srcArray.length);
    assertBounds(offsetBytes, copyBytes, this.capacity);
    unsafe.copyMemory(
      srcArray,
      ARRAY_SHORT_BASE_OFFSET + (srcOffset << SHORT_SHIFT),
      this.unsafeObj,
      this.cumBaseOffset,
      copyBytes
      );
  }

  //Atomic Write Methods //XXX

  @Override
  public long getAndAddLong(final long offsetBytes, final long delta) {
    assertBounds(offsetBytes, ARRAY_LONG_INDEX_SCALE, capacity);
    return unsafe.getAndAddLong(unsafeObj, cumBaseOffset, delta) + delta;
  }

  @Override
  public boolean compareAndSwapLong(final long offsetBytes, final long expect, final long update) {
    assertBounds(offsetBytes, ARRAY_INT_INDEX_SCALE, capacity);
    return unsafe.compareAndSwapLong(unsafeObj, cumBaseOffset, expect, update);
  }

  @Override
  public long getAndSetLong(final long offsetBytes, final long newValue) {
    assertBounds(offsetBytes, ARRAY_LONG_INDEX_SCALE, capacity);
    return unsafe.getAndSetLong(unsafeObj, cumBaseOffset, newValue);
  }

  //OTHER WRITE METHODS //XXX

  @Override
  Object getArray() {
    return unsafeObj;
  }

  @Override
  public void clear() {
    fill(0, capacity, (byte) 0);
  }

  @Override
  public void clear(final long offsetBytes, final long lengthBytes) {
    fill(offsetBytes, lengthBytes, (byte) 0);
  }

  @Override
  public void clearBits(final long offsetBytes, final byte bitMask) {
    assertBounds(offsetBytes, ARRAY_BYTE_INDEX_SCALE, capacity);
    final long cumBaseOff = this.cumBaseOffset;
    int value = unsafe.getByte(unsafeObj, cumBaseOff) & 0XFF;
    value &= ~bitMask;
    unsafe.putByte(unsafeObj, cumBaseOff, (byte)value);
  }

  @Override
  public void fill(final byte value) {
    fill(0, capacity, value);
  }

  @Override
  public void fill(final long offsetBytes, final long lengthBytes, final byte value) {
    assertBounds(offsetBytes, lengthBytes, capacity);
    unsafe.setMemory(unsafeObj, cumBaseOffset, lengthBytes, value);
  }

  //OTHER //XXX

  @Override
  public MemoryRequest getMemoryRequest() { //only applicable to writable
    return this.state.getMemoryRequest();
  }

}
