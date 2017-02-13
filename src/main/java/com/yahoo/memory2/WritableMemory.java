/*
 * Copyright 2017, Yahoo! Inc. Licensed under the terms of the
 * Apache License 2.0. See LICENSE file at the project root for terms.
 */

package com.yahoo.memory2;

import static com.yahoo.memory.UnsafeUtil.ARRAY_LONG_INDEX_SCALE;
import static com.yahoo.memory.UnsafeUtil.assertBounds;
import static com.yahoo.memory.UnsafeUtil.unsafe;

import java.io.File;
import java.nio.ByteBuffer;

@SuppressWarnings("unused")
public class WritableMemory extends BaseMemory {
  private MemoryRequest memReq = null;

  /**
   * @param cumBaseOffset blah
   * @param arrayOffset blah
   * @param capacity blah
   */
  WritableMemory(final long cumBaseOffset, final long arrayOffset, final long capacity,
      final MemoryRequest memReq) {
    super(cumBaseOffset, arrayOffset, capacity);
    this.memReq = memReq;
  }

  //Allocations using native memory and heap

  //ALLOCATE DIRECT
  public static WritableMemory allocateDirect(final long capacityBytes, final MemoryRequest memReq) {
    return MemoryDW.allocDirect(capacityBytes, memReq);
  }

  //ALLOCATE HEAP VIA AUTOMATIC BYTE ARRAY
  public static WritableMemory allocate(final long capacityBytes, final MemoryRequest memReq) {
    //-> MemoryHW.  Allocates a heap byte[] for you.
    return null;
  }

  //ALLOCATE HEAP VIA PRIMITIVE ARRAYS (8 of these)
  //Wraps a given primitive array (8 of these each)

  public static WritableMemory wrap(final byte[] arr) {
    //-> MemoryHW  Wraps the given primitive array
    return null;
  }

  //ByteBuffer

  /**
   * @param bb blah
   * @return blah
   */
  public static WritableMemory wrap(final ByteBuffer bb) { //could end up RO or W
    //if BB is RO Direct -> MemoryBBDR //throws exception at runtime
    //if BB is RO Heap -> MemoryBBHR  //throws exception at runtime
    //if BB is W Direct -> MemoryBBDW
    //if BB is W Heap -> MemoryBBHW
    return null;
  }

  //Map

  public static WritableMemory mapWritable(final File file, final long offsetBytes,
      final long capacityBytes) {
    //-> MapDW
    return null;
  }

  //Primitive R/W methods 8 of each

  public long getLong(final long offsetBytes) {
    assertBounds(offsetBytes, ARRAY_LONG_INDEX_SCALE, capacity());
    return unsafe.getLong(null, cumBaseOffset() + offsetBytes);
  }

  public void putLong(final long offsetBytes, final long value) {
    assertBounds(offsetBytes, ARRAY_LONG_INDEX_SCALE, capacity());
    unsafe.putLong(null, cumBaseOffset() + offsetBytes, value);
  }

  //Plus a number of convenience write methods not listed
  // e.g., clean, fill, MemoryRequest, etc.

  public MemoryRequest getMemoryRequest() {
    return memReq;
  }

  /**
   * Optional freeMemory blah, blah
   */
  public void freeMemory() {
    memReq = null;
  }

}
