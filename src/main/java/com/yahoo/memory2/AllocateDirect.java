/*
 * Copyright 2017, Yahoo! Inc. Licensed under the terms of the
 * Apache License 2.0. See LICENSE file at the project root for terms.
 */

package com.yahoo.memory2;

import static com.yahoo.memory2.UnsafeUtil.unsafe;

import java.util.concurrent.atomic.AtomicBoolean;

import sun.misc.Cleaner;

final class AllocateDirect extends WritableMemoryImpl {
  private final Cleaner cleaner;

  /**
   * Base Constructor for allocate native memory with MemoryRequest.
   *
   * <p>Allocates and provides access to capacityBytes directly in native (off-heap) memory
   * leveraging the Memory interface.
   * The allocated memory will be 8-byte aligned, but may not be page aligned.
   * @param capacityBytes the size in bytes of the native memory
   * @param memReq The MemoryRequest callback
   */
  private AllocateDirect(final long nativeBaseOffset, final long capacity,
      final MemoryRequest memReq) {
    super(nativeBaseOffset, null, 0L, null, 0L, capacity, memReq, false); //always writable
    this.cleaner = Cleaner.create(this, new Deallocator(nativeBaseOffset, super.valid));
  }

  static WritableMemoryImpl allocDirect(final long capacity, final MemoryRequest memReq) {
    return new AllocateDirect(unsafe.allocateMemory(capacity), capacity, memReq);
  }

  @Override
  public void close() {
    try {
      this.cleaner.clean();
    } catch (final Exception e) {
      throw e;
    }
  }

  private static final class Deallocator implements Runnable {
    //This is the only place the actual native offset is kept for use by unsafe.freeMemory();
    //It can never be modified until it is deallocated.
    private long actualNativeBaseOffset; //
    private final AtomicBoolean parentValidRef;

    private Deallocator(final long nativeBaseOffset, final AtomicBoolean valid) {
      assert (nativeBaseOffset != 0);
      this.actualNativeBaseOffset = nativeBaseOffset;
      this.parentValidRef = valid;
    }

    @Override
    public void run() {
      if (this.actualNativeBaseOffset == 0) {
        // Paranoia
        return;
      }
      unsafe.freeMemory(this.actualNativeBaseOffset);
      this.actualNativeBaseOffset = 0L;
      this.parentValidRef.set(false); //The only place valid is set false.
    }
  }

}
