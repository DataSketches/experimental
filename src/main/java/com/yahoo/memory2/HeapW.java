/*
 * Copyright 2017, Yahoo! Inc. Licensed under the terms of the
 * Apache License 2.0. See LICENSE file at the project root for terms.
 */

package com.yahoo.memory2;

class HeapW extends WritableMemory {
  final Object memObj_;
  final long objectOffset_;

  HeapW(final Object memObj, final long objectOffset, final long arrayOffset, final long capacity,
      final MemoryRequest memReq) {
    super(objectOffset + arrayOffset, arrayOffset, capacity, memReq);
    memObj_ = memObj;
    objectOffset_ = objectOffset;
  }
}
