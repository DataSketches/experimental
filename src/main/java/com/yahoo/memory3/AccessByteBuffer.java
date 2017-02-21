/*
 * Copyright 2017, Yahoo! Inc. Licensed under the terms of the
 * Apache License 2.0. See LICENSE file at the project root for terms.
 */

package com.yahoo.memory3;

import static com.yahoo.memory.UnsafeUtil.ARRAY_BYTE_BASE_OFFSET;
import static com.yahoo.memory.UnsafeUtil.ARRAY_BYTE_INDEX_SCALE;

import java.lang.reflect.Field;
import java.nio.ByteBuffer;

final class AccessByteBuffer {

  static Memory wrap(final ByteBuffer byteBuffer) {
    final long nativeBaseAddress;  //includes the slice() offset for direct.
    final Object unsafeObj;
    final long unsafeObjHeader;
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
        return new MemoryImpl(nativeBaseAddress, byteBuffer, capacity);
      }

      //READ-ONLY HEAP
      //The messy acquisition of arrayOffset() and array() created from a RO slice()
      try {
        Field field = ByteBuffer.class.getDeclaredField("offset");
        field.setAccessible(true);
        regionOffset = ((long) field.get(byteBuffer)) * ARRAY_BYTE_INDEX_SCALE;

        field = ByteBuffer.class.getDeclaredField("hb"); //the backing byte[] from HeapByteBuffer
        field.setAccessible(true);
        unsafeObj = field.get(byteBuffer);
      }
      catch (final IllegalAccessException | NoSuchFieldException e) {
        throw new RuntimeException(
            "Could not get offset/byteArray from OnHeap ByteBuffer instance: " + e.getClass());
      }
      unsafeObjHeader = ARRAY_BYTE_BASE_OFFSET;
      return new MemoryImpl(unsafeObj, unsafeObjHeader, byteBuffer, regionOffset, capacity);
    }

    //WRITABLE - converted to read only
    else {

      //WRITABLE-DIRECT  //nativeBaseAddress, byteBuf, capacity
      if (direct) {
        //address() is already adjusted for direct slices, so regionOffset = 0
        nativeBaseAddress = ((sun.nio.ch.DirectBuffer) byteBuf).address();
        return new MemoryImpl(nativeBaseAddress, byteBuffer, capacity);
      }

      //WRITABLE-HEAP  //unsafeObj, unsafeObjHeader, bytBuf, regionOffset, capacity
      unsafeObj = byteBuf.array();
      unsafeObjHeader = ARRAY_BYTE_BASE_OFFSET;
      regionOffset = byteBuf.arrayOffset() * ARRAY_BYTE_INDEX_SCALE;
      return new MemoryImpl(unsafeObj, unsafeObjHeader, byteBuffer, regionOffset, capacity);
    }
  }

}
