/*
 * Copyright 2017, Yahoo! Inc. Licensed under the terms of the
 * Apache License 2.0. See LICENSE file at the project root for terms.
 */

package com.yahoo.memory2;

import static com.yahoo.memory.UnsafeUtil.ARRAY_LONG_INDEX_SCALE;
import static com.yahoo.memory.UnsafeUtil.assertBounds;
import static com.yahoo.memory.UnsafeUtil.unsafe;

class MemoryW extends Memory {

  /**
   * @param cumBaseOffset blah
   * @param arrayOffset blah
   * @param capacity blah
   */
  MemoryW(final long cumBaseOffset, final long arrayOffset, final long capacity) {
    super(cumBaseOffset, arrayOffset, capacity);
  }

  //all the actual put methods
  @Override
  public void putLong(final long offsetBytes, final long value) {
    assertBounds(offsetBytes, ARRAY_LONG_INDEX_SCALE, capacity_);
    unsafe.putLong(null, cumBaseOffset_ + offsetBytes, value);
  }

  @Override
  public void freeMemory() {
    super.freeMemory();
  }

}
