/*
 * Copyright 2017, Yahoo! Inc. Licensed under the terms of the
 * Apache License 2.0. See LICENSE file at the project root for terms.
 */

package com.yahoo.memory2;

import static com.yahoo.memory.UnsafeUtil.ARRAY_BYTE_BASE_OFFSET;

final class MemoryHW extends WritableMemoryImpl {

  private MemoryHW(final Object memObj, final long objectOffset, final long arrayOffset,
      final long capacity, final MemoryRequest memReq) {
    super(0L, memObj, objectOffset, arrayOffset, capacity, memReq);
  }

  static WritableMemoryImpl allocateArray(final int capacity, final MemoryRequest memReq) {
    final byte[] memObj = new byte[capacity];
    return new MemoryHW(memObj, ARRAY_BYTE_BASE_OFFSET, 0L, capacity, memReq);
  }

}
