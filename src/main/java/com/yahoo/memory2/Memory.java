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

  public static Memory allocateDirect(final long capacityBytes, final MemoryRequest memReq) {
    return MemoryDR.allocate(capacityBytes, memReq);
  }

  public static Memory allocate(final long capacityBytes) {
    //-> MemoryHR.  Allocates a heap byte[] for you.
    return null;
  }

  //Wraps a given primitive array (8 of these)

  public static Memory wrap(final byte[] arr) {
    //-> MemoryHR.  Wraps the given primitive array
    return null;
  }



  //ByteBuffer

  /**
   * @param bb blah
   * @return blah
   */
  public static Memory wrap(final ByteBuffer bb) {
    //if BB is W or RO Direct -> MemoryBBDR
    //if BB is W or RO Heap -> MemoryBBHR
    return null;
  }

  public static Memory map(final File file, final long offsetBytes,
      final long capacityBytes) {
    //-> MapDR
    return null;
  }

  //Primitive Read methods 8 of them

  public long getLong(final long offsetBytes) {
    assertBounds(offsetBytes, ARRAY_LONG_INDEX_SCALE, capacity_);
    return unsafe.getLong(null, cumBaseOffset_ + offsetBytes);
  }

  //Plus some convenience read methods not listed
  //isDirect, etc.

  @Override
  public void freeMemory() {
    super.freeMemory();
  }

}
