/*
 * Copyright 2017, Yahoo! Inc. Licensed under the terms of the
 * Apache License 2.0. See LICENSE file at the project root for terms.
 */

package com.yahoo.memory2;

class BaseMemory {
  private final long cumBaseOffset;
  private long arrayOffset;
  private final long capacity;

  BaseMemory(final long cumBaseOffset, final long arrayOffset, final long capacity) {
    this.cumBaseOffset = cumBaseOffset;
    this.arrayOffset = arrayOffset;
    this.capacity = capacity;
  }

  final long cumBaseOffset() {
    return cumBaseOffset;
  }

  //used for ByteBuffer slices and Memory Regions
  final long arrayOffset() {
    return arrayOffset;
  }

  final long capacity() {
    return capacity;
  }
}
