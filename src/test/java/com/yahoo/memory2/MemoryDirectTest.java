/*
 * Copyright 2017, Yahoo! Inc. Licensed under the terms of the
 * Apache License 2.0. See LICENSE file at the project root for terms.
 */

package com.yahoo.memory2;

import org.testng.Assert;
import org.testng.annotations.Test;

public class MemoryDirectTest {

  @Test
  public void checkDirectRoundTrip() {
    int n = 1024; //longs
    WritableMemory mem = WritableMemory.allocateDirect(n * 8, null);
    for (int i = 0; i < n; i++) mem.putLong(i * 8, i);
    for (int i = 0; i < n; i++) {
      long v = mem.getLong(i * 8);
      Assert.assertEquals(v, i);
    }

    mem.freeMemory();
  }

}
