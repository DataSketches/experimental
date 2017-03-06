/*
 * Copyright 2017, Yahoo! Inc. Licensed under the terms of the
 * Apache License 2.0. See LICENSE file at the project root for terms.
 */

package com.yahoo.memory2;

import static com.yahoo.memory.UnsafeUtil.ARRAY_BYTE_BASE_OFFSET;
import static com.yahoo.memory.UnsafeUtil.ARRAY_BYTE_INDEX_SCALE;

import java.lang.reflect.Field;
import java.nio.ByteBuffer;

final class AccessByteBuffer {

  static WritableMemoryImpl writableWrap(final ByteBuffer byteBuffer, final boolean readOnlyRequest) {
    final long nativeBaseAddress;  //includes the slice() offset for direct.
    final Object unsafeObj;
    final long unsafeObjHeader;
    final ByteBuffer byteBuf = byteBuffer;
    final long regionOffset; //includes the slice() offset for heap.
    final long capacity = byteBuffer.capacity();

    final boolean readOnlyBB = byteBuffer.isReadOnly();
    final boolean direct = byteBuffer.isDirect();

    if (readOnlyBB) {
      //READ-ONLY DIRECT
      if (direct) {
        //address() is already adjusted for direct slices, so regionOffset = 0
        nativeBaseAddress = ((sun.nio.ch.DirectBuffer) byteBuf).address();
        return new WritableMemoryImpl(
            nativeBaseAddress, byteBuffer, capacity, true);
      }

      //READ-ONLY HEAP
      //The messy acquisition of arrayOffset() and array() created from a RO slice()
      try {
        Field field = ByteBuffer.class.getDeclaredField("offset");
        field.setAccessible(true);
        regionOffset = ((Integer)field.get(byteBuffer)).longValue() * ARRAY_BYTE_INDEX_SCALE;

        field = ByteBuffer.class.getDeclaredField("hb"); //the backing byte[] from HeapByteBuffer
        field.setAccessible(true);
        unsafeObj = field.get(byteBuffer);
      }
      catch (final IllegalAccessException | NoSuchFieldException e) {
        throw new RuntimeException(
            "Could not get offset/byteArray from OnHeap ByteBuffer instance: " + e.getClass());
      }
      unsafeObjHeader = ARRAY_BYTE_BASE_OFFSET;
      return new WritableMemoryImpl(
          unsafeObj, unsafeObjHeader, byteBuffer, regionOffset, capacity, true);
    }

    else { //BB is WRITABLE

      //WRITABLE-DIRECT  //nativeBaseAddress, byteBuf, capacity
      if (direct) {
        //address() is already adjusted for direct slices, so regionOffset = 0
        nativeBaseAddress = ((sun.nio.ch.DirectBuffer) byteBuf).address();
        return new WritableMemoryImpl(
            nativeBaseAddress, byteBuffer, capacity, readOnlyRequest);
      }

      //WRITABLE-HEAP  //unsafeObj, unsafeObjHeader, bytBuf, regionOffset, capacity
      unsafeObj = byteBuf.array();
      unsafeObjHeader = ARRAY_BYTE_BASE_OFFSET;
      regionOffset = byteBuf.arrayOffset() * ARRAY_BYTE_INDEX_SCALE;
      return new WritableMemoryImpl(
          unsafeObj, unsafeObjHeader, byteBuffer, regionOffset, capacity, readOnlyRequest);
    }
  }

}
