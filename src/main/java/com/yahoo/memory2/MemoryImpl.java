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

import java.io.File;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;

@SuppressWarnings("unused")
class MemoryImpl extends Memory {
  long nativeBaseOffset;
  Object memObj;
  long memObjHeader;
  ByteBuffer byteBuf;
  long regionOffset;
  long cumBaseOffset;
  long capacity;

  MemoryImpl(final long nativeBaseOffset, final Object memObj, final long memObjHeader,
      final ByteBuffer byteBuf, final long regionOffset, final long capacity) {
    this.nativeBaseOffset = nativeBaseOffset;
    this.memObj = memObj;
    this.memObjHeader = memObjHeader;
    this.byteBuf = byteBuf;
    this.regionOffset = regionOffset;
    this.cumBaseOffset = (memObj == null)
        ? nativeBaseOffset + regionOffset
        : memObjHeader + regionOffset;
    this.capacity = capacity;
  }

  //ByteBuffer
  /**
   * Provides access to the backing store of the given ByteBuffer.
   * If the given <i>ByteBuffer</i> is read-only, the returned <i>Memory</i> will also be a
   * read-only instance.
   * @param byteBuffer the given <i>ByteBuffer</i>
   * @return a <i>Memory</i> object
   */
  public static Memory wrap(final ByteBuffer byteBuffer) {
    final long nativeBaseAddress;  //includes the slice() offset for direct.
    final Object memObj;
    final long memObjHeader;
    final ByteBuffer byteBuf = byteBuffer;
    final long regionOffset; //includes the slice() offset for heap.
    final long capacity = byteBuffer.capacity();

    final boolean readOnly = byteBuffer.isReadOnly();
    final boolean direct = byteBuffer.isDirect();

    if (readOnly) {

      //READ-ONLY DIRECT
      if (direct) {
        //address() is already adjusted for direct slices, so regionOffset = 0
        nativeBaseAddress = ((sun.nio.ch.DirectBuffer) byteBuf).address();
        memObj = null;
        memObjHeader = 0L;
        regionOffset = 0;

        final MemoryImpl nmr = new MemoryImpl(
            nativeBaseAddress, memObj, memObjHeader, byteBuffer, regionOffset, capacity);
        return nmr;
      }

      //READ-ONLY HEAP
      nativeBaseAddress = 0L;
      memObjHeader = ARRAY_BYTE_BASE_OFFSET;
      //The messy acquisition of arrayOffset() and array() created from a RO slice()
      try {
        Field field = ByteBuffer.class.getDeclaredField("offset");
        field.setAccessible(true);
        regionOffset = ((long) field.get(byteBuffer)) * ARRAY_BYTE_INDEX_SCALE;

        field = ByteBuffer.class.getDeclaredField("hb"); //the backing byte[]
        field.setAccessible(true);
        memObj = field.get(byteBuffer);
      }
      catch (final IllegalAccessException | NoSuchFieldException e) {
        throw new RuntimeException(
            "Could not get offset/byteArray from OnHeap ByteBuffer instance: " + e.getClass());
      }

      final MemoryImpl nmr = new MemoryImpl(
          nativeBaseAddress, memObj, memObjHeader, byteBuffer, regionOffset, capacity);
      return nmr;
    }

    //WRITABLE - converted to read only
    else {

      //WRITABLE-DIRECT
      if (direct) {
        //address() is already adjusted for direct slices, so regionOffset = 0
        nativeBaseAddress = ((sun.nio.ch.DirectBuffer) byteBuf).address();
        memObj = null;
        memObjHeader = 0L;
        regionOffset = 0L;

        final MemoryImpl nmr = new MemoryImpl(
            nativeBaseAddress, memObj, memObjHeader, byteBuffer, regionOffset, capacity);
        return nmr;
      }

      //WRITABLE-HEAP
      nativeBaseAddress = 0L;
      regionOffset = byteBuf.arrayOffset() * ARRAY_BYTE_INDEX_SCALE;
      memObjHeader = ARRAY_BYTE_BASE_OFFSET;
      memObj = byteBuf.array();

      final MemoryImpl nmr = new MemoryImpl(
          nativeBaseAddress, memObj, memObjHeader, byteBuffer, regionOffset, capacity);
      return nmr;
    }
  }


  public static MemoryImpl map(final File file, final long offsetBytes,
      final long capacityBytes) {
    //-> MapDR
    return null;
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

  //plus 6 more
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

  //Plus some convenience read methods not listed
  //isDirect, etc.


}
