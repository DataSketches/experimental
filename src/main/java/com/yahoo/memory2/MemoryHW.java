/*
 * Copyright 2017, Yahoo! Inc. Licensed under the terms of the
 * Apache License 2.0. See LICENSE file at the project root for terms.
 */

package com.yahoo.memory2;

final class MemoryHW extends HeapW {

  private MemoryHW(final Object memObj, final long objectOffset, final long arrayOffset,
      final long capacity, final MemoryRequest memReq) {
    super(memObj, objectOffset, arrayOffset, capacity, memReq);
  }

}
