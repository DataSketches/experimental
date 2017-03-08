/*
 * Copyright 2017, Yahoo! Inc. Licensed under the terms of the
 * Apache License 2.0. See LICENSE file at the project root for terms.
 */

package com.yahoo.memory4;

import static com.yahoo.memory.UnsafeUtil.ARRAY_BYTE_BASE_OFFSET;
import static com.yahoo.memory.UnsafeUtil.ARRAY_BYTE_INDEX_SCALE;

import java.nio.ByteBuffer;

/**
 * @author Lee Rhodes
 */
final class AccessWritableByteBuffer extends WritableMemoryImpl implements WritableMemoryHandler {

  private AccessWritableByteBuffer(final MemoryState state) {
    super(state);
  }

  @Override
  public WritableMemory get() {
    return this;
  }

  //The provided ByteBuffer (via state) must be writable
  static WritableMemoryImpl wrap(final MemoryState state) {
    final ByteBuffer byteBuf = state.getByteBuffer();
    state.putCapacity(byteBuf.capacity());

    final boolean direct = byteBuf.isDirect();

    //WRITABLE-DIRECT
    if (direct) {
      //address() is already adjusted for direct slices, so regionOffset = 0
      state.putNativeBaseOffset(((sun.nio.ch.DirectBuffer) byteBuf).address());
      return new AccessWritableByteBuffer(state);
    }

    //WRITABLE-HEAP
    state.putUnsafeObject(byteBuf.array());
    state.putUnsafeObjectHeader(ARRAY_BYTE_BASE_OFFSET);
    state.putRegionOffset(byteBuf.arrayOffset() * ARRAY_BYTE_INDEX_SCALE);
    return new AccessWritableByteBuffer(state);
  }


  @Override
  public void close() {
    super.state.setInvalid();
  }

  @Override
  public void load() {
    // No-op
  }

  @Override
  public boolean isLoaded() {
    // Means nothing
    return false;
  }

  @Override
  public void force() {
    // No-op
  }

}
