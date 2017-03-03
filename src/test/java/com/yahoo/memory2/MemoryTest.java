/*
 * Copyright 2017, Yahoo! Inc. Licensed under the terms of the
 * Apache License 2.0. See LICENSE file at the project root for terms.
 */

package com.yahoo.memory2;

import static org.testng.Assert.assertEquals;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.testng.annotations.Test;

public class MemoryTest {

  @Test
  public void checkDirectRoundTrip() {
    int nLongs = 1024;
    WritableMemory mem =
    WritableMemory.allocateDirect(nLongs * 8, null);
    for (int i = 0; i < nLongs; i++) mem.putLong(i * 8, i);
    for (int i = 0; i < nLongs; i++) {
      long v = mem.getLong(i * 8);
      assertEquals(v, i);
    }
    mem.close();
  }

  @Test
  public void checkAutoHeapRoundTrip() {
    int nLongs = 1024;
    WritableMemory mem = WritableMemory.allocate(nLongs * 8);
    for (int i = 0; i < nLongs; i++) mem.putLong(i * 8, i);
    for (int i = 0; i < nLongs; i++) {
      long v = mem.getLong(i * 8);
      assertEquals(v, i);
    }
    mem.close();
  }

  @Test
  public void checkArrayWrap() {
    int nLongs = 1024;
    byte[] arr = new byte[nLongs * 8];
    WritableMemory wmem = WritableMemory.writableWrap(arr);
    for (int i = 0; i < nLongs; i++) wmem.putLong(i * 8, i);
    for (int i = 0; i < nLongs; i++) {
      long v = wmem.getLong(i * 8);
      assertEquals(v, i);
    }
    Memory mem = WritableMemory.writableWrap(arr).asReadOnly();
    for (int i = 0; i < nLongs; i++) {
      long v = mem.getLong(i * 8);
      assertEquals(v, i);
    }
    wmem.close();
  }

  //BYTE BUFFER

  @Test
  public void checkWBBHeap() {
    int nLongs = 1024;
    ByteBuffer wbb = MemoryTest.genWBB(nLongs, false, false, false); //RO, direct, load
    WritableMemory wmem = WritableMemory.writableWrap(wbb);
    //check writes / reads
    for (int i = 0; i < nLongs; i++) {
      wmem.putLong(i * 8, i + 1); //different values
      long v = wmem.getLong(i * 8);
      assertEquals(v, i + 1);
    }
    println(wmem.toHexString("wmem", 0, 32));

    Memory mem = wmem.asReadOnly();
    println(mem.toHexString("mem", 0, 32));
    //check reads, wmem should have data
    for (int i = 0; i < nLongs; i++) { //write to wmem
      long v = mem.getLong(i * 8);
      assertEquals(v, i + 1);
    }
    wmem.close();
  }

  @Test
  public void checkROBBHeap() {
    int nLongs = 1024;
    ByteBuffer robb = MemoryTest.genWBB(nLongs, true, false, true); //RO, direct, load
    WritableMemory mem = WritableMemory.writableWrap(robb); //does not throw error
    //check reads
    for (int i = 0; i < nLongs; i++) {
      long v = mem.getLong(i * 8);
      assertEquals(v, i);
    }
    //check writes via readOnly flag
    try {
      mem.putLong(0, 0);
      mem.close();
      failure();
    } catch (AssertionError e) {
      mem.close();
    }
    mem.close();
  }

  @Test
  public void checkWBBDirect() {
    int nLongs = 1024;
    ByteBuffer wbb = MemoryTest.genWBB(nLongs, false, true, false); //RO, direct, load
    WritableMemory wmem = WritableMemory.writableWrap(wbb);

    //check writes / reads
    for (int i = 0; i < nLongs; i++) {
      wmem.putLong(i * 8, i + 1);
      long v = wmem.getLong(i * 8);
      assertEquals(v, i + 1);
    }

    Memory mem2 = wmem.asReadOnly();
    //check reads, wmem should have data
    for (int i = 0; i < nLongs; i++) { //write to wmem
      long v = mem2.getLong(i * 8);
      assertEquals(v, i + 1);
    }
    wmem.close();
  }

  @Test
  public void checkROBBDirect() {
    int nLongs = 1024;
    ByteBuffer robb = MemoryTest.genWBB(nLongs, true, true, true); //RO, direct, load
    WritableMemory mem = WritableMemory.writableWrap(robb); //does not throw error
    //check reads
    for (int i = 0; i < nLongs; i++) {
      long v = mem.getLong(i * 8);
      assertEquals(v, i);
    }
    //check writes via readOnly flag
    try {
      mem.putLong(0, 0);
      mem.close();
      failure();
    } catch (AssertionError e) {
      mem.close();
    }
    mem.close();
  }


  @Test
  public void checkPutGetArraysHeap() {
    int n = 1024; //longs
    long[] arr = new long[n];
    for (int i = 0; i < n; i++) { arr[i] = i; }
    WritableMemory wmem = WritableMemory.allocate(n * 8);
    wmem.putLongArray(0, arr, 0, n);
    long[] arr2 = new long[n];
    wmem.getLongArray(0, arr2, 0, n);
    for (int i = 0; i < n; i++) {
      assertEquals(arr2[i], i);
    }
    wmem.close();
  }

  @Test
  public void checkRORegions() {
    int n = 16;
    int n2 = n / 2;
    long[] arr = new long[n];
    for (int i = 0; i < n; i++) { arr[i] = i; }
    Memory mem = WritableMemory.writableWrap(arr).asReadOnly();
    Memory reg = mem.region(n2 * 8, n2 * 8);
    for (int i = 0; i < n2; i++) { println("" + reg.getLong(i * 8)); }
  }

  @Test
  public void checkWRegions() {
    int n = 16;
    int n2 = n / 2;
    long[] arr = new long[n];
    for (int i = 0; i < n; i++) { arr[i] = i; }
    WritableMemory wmem = WritableMemory.writableWrap(arr);
    for (int i = 0; i < n; i++) { println("" + wmem.getLong(i * 8)); }
    println("");
    WritableMemory reg = wmem.writableRegion(n2 * 8, n2 * 8);
    for (int i = 0; i < n2; i++) { reg.putLong(i * 8, i); }
    for (int i = 0; i < n; i++) { println("" + wmem.getLong(i * 8)); }
    wmem.close();
    reg.close();
  }

  @Test(expectedExceptions = AssertionError.class)
  public void checkParentUseAfterFree() {
    int bytes = 64 * 8;
    WritableMemory wmem = WritableMemory.allocateDirect(bytes, null);
    wmem.close();
    //with -ea assert: Memory not valid.
    //with -da sometimes segfaults, sometimes passes!
    wmem.getLong(0);
  }

  @Test(expectedExceptions = AssertionError.class)
  public void checkRORegionUseAfterFree() {
    int bytes = 64;
    WritableMemory wmem = WritableMemory.allocateDirect(bytes);
    Memory reg = wmem.region(0L, bytes);
    wmem.close();
    //with -ea assert: Memory not valid.
    //with -da sometimes segfaults, sometimes passes!
    reg.getByte(0);
  }

  private static ByteBuffer genWBB(int nLongs, boolean readOnly, boolean direct, boolean load) {
    ByteBuffer wbb = (direct)
        ? ByteBuffer.allocateDirect(nLongs * 8)
        : ByteBuffer.allocate(nLongs * 8);
    setEndian(wbb, true);
    if (load) {
      for (int i = 0; i < nLongs; i++) {
        wbb.putLong(i * 8, i);
      }
    }
    ByteBuffer out = wbb;
    setEndian(out, true);
    if (readOnly) {
      out = wbb.asReadOnlyBuffer();
      setEndian(out, true);
    }
    return out;
  }

  private static void setEndian(ByteBuffer bb, boolean nativeEndian) {
    if (nativeEndian) {
      bb.order(ByteOrder.nativeOrder());
    } else {
      bb.order(ByteOrder.BIG_ENDIAN);
    }
  }

  private static void failure() {
    throw new IllegalArgumentException("Failed");
  }

  /**
   * @param s blah
   */
  static void println(String s) {
    //System.out.println(s);
  }

}
