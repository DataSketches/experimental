/*
 * Copyright 2017, Yahoo! Inc. Licensed under the terms of the
 * Apache License 2.0. See LICENSE file at the project root for terms.
 */

package com.yahoo.memory2;

import static com.yahoo.memory.UnsafeUtil.unsafe;

import sun.misc.Cleaner;

final class AllocateDirect extends WritableMemory2 implements AutoCloseable {
  private final Cleaner cleaner_;

  private AllocateDirect(final long nativeBaseOffset, final long capacity, final MemoryRequest memReq) {
    super(nativeBaseOffset, capacity, memReq);
    cleaner_ = Cleaner.create(this, new Deallocator(nativeBaseOffset));
  }

  static WritableMemory2 allocDirect(final long capacity, final MemoryRequest memReq) {
    return new AllocateDirect(unsafe.allocateMemory(capacity), capacity, memReq);
  }

  @Override
  public void freeMemory() {
    close();
  }

  @Override
  public void close() {
    try {
      cleaner_.clean();
    } catch (final Exception e) {
      throw e;
    }
    super.freeMemory();
  }

  private static final class Deallocator implements Runnable {
    private long myNativeBaseOffset;

    private Deallocator(final long nativeBaseOffset) {
      assert (nativeBaseOffset != 0);
      this.myNativeBaseOffset = nativeBaseOffset;
    }

    @Override
    public void run() {
      if (myNativeBaseOffset == 0) { // Paranoia
        return;
      }
      unsafe.freeMemory(myNativeBaseOffset);
    }
  }

}
