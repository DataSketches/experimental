/*
 * Copyright 2015-16, Yahoo! Inc.
 * Licensed under the terms of the Apache License 2.0. See LICENSE file at the project root for terms.
 */

package com.yahoo.memory2;

/**
 * The MemoryRequest is a callback interface that is accessible from the WritableMemory interface and
 * provides a means for a WritableMemory object to request more memory from the calling class and to
 * free memory that is no longer needed.
 *
 * @author Lee Rhodes
 */
public interface MemoryRequest {

  /**
   * Request new WritableMemory with the given capacity.
   * @param capacityBytes The capacity being requested
   * @return new WritableMemory with the given capacity. If this request is refused it will be null.
   */
  WritableMemory request(long capacityBytes);

  /**
   * Request for allocate and copy.
   *
   * <p>Request to allocate new WritableMemory with the capacityBytes; copy the contents of origMem
   * from zero to copyToBytes.</p>
   *
   * @param origMem The original WritableMemory, a portion, starting at zero, which will be copied
   * to the newly allocated WritableMemory. This reference must not be null.
   * This origMem must not modified in any way, and may be reused or freed by the implementation.
   * The requesting application may NOT assume anything about the origMem.
   *
   * @param copyToBytes the upper limit of the region to be copied from origMem to the newly
   * allocated WritableMemory. The upper region of the new WritableMemory may or may not be cleared
   * depending on the implementation.
   *
   * @param capacityBytes the desired new capacity of the newly allocated WritableMemory in bytes.
   * @return The new WritableMemory with the given capacity. If this request is refused it will be
   * null.
   */
  WritableMemory request(WritableMemory origMem, long copyToBytes, long capacityBytes);

  /**
   * The given WritableMemory with its capacity is to be freed. It is assumed that the
   * implementation of this interface knows the type of WritableMemory that was created and how
   * to free it.
   * @param mem The WritableMemory to be freed
   */
  void free(WritableMemory mem);

  /**
   * The given memToFree with its capacity may be freed by the implementation.
   * Providing a reference to newMem enables the implementation to link the memToFree to the
   * newMem, if desired.
   *
   * @param memToFree the WritableMemory to be freed. It is assumed that the implementation of
   * this interface knows the type of WritableMemory that was created and how to free it,
   * if desired.
   *
   * @param newMem
   * Providing a reference to newMem enables the implementation to link the memToFree to the
   * newMem, if desired.
   */
  void free(WritableMemory memToFree, WritableMemory newMem);
}
