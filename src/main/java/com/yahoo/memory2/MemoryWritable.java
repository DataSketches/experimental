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

import com.yahoo.memory.MemoryRequest;

@SuppressWarnings("unused")
public class MemoryWritable extends BaseMemory {

  /**
   * @param cumBaseOffset blah
   * @param arrayOffset blah
   * @param capacity blah
   */
  MemoryWritable(final long cumBaseOffset, final long arrayOffset, final long capacity) {
    super(cumBaseOffset, arrayOffset, capacity);
  }

  //Allocations using native memory and heap

  public static MemoryWritable allocateDirect(final long capacityBytes, final MemoryRequest memReq) {
    return MemoryDW.allocate(capacityBytes, memReq);
  }

  public static MemoryWritable allocate(final long capacityBytes) {
    //-> MemoryHW.  Allocates a heap byte[] for you.
    return null;
  }

  //Wraps a given primitive array (8 of these each)

  public static MemoryWritable wrap(final byte[] arr) {
    //-> MemoryHW  Wraps the given primitive array
    return null;
  }

  //ByteBuffer

  /**
   * @param bb blah
   * @return blah
   */
  public static MemoryWritable wrap(final ByteBuffer bb) { //could end up RO or W
    //if BB is RO Direct -> MemoryBBDR //throws exception at runtime
    //if BB is RO Heap -> MemoryBBHR  //throws exception at runtime
    //if BB is W Direct -> MemoryBBDW
    //if BB is W Heap -> MemoryBBHW
    return null;
  }

  //Map

  public static MemoryWritable mapWritable(final File file, final long offsetBytes,
      final long capacityBytes) {
    //-> MapDW
    return null;
  }

  //Primitive R/W methods 8 of each

  public long getLong(final long offsetBytes) {
    assertBounds(offsetBytes, ARRAY_LONG_INDEX_SCALE, capacity_);
    return unsafe.getLong(null, cumBaseOffset_ + offsetBytes);
  }

  public void putLong(final long offsetBytes, final long value) {
    assertBounds(offsetBytes, ARRAY_LONG_INDEX_SCALE, capacity_);
    unsafe.putLong(null, cumBaseOffset_ + offsetBytes, value);
  }

  //Plus a number of convenience write methods not listed
  // e.g., clean, fill, etc.

  @Override
  public void freeMemory() {
    super.freeMemory();
  }

}
