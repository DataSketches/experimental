/*
 * Copyright 2017, Yahoo! Inc. Licensed under the terms of the
 * Apache License 2.0. See LICENSE file at the project root for terms.
 */

package com.yahoo.memory2;

class BaseMemory {
  final long cumBaseOffset_;
  final long arrayOffset_;
  final long capacity_;

  BaseMemory(final long cumBaseOffset, final long arrayOffset, final long capacity) {
    cumBaseOffset_ = cumBaseOffset;
    arrayOffset_ = arrayOffset;
    capacity_ = capacity;
  }

  void freeMemory() {}
}
