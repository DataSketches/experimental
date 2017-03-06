/*
 * Copyright 2017, Yahoo! Inc. Licensed under the terms of the
 * Apache License 2.0. See LICENSE file at the project root for terms.
 */

package com.yahoo.memory2;

import java.io.File;
import java.nio.ByteBuffer;

public class MemoryHandler implements AutoCloseable {
  private final WritableMemoryImpl wmem;
  private final boolean readOnly;

  MemoryHandler(final WritableMemoryImpl wmem) {
    this.wmem = wmem;
    this.readOnly = wmem.isReadOnly();
  }

  public Memory get() {
    return wmem.asReadOnly(); //sets the RO flag
  }

  public WritableMemory getWritable() {
    return readOnly ? null : wmem;
  }

  //Allocate Direct (native) memory
  /**
   * Allocates direct memory.
   * @param capacityBytes the required capacity of direct memory in bytes
   * @return <i>MemoryHandler</i>, which implements {@link AutoCloseable}.
   */
  public static MemoryHandler allocateDirect(final long capacityBytes) {
    return allocateDirect(capacityBytes, null);
  }

  /**
   * Allocates direct memory.
   * @param capacityBytes the required capacity of direct memory in bytes
   * @param memReq a MemoryRequest callback
   * @return <i>MemoryHandler</i>, which implements {@link AutoCloseable}.
   */
  public static MemoryHandler allocateDirect(
      final long capacityBytes,
      final MemoryRequest memReq) {
    return new MemoryHandler(AllocateDirect.allocDirect(capacityBytes, memReq));
  }

  //Wrap ByteBuffer
  /**
   * Provides writable access to the backing store of the given ByteBuffer.
   * If the given <i>ByteBuffer</i> is read-only and if asserts are enabled, any write operation
   * will throw an assertion error.
   * @param byteBuffer the given <i>ByteBuffer</i>
   * @return <i>MemoryHandler</i>, which implements {@link AutoCloseable}.
   */
  public static MemoryHandler writableWrap(final ByteBuffer byteBuffer) {
    return new MemoryHandler(AccessByteBuffer.writableWrap(byteBuffer, false));
  }

  //Create Memory-mapped file
  /**
   * Provides writable, memory-mapped access to the newly allocated native backing store for the
   * given File. If the given <i>File</i> is read-only, any write operation will throw an
   * exception or an assertion error, if asserts are enabled.
   * @param file the given <i>File</i>
   * @param offsetBytes offset into the file in bytes.
   * @param capacityBytes the capacity of the memory-mapped buffer space in bytes.
   * @return <i>MemoryHandler</i>, which implements {@link AutoCloseable}.
   * @throws Exception file not found or RuntimeException, etc.
   */
  public static MemoryHandler writableMap(final File file, final long offsetBytes,
      final long capacityBytes) throws Exception {
    return new MemoryHandler(AllocateDirectMap.getInstance(file, offsetBytes, capacityBytes));
  }

  @Override
  public void close() {
    wmem.close();
  }
}
