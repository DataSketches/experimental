/*
 * Copyright 2017, Yahoo! Inc. Licensed under the terms of the
 * Apache License 2.0. See LICENSE file at the project root for terms.
 */

package com.yahoo.memory2;

import static com.yahoo.memory.UnsafeUtil.unsafe;

import sun.misc.Cleaner;

final class AllocateDirect extends WritableMemoryImpl implements AutoCloseable {
  private final Cleaner cleaner_;

  private AllocateDirect(final long nativeBaseOffset, final long regionOffset, final long capacity,
      final MemoryRequest memReq) {
    super(nativeBaseOffset, null, 0L, null, regionOffset, capacity, memReq);
    cleaner_ = Cleaner.create(this, new Deallocator(nativeBaseOffset));
  }

  static WritableMemoryImpl allocDirect(final long capacity, final MemoryRequest memReq) {
    final long nativeBaseOffset = unsafe.allocateMemory(capacity);
    return new AllocateDirect(nativeBaseOffset, 0L, capacity, memReq);
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
    private long natBaseAdd;

    private Deallocator(final long nativeBaseAddress) {
      assert (nativeBaseAddress != 0);
      this.natBaseAdd = nativeBaseAddress;
    }

    @Override
    public void run() {
      if (natBaseAdd == 0) { // Paranoia
        return;
      }
      unsafe.freeMemory(natBaseAdd);
    }
  }

}
