/*
 * Copyright 2017, Yahoo! Inc. Licensed under the terms of the
 * Apache License 2.0. See LICENSE file at the project root for terms.
 */

package com.yahoo.memory2;

import com.yahoo.memory.MemoryRequest;

class DirectR extends Memory {
  final long nativeBaseOffset_;
  MemoryRequest memReq_;

  DirectR(final long nativeBaseOffset, final long arrayOffset, final long capacity,
      final MemoryRequest memReq) {
    super(nativeBaseOffset + arrayOffset, arrayOffset, capacity);
    nativeBaseOffset_ = nativeBaseOffset;
    memReq_ = memReq;
  }

  void freeMemory() {
    memReq_ = null;
  }
}
