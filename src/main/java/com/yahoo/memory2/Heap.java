/*
 * Copyright 2017, Yahoo! Inc. Licensed under the terms of the
 * Apache License 2.0. See LICENSE file at the project root for terms.
 */

package com.yahoo.memory2;

class Heap extends BaseMemory {
  final Object memObj_;
  final long objectOffset_;

  Heap(final Object memObj, final long objectOffset, final long arrayOffset, final long capacity) {
    super(objectOffset + arrayOffset, arrayOffset, capacity);
    memObj_ = memObj;
    objectOffset_ = objectOffset;
  }
}
