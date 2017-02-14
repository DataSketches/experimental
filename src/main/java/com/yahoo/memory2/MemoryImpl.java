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

//has "absolute" Read-Only methods and launches the rest using factory methods
@SuppressWarnings("unused")
class MemoryImpl extends Memory {
  private long nativeBaseOffset;
  private final Object memObj;
  private final long objectOffset;
  private final long cumBaseOffset;
  private long arrayOffset;
  private final long capacity;

  MemoryImpl(final long nativeBaseOffset, final Object memObj, final long objectOffset,
      final long arrayOffset, final long capacity) {
    this.nativeBaseOffset = nativeBaseOffset;
    this.memObj = null;
    this.objectOffset = 0;
    this.cumBaseOffset = (memObj == null)
        ? nativeBaseOffset + arrayOffset
        : objectOffset + arrayOffset;
    this.arrayOffset = arrayOffset;
    this.capacity = capacity;
  }




  //Allocations using native memory and heap

  public static MemoryImpl allocateDirect(final long capacityBytes) {
    return ReadOnlyDirect.allocDirect(capacityBytes);
  }

  public static MemoryImpl allocate(final long capacityBytes) {
    //-> MemoryHR.  Allocates a heap byte[] for you.
    return null;
  }

  //Wraps a given primitive array (8 of these)

  public static MemoryImpl wrap(final byte[] arr) {
    //-> MemoryHR.  Wraps the given primitive array
    return null;
  }

  //ByteBuffer

  /**
   * @param bb blah
   * @return blah
   */
  public static MemoryImpl wrap(final ByteBuffer bb) {
    //if BB is W or RO Direct -> MemoryBBDR
    //if BB is W or RO Heap -> MemoryBBHR
    return null;
  }

  public static MemoryImpl map(final File file, final long offsetBytes,
      final long capacityBytes) {
    //-> MapDR
    return null;
  }

  //Primitive Read methods Many of them

  @Override
  public long getLong(final long offsetBytes) {
    assertBounds(offsetBytes, ARRAY_LONG_INDEX_SCALE, capacity);
    return unsafe.getLong(null, cumBaseOffset + offsetBytes);
  }

  //Plus some convenience read methods not listed
  //isDirect, etc.

  /**
   * Optional freeMemory blah, blah
   */
  public void freeMemory() {
    //nothing to free here but must be public and visible.
  }

}
