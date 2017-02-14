/*
 * Copyright 2017, Yahoo! Inc. Licensed under the terms of the
 * Apache License 2.0. See LICENSE file at the project root for terms.
 */

package com.yahoo.memory2;

import java.io.File;
import java.nio.ByteBuffer;

@SuppressWarnings("unused")
public abstract class Memory {
  //Allocations using native memory and heap

  //ALLOCATE DIRECT
  public static Memory allocateDirect(final long capacity) {
    return ReadOnlyDirect.allocDirect(capacity);
  }

  //ALLOCATE HEAP VIA AUTOMATIC BYTE ARRAY
  //  public static Memory allocate(final int capacity) {
  //    return MemoryHR.allocateArray(capacity, memReq);
  //  }

  //ALLOCATE HEAP VIA PRIMITIVE ARRAYS (8 of these)
  //Wraps a given primitive array (8 of these each)

  public static Memory wrap(final byte[] arr) {
    //-> MemoryHR  Wraps the given primitive array
    return null;
  }

  //ByteBuffer

  /**
   * @param bb blah
   * @return blah
   */
  public static Memory wrap(final ByteBuffer bb) { //could end up RO or W
    //if BB is RO Direct -> MemoryBBDR
    //if BB is RO Heap -> MemoryBBHR
    //if BB is W Direct -> MemoryBBDW //OK, methods won't be there to write
    //if BB is W Heap -> MemoryBBHW //OK, methods won't be there to write
    return null;
  }

  //Map

  public static Memory mapWritable(final File file, final long offset,
      final long capacity) {
    //-> MapDW
    return null;
  }

  //Primitive Read methods 8 of each

  public abstract long getLong(final long offsetBytes);

  //Plus a number of convenience read methods not listed

}
