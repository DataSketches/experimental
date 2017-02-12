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

//has "absolute" Read-Only methods and launches the rest using factory methods
@SuppressWarnings("unused")
public class Memory extends BaseMemory {

  Memory(final long cumBaseOffset, final long arrayOffset, final long capacity) {
    super(cumBaseOffset, arrayOffset, capacity);
  }

  //Allocations using native memory and heap

  public static Memory allocateDirectReadOnly(final long capacityBytes, final MemoryRequest memReq) {
    return MemoryDR.allocate(capacityBytes, memReq);
  }

  public static Memory allocateDirectWritable(final long capacityBytes, final MemoryRequest memReq) {
    return MemoryDW.allocate(capacityBytes, memReq);
  }

  public static Memory allocateHeapReadOnly(final long capacityBytes) {
    //-> MemoryHR.  Allocates a heap byte[] for you.
    return null;
  }

  public static Memory allocateHeapWritable(final long capacityBytes) {
    //-> MemoryHW.  Allocates a heap byte[] for you.
    return null;
  }

  //Wraps a given primitive array (8 of these each)

  public static Memory wrapReadOnly(final byte[] arr) {
    //-> MemoryHR.  Wraps the given primitive array
    return null;
  }

  public static Memory wrapWritable(final byte[] arr) {
    //-> MemoryHW  Wraps the given primitive array
    return null;
  }

  //ByteBuffer

  /**
   * @param bb blah
   * @return blah
   */
  public static Memory wrapWritable(final ByteBuffer bb) { //could end up RO or W
    //if BB is RO Direct -> MemoryBBDR //throws exception at runtime
    //if BB is RO Heap -> MemoryBBHR  //throws exception at runtime
    //if BB is W Direct -> MemoryBBDW
    //if BB is W Heap -> MemoryBBHW
    return null;
  }

  /**
   * @param bb blah
   * @return blah
   */
  public static Memory wrapReadOnly(final ByteBuffer bb) { //only results in RO
    //if BB is W or RO Direct -> MemoryBBDR
    //if BB is W or RO Heap -> MemoryBBHR
    return null;
  }

  public static Memory mapReadOnly(final File file, final long offsetBytes,
      final long capacityBytes) {
    //-> MapDR
    return null;
  }

  public static Memory mapWritable(final File file, final long offsetBytes, final long capacityBytes) {
    //-> MapDW
    return null;
  }

  //all the read methods
  public long getLong(final long offsetBytes) {
    assertBounds(offsetBytes, ARRAY_LONG_INDEX_SCALE, capacity_);
    return unsafe.getLong(null, cumBaseOffset_ + offsetBytes);
  }

  //all the write methods throw exceptions here
  public void putLong(final long offsetBytes, final long value) {
    throw new UnsupportedOperationException("Write unsupported");
  }

  @Override
  public void freeMemory() {
    super.freeMemory();
  }

}
