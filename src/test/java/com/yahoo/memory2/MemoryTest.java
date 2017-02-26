/*
 * Copyright 2017, Yahoo! Inc. Licensed under the terms of the
 * Apache License 2.0. See LICENSE file at the project root for terms.
 */

package com.yahoo.memory2;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.fail;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.testng.annotations.Test;

public class MemoryTest {

  @Test
  public void checkDirectRoundTrip() {
    int n = 1024; //longs
    WritableMemory mem =
    WritableMemory.allocateDirect(n * 8, null);
    for (int i = 0; i < n; i++) mem.putLong(i * 8, i);
    for (int i = 0; i < n; i++) {
      long v = mem.getLong(i * 8);
      assertEquals(v, i);
    }
    mem.close();
  }

  @Test
  public void checkAutoHeapRoundTrip() {
    int n = 1024; //longs
    WritableMemory mem = WritableMemory.allocate(n * 8);
    for (int i = 0; i < n; i++) mem.putLong(i * 8, i);
    for (int i = 0; i < n; i++) {
      long v = mem.getLong(i * 8);
      assertEquals(v, i);
    }
    mem.close();
  }

  @Test
  public void checkArrayWrap() {
    int n = 1024; //longs
    byte[] arr = new byte[n * 8];
    WritableMemory wmem = WritableMemory.writableWrap(arr);
    for (int i = 0; i < n; i++) wmem.putLong(i * 8, i);
    for (int i = 0; i < n; i++) {
      long v = wmem.getLong(i * 8);
      assertEquals(v, i);
    }
    Memory mem = Memory.wrap(arr);
    for (int i = 0; i < n; i++) {
      long v = mem.getLong(i * 8);
      assertEquals(v, i);
    }
    wmem.close();
  }

  @Test
  public void checkByteBufHeap() {
    int n = 1024; //longs
    byte[] arr = new byte[n * 8];
    ByteBuffer bb = ByteBuffer.wrap(arr);
    bb.order(ByteOrder.nativeOrder());
    WritableMemory wmem = WritableMemory.writableWrap(bb);
    for (int i = 0; i < n; i++) { //write to wmem
      wmem.putLong(i * 8, i);
    }
    for (int i = 0; i < n; i++) { //read from wmem
      long v = wmem.getLong(i * 8);
      assertEquals(v, i);
    }
    for (int i = 0; i < n; i++) { //read from BB
      long v = bb.getLong(i * 8);
      assertEquals(v, i);
    }
    Memory mem1 = Memory.wrap(arr);
    for (int i = 0; i < n; i++) { //read from wrapped arr
      long v = mem1.getLong(i * 8);
      assertEquals(v, i);
    }
    //convert to RO
    Memory mem = wmem.asReadOnly();
    for (int i = 0; i < n; i++) {
      long v = mem.getLong(i * 8);
      assertEquals(v, i);
    }
    wmem.close();
  }

  @Test
  public void checkByteBufHeap2() {
    int n = 1024; //longs
    byte[] arr = new byte[n * 8];
    ByteBuffer wbb = ByteBuffer.wrap(arr);
    wbb.order(ByteOrder.nativeOrder());
    WritableMemory wmem = WritableMemory.writableWrap(wbb);
    for (int i = 0; i < n; i++) { //write to wmem
      wmem.putLong(i * 8, i);
    }
    //convert to RO
    Memory mem = wmem.asReadOnly();

    try ( WritableMemory wmem2 = (WritableMemory) mem; )
    { //perform ill-advised cast
      wmem2.putByte(0, (byte) 0);
      fail();
    } catch (AssertionError e) {
      println("Ill-advised cast 1");
    }

    ByteBuffer rbb = wbb.asReadOnlyBuffer();
    Memory mem2 = Memory.wrap(rbb); //created from RO BB
    mem2.getByte(0);

    try ( WritableMemory wmem3 = (WritableMemory) mem2; )
    { //perform ill-advised cast

      wmem3.putByte(0, (byte) 0);
      fail();
    } catch (AssertionError e) {
      println("Ill-advised cast 2");
    }

    try ( WritableMemory wmem4 = WritableMemory.writableWrap(rbb); )
    {
      wmem4.close();
      fail();
    } catch (IllegalArgumentException e) {
      println("Cannot assign a ReadOnly ByteBuffer to WritableMemory.");
    }
    wmem.close();
  }


  @Test
  public void checkByteBufDirect() {
    int n = 1024; //longs
    ByteBuffer bb = ByteBuffer.allocateDirect(n * 8);
    bb.order(ByteOrder.nativeOrder());
    WritableMemory wmem = WritableMemory.writableWrap(bb);
    for (int i = 0; i < n; i++) { //write to wmem
      wmem.putLong(i * 8, i);
    }
    for (int i = 0; i < n; i++) { //read from wmem
      long v = wmem.getLong(i * 8);
      assertEquals(v, i);
    }
    for (int i = 0; i < n; i++) { //read from BB
      long v = bb.getLong(i * 8);
      assertEquals(v, i);
    }
    Memory mem1 = Memory.wrap(bb);
    for (int i = 0; i < n; i++) { //read from wrapped bb RO
      long v = mem1.getLong(i * 8);
      assertEquals(v, i);
    }
    //convert to RO
    Memory mem = wmem.asReadOnly();
    for (int i = 0; i < n; i++) {
      long v = mem.getLong(i * 8);
      assertEquals(v, i);
    }
    wmem.close();
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
    Memory mem = Memory.wrap(arr);
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

  /**
   * @param s blah
   */
  static void println(String s) {
    //System.out.println(s);
  }

}
