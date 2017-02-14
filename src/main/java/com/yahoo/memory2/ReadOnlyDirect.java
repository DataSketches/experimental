/*
 * Copyright 2017, Yahoo! Inc. Licensed under the terms of the
 * Apache License 2.0. See LICENSE file at the project root for terms.
 */

package com.yahoo.memory2;

import static com.yahoo.memory.UnsafeUtil.unsafe;

final class ReadOnlyDirect extends MemoryROImpl {


  //PRIVATE CONSTRUCTOR
  private ReadOnlyDirect(final long nativeBaseOffset, final long arrayOffset, final long capacity) {
    super(nativeBaseOffset, null, 0L, arrayOffset, capacity);

  }

  //Factories
  static MemoryROImpl allocDirect(final long capacity) {
    final long nativeBaseOffset = unsafe.allocateMemory(capacity);
    return new ReadOnlyDirect(nativeBaseOffset, 0L, capacity);
  }

  @Override
  public void freeMemory() {

  }

}
