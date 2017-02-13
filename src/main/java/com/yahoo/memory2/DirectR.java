/*
 * Copyright 2017, Yahoo! Inc. Licensed under the terms of the
 * Apache License 2.0. See LICENSE file at the project root for terms.
 */

package com.yahoo.memory2;

class DirectR extends Memory {
  long nativeBaseOffset_;

  DirectR(final long nativeBaseOffset, final long arrayOffset, final long capacity) {
    super(nativeBaseOffset + arrayOffset, arrayOffset, capacity);
    nativeBaseOffset_ = nativeBaseOffset;
  }

  @Override
  public void freeMemory() {
    nativeBaseOffset_ = 0L;
  }
}
