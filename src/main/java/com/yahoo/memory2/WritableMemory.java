/*
 * Copyright 2017, Yahoo! Inc. Licensed under the terms of the
 * Apache License 2.0. See LICENSE file at the project root for terms.
 */

package com.yahoo.memory2;

import java.io.File;
import java.nio.ByteBuffer;

@SuppressWarnings("unused")
public abstract class WritableMemory {

  //Allocations using native memory and heap

  //ALLOCATE DIRECT
  public static WritableMemory allocateDirect(final long capacity, final MemoryRequest memReq) {
    return MemoryDW.allocDirect(capacity, memReq);
  }

  //ALLOCATE HEAP VIA AUTOMATIC BYTE ARRAY
  public static WritableMemory allocate(final int capacity, final MemoryRequest memReq) {
    return MemoryHW.allocateArray(capacity, memReq);
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

  public static WritableMemory mapWritable(final File file, final long offset,
      final long capacity) {
    //-> MapDW
    return null;
  }

  //Primitive R/W methods 8 of each

  public abstract long getLong(final long offsetBytes);

  public abstract void putLong(final long offsetBytes, final long value);

  //Plus a number of convenience write methods not listed
  // e.g., clean, fill, MemoryRequest, etc.

  public abstract MemoryRequest getMemoryRequest();

  public void freeMemory() {

  }

}
