/*
 * Copyright 2017, Yahoo! Inc. Licensed under the terms of the
 * Apache License 2.0. See LICENSE file at the project root for terms.
 */

package com.yahoo.memory2;

import static com.yahoo.memory.UnsafeUtil.ARRAY_LONG_INDEX_SCALE;
import static com.yahoo.memory.UnsafeUtil.assertBounds;
import static com.yahoo.memory.UnsafeUtil.unsafe;

@SuppressWarnings("unused")
class WritableMemoryImpl extends WritableMemory {
  private long nativeBaseOffset;
  private final Object memObj;
  private final long objectOffset;
  private final long cumBaseOffset;
  private long arrayOffset;
  private final long capacity;
  private MemoryRequest memReq = null;

  /**
   * @param cumBaseOffset blah
   * @param arrayOffset blah
   * @param capacity blah
   */
  WritableMemoryImpl(final long nativeBaseOffset, final Object memObj, final long objectOffset,
      final long arrayOffset, final long capacity, final MemoryRequest memReq) {
    this.nativeBaseOffset = nativeBaseOffset;
    this.memObj = memObj;
    this.objectOffset = objectOffset;
    this.cumBaseOffset = (memObj == null)
        ? nativeBaseOffset + arrayOffset
        : objectOffset + arrayOffset;
    this.arrayOffset = arrayOffset;
    this.capacity = capacity;
    this.memReq = memReq;
  }


  //Primitive R/W methods 8 of each

  @Override
  public long getLong(final long offsetBytes) {
    assertBounds(offsetBytes, ARRAY_LONG_INDEX_SCALE, capacity);
    return unsafe.getLong(memObj, cumBaseOffset + offsetBytes);
  }

  @Override
  public void putLong(final long offsetBytes, final long value) {
    assertBounds(offsetBytes, ARRAY_LONG_INDEX_SCALE, capacity);
    unsafe.putLong(memObj, cumBaseOffset + offsetBytes, value);
  }

  //Plus a number of convenience write methods not listed
  // e.g., clean, fill, MemoryRequest, etc.

  @Override
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
