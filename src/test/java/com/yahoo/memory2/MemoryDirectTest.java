/*
 * Copyright 2017, Yahoo! Inc. Licensed under the terms of the
 * Apache License 2.0. See LICENSE file at the project root for terms.
 */

package com.yahoo.memory2;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.testng.Assert;
import org.testng.annotations.Test;

public class MemoryDirectTest {

  @Test
  public void checkDirectRoundTrip() {
    int n = 1024; //longs
    WritableMemory2 mem = WritableMemory2.allocateDirect(n * 8, null);
    for (int i = 0; i < n; i++) mem.putLong(i * 8, i);
    for (int i = 0; i < n; i++) {
      long v = mem.getLong(i * 8);
      Assert.assertEquals(v, i);
    }

    mem.freeMemory();
  }

  @Test
  public void checkAutoHeapRoundTrip() {
    int n = 1024; //longs
    WritableMemory2 mem = WritableMemory2.allocate(n * 8);
    for (int i = 0; i < n; i++) mem.putLong(i * 8, i);
    for (int i = 0; i < n; i++) {
      long v = mem.getLong(i * 8);
      Assert.assertEquals(v, i);
    }
  }

  @Test
  public void checkArrayWrap() {
    int n = 1024; //longs
    byte[] arr = new byte[n * 8];
    WritableMemory2 wmem = WritableMemory2.writableWrap(arr);
    for (int i = 0; i < n; i++) wmem.putLong(i * 8, i);
    for (int i = 0; i < n; i++) {
      long v = wmem.getLong(i * 8);
      Assert.assertEquals(v, i);
    }
    Memory mem = Memory.wrap(arr);
    for (int i = 0; i < n; i++) {
      long v = mem.getLong(i * 8);
      Assert.assertEquals(v, i);
    }
  }

  @Test
  public void checkByteBufHeap() {
    int n = 1024; //longs
    byte[] arr = new byte[n * 8];
    ByteBuffer bb = ByteBuffer.wrap(arr);
    bb.order(ByteOrder.nativeOrder());
    WritableMemory2 wmem = WritableMemory2.writableWrap(bb);
    for (int i = 0; i < n; i++) { //write to wmem
      wmem.putLong(i * 8, i);
    }
    for (int i = 0; i < n; i++) { //read from wmem
      long v = wmem.getLong(i * 8);
      Assert.assertEquals(v, i);
    }
    for (int i = 0; i < n; i++) { //read from BB
      long v = bb.getLong(i * 8);
      Assert.assertEquals(v, i);
    }
    Memory mem1 = Memory.wrap(arr);
    for (int i = 0; i < n; i++) { //read from wrapped arr
      long v = mem1.getLong(i * 8);
      Assert.assertEquals(v, i);
    }
    //convert to RO
    Memory mem = wmem.asReadOnly();
    for (int i = 0; i < n; i++) {
      long v = mem.getLong(i * 8);
      Assert.assertEquals(v, i);
    }
  }

  @Test
  public void checkByteBufDirect() {
    int n = 1024; //longs
    ByteBuffer bb = ByteBuffer.allocateDirect(n * 8);
    bb.order(ByteOrder.nativeOrder());
    WritableMemory2 wmem = WritableMemory2.writableWrap(bb);
    for (int i = 0; i < n; i++) { //write to wmem
      wmem.putLong(i * 8, i);
    }
    for (int i = 0; i < n; i++) { //read from wmem
      long v = wmem.getLong(i * 8);
      Assert.assertEquals(v, i);
    }
    for (int i = 0; i < n; i++) { //read from BB
      long v = bb.getLong(i * 8);
      Assert.assertEquals(v, i);
    }
    Memory mem1 = Memory.wrap(bb);
    for (int i = 0; i < n; i++) { //read from wrapped bb RO
      long v = mem1.getLong(i * 8);
      Assert.assertEquals(v, i);
    }
    //convert to RO
    Memory mem = wmem.asReadOnly();
    for (int i = 0; i < n; i++) {
      long v = mem.getLong(i * 8);
      Assert.assertEquals(v, i);
    }
  }

  @Test
  public void checkPutGetArraysHeap() {
    int n = 1024; //longs
    long[] arr = new long[n];
    for (int i = 0; i < n; i++) { arr[i] = i; }
    WritableMemory2 wmem = WritableMemory2.allocate(n * 8);
    wmem.putLongArray(0, arr, 0, n);
    long[] arr2 = new long[n];
    wmem.getLongArray(0, arr2, 0, n);
    for (int i = 0; i < n; i++) {
      Assert.assertEquals(arr2[i], i);
    }
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
    WritableMemory2 wmem = WritableMemory2.writableWrap(arr);
    for (int i = 0; i < n; i++) { println("" + wmem.getLong(i * 8)); }
    println("");
    WritableMemory2 reg = wmem.writableRegion(n2 * 8, n2 * 8);
    for (int i = 0; i < n2; i++) { reg.putLong(i * 8, i); }
    for (int i = 0; i < n; i++) { println("" + wmem.getLong(i * 8)); }
  }

  @Test(expectedExceptions = AssertionError.class)
  public void checkCleaner() {
    WritableMemory2 wmem = WritableMemory2.allocateDirect(1024, null);
    wmem.putLong(0, -1L);
    wmem.freeMemory();
    wmem.putLong(0,0);
  }

  @Test
  public void printlnTest() {
    println("PRINTING: "+this.getClass().getName());
  }

  /**
   * @param s value to print
   */
  static void println(String s) {
    System.out.println(s); //disable here
  }


}
