/*
 * Copyright 2017, Yahoo! Inc. Licensed under the terms of the
 * Apache License 2.0. See LICENSE file at the project root for terms.
 */

package com.yahoo.memory2;

import static com.yahoo.memory.UnsafeUtil.ARRAY_BOOLEAN_BASE_OFFSET;
import static com.yahoo.memory.UnsafeUtil.ARRAY_BOOLEAN_INDEX_SCALE;
import static com.yahoo.memory.UnsafeUtil.ARRAY_BYTE_BASE_OFFSET;
import static com.yahoo.memory.UnsafeUtil.ARRAY_BYTE_INDEX_SCALE;
import static com.yahoo.memory.UnsafeUtil.ARRAY_CHAR_BASE_OFFSET;
import static com.yahoo.memory.UnsafeUtil.ARRAY_CHAR_INDEX_SCALE;
import static com.yahoo.memory.UnsafeUtil.ARRAY_DOUBLE_INDEX_SCALE;
import static com.yahoo.memory.UnsafeUtil.ARRAY_FLOAT_INDEX_SCALE;
import static com.yahoo.memory.UnsafeUtil.ARRAY_INT_INDEX_SCALE;
import static com.yahoo.memory.UnsafeUtil.ARRAY_LONG_BASE_OFFSET;
import static com.yahoo.memory.UnsafeUtil.ARRAY_LONG_INDEX_SCALE;
import static com.yahoo.memory.UnsafeUtil.ARRAY_SHORT_INDEX_SCALE;
import static com.yahoo.memory.UnsafeUtil.BOOLEAN_SHIFT;
import static com.yahoo.memory.UnsafeUtil.BYTE_SHIFT;
import static com.yahoo.memory.UnsafeUtil.CHAR_SHIFT;
import static com.yahoo.memory.UnsafeUtil.LONG_SHIFT;
import static com.yahoo.memory.UnsafeUtil.LS;
import static com.yahoo.memory.UnsafeUtil.assertBounds;
import static com.yahoo.memory.UnsafeUtil.unsafe;

import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;

//@SuppressWarnings("unused")
class WritableMemoryImpl extends WritableMemory {
  final long nativeBaseOffset; //Direct ByteBuffer includes the slice() offset here.
  final Object unsafeObj; //Array objects are held here.
  final long unsafeObjHeader; //Heap ByteBuffer includes the slice() offset here.
  final ByteBuffer byteBuf; //Holding on to this so that it is not GCed before we are done with it.
  final long regionOffset;
  final long capacity;
  final long cumBaseOffset; //Holds the cum offset to the start of data.
  final MemoryRequest memReq;
  final AtomicBoolean valid;
  final boolean readOnly; //Assert protection against casting Memory to WritableMemory.

  //FORMAL CONSTRUCTORS

  //Base Constructor. Also used by regions, asReadOnly(), which must inherit valid.
  WritableMemoryImpl(
      final long nativeBaseOffset,
      final Object unsafeObj,
      final long unsafeObjHeader,
      final ByteBuffer byteBuf,
      final long regionOffset,
      final long capacity,
      final MemoryRequest memReq,
      final boolean readOnly,
      final AtomicBoolean valid) {
    this.nativeBaseOffset = nativeBaseOffset;
    this.unsafeObj = unsafeObj;
    this.unsafeObjHeader = unsafeObjHeader;
    this.byteBuf = byteBuf;
    this.regionOffset = regionOffset;
    this.capacity = capacity;
    this.cumBaseOffset = regionOffset + ((unsafeObj == null) ? nativeBaseOffset : unsafeObjHeader);
    this.memReq = memReq;
    this.readOnly = readOnly;
    this.valid = valid;
    assert ((unsafeObj == null) ^ (unsafeObjHeader > 0));
    assert valid.get();
  }

  //Sub-Base Constructor. Also used by AllocateDirect, AllocateDirectMap. Calls Base Constructor
  //The ONLY place where valid is constructed and set to true.
  WritableMemoryImpl(
      final long nativeBaseOffset,
      final Object unsafeObj,
      final long unsafeObjHeader,
      final ByteBuffer byteBuf,
      final long regionOffset,
      final long capacity,
      final MemoryRequest memReq,
      final boolean readOnly) {
    this(nativeBaseOffset, unsafeObj, unsafeObjHeader, byteBuf, regionOffset, capacity, memReq,
        readOnly, new AtomicBoolean(true));
  }

  //Constructor for heap array allocations. Calls Sub-Base
  WritableMemoryImpl(final Object unsafeObj, final long unsafeObjHeader, final long capacity,
      final boolean readOnly) {
    this(0L, unsafeObj, unsafeObjHeader, null, 0L, capacity, null, readOnly);
  }

  //Constructor for ByteBuffer direct
  WritableMemoryImpl(final long nativeBaseOffset, final ByteBuffer byteBuf, final long capacity,
      final boolean readOnly) {
    this(nativeBaseOffset, null, 0L, byteBuf, 0L, capacity, null, readOnly);
  }

  //Constructor for ByteBuffer heap
  WritableMemoryImpl(final Object unsafeObj, final long unsafeObjHeader, final ByteBuffer byteBuf,
      final long regionOffset, final long capacity, final boolean readOnly) {
    this(0L, unsafeObj, unsafeObjHeader, byteBuf, regionOffset, capacity, null, readOnly);
  }

  //METHODS THAT CALL CONSTRUCTORS

  static final WritableMemoryImpl directInstance(final long nativeBaseOffset, final long capacity,
      final MemoryRequest memReq, final boolean readOnlyRequest) {
    return new WritableMemoryImpl(nativeBaseOffset, null, 0L, null, 0L, capacity, memReq,
        readOnlyRequest, new AtomicBoolean(true));
  }

  @Override
  public Memory asReadOnly() {
    return new WritableMemoryImpl(this.nativeBaseOffset, this.unsafeObj, this.unsafeObjHeader,
        this.byteBuf, this.regionOffset, this.capacity, this.memReq, true, this.valid);
  }

  //REGIONS

  @Override //for read only
  public Memory region(final long offsetBytes, final long capacityBytes) {
    assert offsetBytes + capacityBytes <= capacity
        : "newOff + newCap: " + (offsetBytes + capacityBytes) + ", origCap: " + this.capacity;

    final long newRegionOffset = this.regionOffset + offsetBytes;

    return new WritableMemoryImpl(this.nativeBaseOffset, this.unsafeObj, this.unsafeObjHeader,
        this.byteBuf, newRegionOffset, capacityBytes, null, true, this.valid);
  }

  @Override //for writable
  public WritableMemory writableRegion(final long offsetBytes, final long capacityBytes) {
    assert offsetBytes + capacityBytes <= capacity
        : "newOff + newCap: " + (offsetBytes + capacityBytes) + ", origCap: " + this.capacity;
    assert this.readOnly == false;
    final long newRegionOffset = this.regionOffset + offsetBytes;

    return new WritableMemoryImpl(this.nativeBaseOffset, this.unsafeObj, this.unsafeObjHeader,
        this.byteBuf, newRegionOffset, capacityBytes, null, false, this.valid);
  }

  //MAP RELATED
  @Override
  public void load() {}

  @Override
  public boolean isLoaded() {
    return false;
  }

  @Override
  public void force() {}

  //PRIMITIVE GETS

  @Override
  public boolean getBoolean(final long offsetBytes) {
    checkValid();
    assertBounds(offsetBytes, ARRAY_BOOLEAN_INDEX_SCALE, this.capacity);
    return unsafe.getBoolean(this.unsafeObj, this.cumBaseOffset + offsetBytes);
  }

  @Override
  public byte getByte(final long offsetBytes) {
    checkValid();
    assertBounds(offsetBytes, ARRAY_BYTE_INDEX_SCALE, capacity);
    return unsafe.getByte(this.unsafeObj, this.cumBaseOffset + offsetBytes);
  }

  @Override
  public char getChar(final long offsetBytes) {
    checkValid();
    assertBounds(offsetBytes, ARRAY_CHAR_INDEX_SCALE, this.capacity);
    return unsafe.getChar(this.unsafeObj, this.cumBaseOffset + offsetBytes);
  }

  @Override
  public short getShort(final long offsetBytes) {
    checkValid();
    assertBounds(offsetBytes, ARRAY_SHORT_INDEX_SCALE, this.capacity);
    return unsafe.getShort(this.unsafeObj, this.cumBaseOffset + offsetBytes);
  }

  @Override
  public int getInt(final long offsetBytes) {
    checkValid();
    assertBounds(offsetBytes, ARRAY_INT_INDEX_SCALE, this.capacity);
    return unsafe.getInt(this.unsafeObj, this.cumBaseOffset + offsetBytes);
  }

  @Override
  public long getLong(final long offsetBytes) {
    checkValid();
    assertBounds(offsetBytes, ARRAY_LONG_INDEX_SCALE, this.capacity);
    return unsafe.getLong(this.unsafeObj, this.cumBaseOffset + offsetBytes);
  }

  @Override
  public float getFloat(final long offsetBytes) {
    checkValid();
    assertBounds(offsetBytes, ARRAY_FLOAT_INDEX_SCALE, this.capacity);
    return unsafe.getFloat(this.unsafeObj, this.cumBaseOffset + offsetBytes);
  }

  @Override
  public double getDouble(final long offsetBytes) {
    checkValid();
    assertBounds(offsetBytes, ARRAY_DOUBLE_INDEX_SCALE, capacity);
    return unsafe.getDouble(unsafeObj, cumBaseOffset + offsetBytes);
  }

  //Primitive Get Arrays

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

  //Primitive Puts

  @Override
  public void putBoolean(final long offsetBytes, final boolean value) {
    checkValid();
    checkReadOnly();
    assertBounds(offsetBytes, ARRAY_BOOLEAN_INDEX_SCALE, this.capacity);
    unsafe.putBoolean(this.unsafeObj, this.cumBaseOffset + offsetBytes, value);
  }

  @Override
  public void putByte(final long offsetBytes, final byte value) {
    checkValid();
    checkReadOnly();
    assertBounds(offsetBytes, ARRAY_BYTE_INDEX_SCALE, this.capacity);
    unsafe.putByte(this.unsafeObj, this.cumBaseOffset + offsetBytes, value);
  }

  @Override
  public void putChar(final long offsetBytes, final char value) {
    checkValid();
    checkReadOnly();
    assertBounds(offsetBytes, ARRAY_CHAR_INDEX_SCALE, this.capacity);
    unsafe.putChar(this.unsafeObj, this.cumBaseOffset + offsetBytes, value);
  }

  @Override
  public void putInt(final long offsetBytes, final int value) {
    checkValid();
    checkReadOnly();
    assertBounds(offsetBytes, ARRAY_INT_INDEX_SCALE, this.capacity);
    unsafe.putInt(this.unsafeObj, this.cumBaseOffset + offsetBytes, value);
  }

  @Override
  public void putLong(final long offsetBytes, final long value) {
    checkValid();
    checkReadOnly();
    assertBounds(offsetBytes, ARRAY_LONG_INDEX_SCALE, this.capacity);
    unsafe.putLong(this.unsafeObj, this.cumBaseOffset + offsetBytes, value);
  }

  @Override
  public void putFloat(final long offsetBytes, final float value) {
    checkValid();
    checkReadOnly();
    assertBounds(offsetBytes, ARRAY_FLOAT_INDEX_SCALE, this.capacity);
    unsafe.putFloat(this.unsafeObj, this.cumBaseOffset + offsetBytes, value);
  }

  @Override
  public void putDouble(final long offsetBytes, final double value) {
    checkValid();
    checkReadOnly();
    assertBounds(offsetBytes, ARRAY_DOUBLE_INDEX_SCALE, this.capacity);
    unsafe.putDouble(this.unsafeObj, this.cumBaseOffset + offsetBytes, value);
  }

  //Primitive Put Arrays

  @Override
  public void putBooleanArray(final long offsetBytes, final boolean[] srcArray, final int srcOffset,
      final int length) {
    checkValid();
    checkReadOnly();
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
  public void putByteArray(final long offsetBytes, final byte[] srcArray, final int srcOffset,
      final int length) {
    checkValid();
    checkReadOnly();
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
  public void putCharArray(final long offsetBytes, final char[] srcArray, final int srcOffset,
      final int length) {
    checkValid();
    checkReadOnly();
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
  public void putLongArray(final long offsetBytes, final long[] srcArray, final int srcOffset,
      final int length) {
    checkValid();
    checkReadOnly();
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

  //Plus a number of convenience write methods not listed
  @Override
  public long getCapacity() {
    return this.capacity;
  }

  @Override
  public MemoryRequest getMemoryRequest() {
    return this.memReq;
  }

  @Override
  public void close() {}

  @Override
  public boolean isValid() {
    return this.valid.get();
  }

  @Override
  public String toHexString(final String header, final long offsetBytes, final int lengthBytes) {
    checkValid();
    final StringBuilder sb = new StringBuilder();
    sb.append(header).append(LS);
    final String s1 = String.format("(..., %d, %d)", offsetBytes, lengthBytes);
    sb.append(this.getClass().getSimpleName()).append(".toHexString")
      .append(s1).append(", hash: ").append(this.hashCode()).append(LS);

    return toHex(sb.toString(), offsetBytes, lengthBytes);
  }


  //RESTRICTED

  private final void checkValid() {
    assert this.valid.get() : "Memory not valid.";
  }

  private final void checkReadOnly() {
    assert !this.readOnly : "Memory is Read-Only. Write methods are not allowed.";
  }

  /**
   * Returns a formatted hex string of an area of this Memory.
   * Used primarily for testing.
   * @param header a descriptive header
   * @param offsetBytes offset bytes relative to the Memory start
   * @param lengthBytes number of bytes to convert to a hex string
   * @return a formatted hex string in a human readable array
   */
  private String toHex(final String header, final long offsetBytes, final int lengthBytes) {
    assertBounds(offsetBytes, lengthBytes, capacity);
    final StringBuilder sb = new StringBuilder();
    final String uObj = (unsafeObj == null) ? "null"
        : unsafeObj.getClass().getSimpleName() + ", " + unsafeObj.hashCode();
    final String bb = (byteBuf == null) ? "null"
        : byteBuf.getClass().getSimpleName() + ", " + byteBuf.hashCode();
    final String mr = (memReq == null) ? "null"
        : memReq.getClass().getSimpleName() + ", " + memReq.hashCode();
    sb.append(header).append(LS);
    sb.append("NativeBaseOffset    : ").append(nativeBaseOffset).append(LS);
    sb.append("UnsafeObj           : ").append(uObj).append(LS);
    sb.append("UnsafeObjHeader     : ").append(unsafeObjHeader).append(LS);
    sb.append("ByteBuf             : ").append(bb).append(LS);
    sb.append("RegionOffset        : ").append(regionOffset).append(LS);
    sb.append("Capacity            : ").append(capacity).append(LS);
    sb.append("CumBaseOffset       : ").append(cumBaseOffset).append(LS);
    sb.append("MemReq              : ").append(mr).append(LS);
    sb.append("Valid               : ").append(valid).append(LS);
    sb.append("Read Only           : ").append(readOnly).append(LS);
    sb.append("Memory, littleEndian:  0  1  2  3  4  5  6  7");
    long j = offsetBytes;
    final StringBuilder sb2 = new StringBuilder();
    for (long i = 0; i < lengthBytes; i++) {
      final int b = unsafe.getByte(unsafeObj, cumBaseOffset + i) & 0XFF;
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
