/*
 * Copyright 2017, Yahoo! Inc. Licensed under the terms of the
 * Apache License 2.0. See LICENSE file at the project root for terms.
 */

package com.yahoo.memory4;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import org.testng.annotations.Test;

public class AllocMemoryTest {

  @Test
  public void checkAllocateDirect() {
    int longs = 1024;
    int bytes = longs << 3;
    WritableResourceHandler wh = WritableMemory.allocateDirect(bytes);
    WritableMemory wMem1 = wh.get();
    for (int i = 0; i<longs; i++) {
      wMem1.putLong(i << 3, i);
      assertEquals(wMem1.getLong(i << 3), i);
    }
    wh.close();
  }

  private static class DummyMemReq implements MemoryRequest {
    @Override public WritableMemory request(long capacityBytes) {
      return null;
    }
    @Override public WritableMemory request(WritableMemory origMem, long copyToBytes,
        long capacityBytes) {
      return null;
    }
    @Override public void closeRequest(WritableMemory mem) {}
    @Override public void closeRequest(WritableMemory memToFree, WritableMemory newMem) {}
  }

  @Test
  public void checkAllocateWithMemReq() {
    MemoryRequest req = new DummyMemReq();
    WritableResourceHandler wh = WritableMemory.allocateDirect(8, req);
    WritableMemory wMem = wh.get();
    assertTrue(req.equals(wMem.getMemoryRequest()));
    wh.close();
  }

  @Test
  public void printlnTest() {
    println("PRINTING: "+this.getClass().getName());
  }

  /**
   * @param s value to print
   */
  static void println(String s) {
    //System.out.println(s); //disable here
  }


}
