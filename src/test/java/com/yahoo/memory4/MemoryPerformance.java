/*
 * Copyright 2016, Yahoo! Inc. Licensed under the terms of the
 * Apache License 2.0. See LICENSE file at the project root for terms.
 */

package com.yahoo.memory4;

import static com.yahoo.memory4.UnsafeUtil.unsafe;
import static java.lang.Math.pow;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.LongBuffer;

import com.yahoo.memory.AllocMemory;
import com.yahoo.memory.NativeMemory;
import com.yahoo.sketches.Util;

@SuppressWarnings("unused")
public final class MemoryPerformance {
  private int arrLongs_;     //# entries in array
  private int lgMinTrials_;  //minimum # of trials
  private int lgMaxTrials_;  //maximum # of trials
  private int lgMinLongs_;   //minimum array longs
  private int lgMaxLongs_;   //maximum array longs
  private double ppo_;       //points per octave of arrLongs (x-axis)
  private double minGI_;     //min generating index
  private double maxGI_;     //max generating index
  private int lgMaxOps_;     //lgMaxLongs_ + lgMinTrials_
  //private long address_ = 0; //used for unsafe

  /**
   * Evaluate Memory performancy under different scenarios
   */
  public MemoryPerformance() {
    //Configure
    lgMinTrials_ = 6;  //was 6
    lgMaxTrials_ = 24;  //was 24
    lgMinLongs_ = 5;   //was 5
    lgMaxLongs_ = 26;  //was 26
    ppo_ = 4.0;        //was 4
    //Compute
    lgMaxOps_ = lgMaxLongs_ + lgMinTrials_;
    maxGI_ = ppo_ * lgMaxLongs_;
    minGI_ = ppo_ * lgMinLongs_;

  }

  private static class Point {
    double ppo;
    double gi;
    int arrLongs;
    int trials;
    long sumReadTrials_nS = 0;
    long sumWriteTrials_nS = 0;

    Point(final double ppo, final double gi, final int arrLongs, final int trials) {
      this.ppo = ppo;
      this.gi = gi;
      this.arrLongs = arrLongs;
      this.trials = trials;
    }

    public static void printHeader() {
      println("LgLongs\tLongs\tTrials\t#Ops\tAvgRTrial_nS\tAvgROp_nS\tAvgWTrial_nS\tAvgWOp_nS");
    }

    public void printRow() {
      final long numOps = (long)((double)trials * arrLongs);
      final double logArrLongs = gi / ppo;
      final double rTrial_nS = (double)sumReadTrials_nS / trials;
      final double wTrial_nS = (double)sumWriteTrials_nS / trials;
      final double rOp_nS = rTrial_nS / arrLongs;
      final double wOp_nS = wTrial_nS / arrLongs;
      //Print
      final String out = String.format("%6.2f\t%d\t%d\t%d\t%.1f\t%8.3f\t%.1f\t%8.3f",
          logArrLongs, arrLongs, trials, numOps, rTrial_nS, rOp_nS, wTrial_nS, wOp_nS);
      println(out);
    }
  }

  private Point getNextPoint(final Point p) {
    final int lastArrLongs = (int)pow(2.0, p.gi / ppo_);
    int nextArrLongs;
    double logArrLongs;
    do {
      logArrLongs = (++p.gi) / ppo_;
      if (p.gi > maxGI_) { return null; }
      nextArrLongs = (int)pow(2.0, logArrLongs);
    } while (nextArrLongs <= lastArrLongs);
    p.arrLongs = nextArrLongs;
    //compute trials
    final double logTrials = Math.min(lgMaxOps_ - logArrLongs, lgMaxTrials_);
    p.trials = (int)pow(2.0, logTrials);
    return p;
  }

  /*************************************/
  // JAVA HEAP
  /*************************************/

  private void testHeapArrayByIndex() {
    Point p = new Point(ppo_, minGI_ - 1, 1 << lgMinLongs_, 1 << lgMaxTrials_); //just below start
    Point.printHeader();
    while ((p = getNextPoint(p)) != null) { //an array size point
      final long[] array = new long[p.arrLongs];
      //Do all write trials first
      p.sumWriteTrials_nS = 0;
      for (int t = 0; t < p.trials; t++) {
        p.sumWriteTrials_nS += trial_HeapArrayByIndex(array, p.arrLongs, false); //a single trial write
      }
      //Do all read trials
      p.sumReadTrials_nS  = 0;
      for (int t = 0; t < p.trials; t++) {
        p.sumReadTrials_nS += trial_HeapArrayByIndex(array, p.arrLongs, true); //a single trial read
      }

      p.printRow();
    }
  }

  //Must do write trial first
  private static final long trial_HeapArrayByIndex(final long[] array, final int arrLongs,
      final boolean read) {
    final long checkSum = (arrLongs * (arrLongs - 1L)) / 2L;
    final long startTime_nS, stopTime_nS;
    if (read) {
      //Timing interval for a single trial
      long trialSum = 0;
      startTime_nS = System.nanoTime();
      for (int i = 0; i < arrLongs; i++) { trialSum += array[i]; }
      stopTime_nS = System.nanoTime();
      if (trialSum != checkSum) {
        throw new IllegalStateException("Bad checksum: " + trialSum + " != " + checkSum);
      }
    } else { //write
      startTime_nS = System.nanoTime();
      for (int i = 0; i < arrLongs; i++) { array[i] = i; }
      stopTime_nS = System.nanoTime();
    }
    return stopTime_nS - startTime_nS;
  }

  /*************************************/
  //  UNSAFE DIRECT
  /*************************************/

  private void testNativeArrayByUnsafe() {
    Point p = new Point(ppo_, minGI_ - 1, 1 << lgMinLongs_, 1 << lgMaxTrials_); //just below start
    Point.printHeader();
    while ((p = getNextPoint(p)) != null) { //an array size point
      final long address = unsafe.allocateMemory(p.arrLongs << 3);
      //Do all write trials first
      p.sumWriteTrials_nS = 0;
      for (int t = 0; t < p.trials; t++) { //do writes first: a single trial write
        p.sumWriteTrials_nS += trial_NativeArrayByUnsafe(address, p.arrLongs, false);
      }
      //Do all read trials
      p.sumReadTrials_nS  = 0;
      for (int t = 0; t < p.trials; t++) { //a single trial read
        p.sumReadTrials_nS += trial_NativeArrayByUnsafe(address, p.arrLongs, true);
      }
      p.printRow();
      unsafe.freeMemory(address);
    }
  }

  //Must do writes first
  private static final long trial_NativeArrayByUnsafe(final long address, final int arrLongs,
      final boolean read) {
    final long checkSum = (arrLongs * (arrLongs - 1L)) / 2L;
    final long startTime_nS, stopTime_nS;
    if (read) {
      //Timing interval for a single trial
      long trialSum = 0;
      startTime_nS = System.nanoTime();
      for (int i = 0; i < arrLongs; i++) {
        trialSum += unsafe.getLong(address + (i << 3));
      }
      stopTime_nS = System.nanoTime();
      if (trialSum != checkSum) {
        if (address != 0) { unsafe.freeMemory(address); }
        throw new IllegalStateException("Bad checksum: " + trialSum + " != " + checkSum);
      }
    } else { //write
      startTime_nS = System.nanoTime();
      for (int i = 0; i < arrLongs; i++) {
        unsafe.putLong(address + (i << 3), i);
      }
      stopTime_nS = System.nanoTime();
    }
    return stopTime_nS - startTime_nS;
  }

  /*************************************/
  //  BYTE BUFFER - HEAP
  /*************************************/

  private void testByteBufferHeap() {
    Point p = new Point(ppo_, minGI_ - 1, 1 << lgMinLongs_, 1 << lgMaxTrials_); //just below start
    Point.printHeader();
    while ((p = getNextPoint(p)) != null) { //an array size point
      final ByteBuffer buf = ByteBuffer.allocate(p.arrLongs << 3);
      buf.order(ByteOrder.nativeOrder());
      //Do all write trials first
      p.sumWriteTrials_nS = 0;
      for (int t = 0; t < p.trials; t++) { //do writes first
        p.sumWriteTrials_nS += trial_ByteBufferHeap(buf, p.arrLongs, false); //a single trial write
      }
      //Do all read trials
      p.sumReadTrials_nS  = 0;
      for (int t = 0; t < p.trials; t++) {
        p.sumReadTrials_nS += trial_ByteBufferHeap(buf, p.arrLongs, true); //a single trial read
      }
      p.printRow();
    }
  }

  //Must do writes first
  private static final long trial_ByteBufferHeap(final ByteBuffer buf, final int arrLongs,
      final boolean read) {
    final long checkSum = (arrLongs * (arrLongs - 1L)) / 2L;
    final long startTime_nS, stopTime_nS;
    if (read) {
      //Timing interval for a single trial
      long trialSum = 0;
      startTime_nS = System.nanoTime();
      for (int i = 0; i < arrLongs; i++) { trialSum += buf.getLong(i << 3); }
      stopTime_nS = System.nanoTime();
      if (trialSum != checkSum) {
        throw new IllegalStateException("Bad checksum: " + trialSum + " != " + checkSum);
      }
    } else { //write
      startTime_nS = System.nanoTime();
      for (int i = 0; i < arrLongs; i++) { buf.putLong(i << 3, i); }
      stopTime_nS = System.nanoTime();
    }
    return stopTime_nS - startTime_nS;
  }

  /*************************************/
  //  BYTE BUFFER - DIRECT
  /*************************************/

  private void testByteBufferDirect() {
    Point p = new Point(ppo_, minGI_ - 1, 1 << lgMinLongs_, 1 << lgMaxTrials_); //just below start
    Point.printHeader();
    while ((p = getNextPoint(p)) != null) { //an array size point
      final ByteBuffer buf = ByteBuffer.allocateDirect(p.arrLongs << 3);
      buf.order(ByteOrder.LITTLE_ENDIAN);
      //Do all write trials first
      p.sumWriteTrials_nS = 0;
      for (int t = 0; t < p.trials; t++) { //do writes first
        p.sumWriteTrials_nS += trial_ByteBufferDirect(buf, p.arrLongs, false); //a single trial write
      }
      //Do all read trials
      p.sumReadTrials_nS  = 0;
      for (int t = 0; t < p.trials; t++) {
        p.sumReadTrials_nS += trial_ByteBufferDirect(buf, p.arrLongs, true); //a single trial read
      }
      p.printRow();
    }
  }

  //Must do writes first
  private static final long trial_ByteBufferDirect(final ByteBuffer buf, final int arrLongs,
      final boolean read) {
    final long checkSum = (arrLongs * (arrLongs - 1L)) / 2L;
    final long startTime_nS, stopTime_nS;
    if (read) {
      //Timing interval for a single trial
      long trialSum = 0;
      startTime_nS = System.nanoTime();
      for (int i = 0; i < arrLongs; i++) { trialSum += buf.getLong(i << 3); }
      stopTime_nS = System.nanoTime();
      if (trialSum != checkSum) {
        throw new IllegalStateException("Bad checksum: " + trialSum + " != " + checkSum);
      }
    } else { //write
      startTime_nS = System.nanoTime();
      for (int i = 0; i < arrLongs; i++) { buf.putLong(i << 3, i); }
      stopTime_nS = System.nanoTime();
    }
    return stopTime_nS - startTime_nS;
  }

  /*************************************/
  //  LONG BUFFER - HEAP
  /*************************************/

  private void testLongBufferHeap() {
    Point p = new Point(ppo_, minGI_ - 1, 1 << lgMinLongs_, 1 << lgMaxTrials_); //just below start
    Point.printHeader();
    while ((p = getNextPoint(p)) != null) { //an array size point
      final long[] arr = new long[p.arrLongs];
      final LongBuffer buf = LongBuffer.wrap(arr);
      //buf.order(ByteOrder.LITTLE_ENDIAN);
      //Do all write trials first
      p.sumWriteTrials_nS = 0;
      for (int t = 0; t < p.trials; t++) { //do writes first
        p.sumWriteTrials_nS += trial_LongBufferHeap(buf, p.arrLongs, false); //a single trial write
      }
      //Do all read trials
      p.sumReadTrials_nS  = 0;
      for (int t = 0; t < p.trials; t++) {
        p.sumReadTrials_nS += trial_LongBufferHeap(buf, p.arrLongs, true); //a single trial read
      }
      p.printRow();
    }
  }

  //Must do writes first
  private static final long trial_LongBufferHeap(final LongBuffer buf, final int arrLongs,
      final boolean read) {
    final long checkSum = (arrLongs * (arrLongs - 1L)) / 2L;
    final long startTime_nS, stopTime_nS;
    if (read) {
      //Timing interval for a single trial
      long trialSum = 0;
      startTime_nS = System.nanoTime();
      for (int i = 0; i < arrLongs; i++) { trialSum += buf.get(i); }
      stopTime_nS = System.nanoTime();
      if (trialSum != checkSum) {
        throw new IllegalStateException("Bad checksum: " + trialSum + " != " + checkSum);
      }
    } else { //write
      startTime_nS = System.nanoTime();
      for (int i = 0; i < arrLongs; i++) { buf.put(i, i); }
      stopTime_nS = System.nanoTime();
    }
    return stopTime_nS - startTime_nS;
  }

  /*************************************/
  //  LONG BUFFER - DIRECT
  /*************************************/

  private void testLongBufferDirect() {
    Point p = new Point(ppo_, minGI_ - 1, 1 << lgMinLongs_, 1 << lgMaxTrials_); //just below start
    Point.printHeader();
    while ((p = getNextPoint(p)) != null) { //an array size point
      final ByteBuffer buf = ByteBuffer.allocateDirect(p.arrLongs << 3);
      final LongBuffer lbuf = buf.asLongBuffer();
      buf.order(ByteOrder.LITTLE_ENDIAN);
      //Do all write trials first
      p.sumWriteTrials_nS = 0;
      for (int t = 0; t < p.trials; t++) { //do writes first
        p.sumWriteTrials_nS += trial_LongBufferDirect(lbuf, p.arrLongs, false); //a single trial write
      }
      //Do all read trials
      p.sumReadTrials_nS  = 0;
      for (int t = 0; t < p.trials; t++) {
        p.sumReadTrials_nS += trial_LongBufferDirect(lbuf, p.arrLongs, true); //a single trial read
      }
      p.printRow();
    }
  }

  //Must do writes first
  private static final long trial_LongBufferDirect(final LongBuffer buf, final int arrLongs,
      final boolean read) {
    final long checkSum = (arrLongs * (arrLongs - 1L)) / 2L;
    final long startTime_nS, stopTime_nS;
    if (read) {
      //Timing interval for a single trial
      long trialSum = 0;
      startTime_nS = System.nanoTime();
      for (int i = 0; i < arrLongs; i++) { trialSum += buf.get(i); }
      stopTime_nS = System.nanoTime();
      if (trialSum != checkSum) {
        throw new IllegalStateException("Bad checksum: " + trialSum + " != " + checkSum);
      }
    } else { //write
      startTime_nS = System.nanoTime();
      for (int i = 0; i < arrLongs; i++) { buf.put(i, i); }
      stopTime_nS = System.nanoTime();
    }
    return stopTime_nS - startTime_nS;
  }

  /*************************************/
  //  NATIVE MEMORY FROM LIBRARY - HEAP
  /*************************************/

  private void testMemoryHeap() {
    Point p = new Point(ppo_, minGI_ - 1, 1 << lgMinLongs_, 1 << lgMaxTrials_); //just below start
    Point.printHeader();
    while ((p = getNextPoint(p)) != null) { //an array size point
      final byte[] array = new byte[p.arrLongs << 3];
      final NativeMemory mem = new NativeMemory(array);
      //Do all write trials first
      p.sumWriteTrials_nS = 0;
      for (int t = 0; t < p.trials; t++) { //do writes first
        p.sumWriteTrials_nS += trial_MemoryHeap(mem, p.arrLongs, false); //a single trial write
      }
      //Do all read trials
      p.sumReadTrials_nS  = 0;
      for (int t = 0; t < p.trials; t++) {
        p.sumReadTrials_nS += trial_MemoryHeap(mem, p.arrLongs, true); //a single trial read
      }
      p.printRow();
    }
  }

  //Must do writes first
  private static final long trial_MemoryHeap(final NativeMemory mem, final int arrLongs,
      final boolean read) {
    final long checkSum = (arrLongs * (arrLongs - 1L)) / 2L;
    final long startTime_nS, stopTime_nS;
    if (read) {
      //Timing interval for a single trial
      long trialSum = 0;
      startTime_nS = System.nanoTime();
      for (int i = 0; i < arrLongs; i++) { trialSum += mem.getLong(i << 3); }
      stopTime_nS = System.nanoTime();
      if (trialSum != checkSum) {
        throw new IllegalStateException("Bad checksum: " + trialSum + " != " + checkSum);
      }
    } else { //write
      startTime_nS = System.nanoTime();
      for (int i = 0; i < arrLongs; i++) { mem.putLong(i << 3, i); }
      stopTime_nS = System.nanoTime();
    }
    return stopTime_nS - startTime_nS;
  }

  /*************************************/
  //  NATIVE MEMORY FROM LIBRARY - DIRECT
  /*************************************/

  private void testMemoryDirect() {
    Point p = new Point(ppo_, minGI_ - 1, 1 << lgMinLongs_, 1 << lgMaxTrials_); //just below start
    Point.printHeader();
    while ((p = getNextPoint(p)) != null) { //an array size point
      final NativeMemory mem = new AllocMemory(p.arrLongs << 3);
      //Do all write trials first
      p.sumWriteTrials_nS = 0;
      for (int t = 0; t < p.trials; t++) { //do writes first
        p.sumWriteTrials_nS += trial_MemoryDirect(mem, p.arrLongs, false); //a single trial write
      }
      //Do all read trials
      p.sumReadTrials_nS  = 0;
      for (int t = 0; t < p.trials; t++) {
        p.sumReadTrials_nS += trial_MemoryDirect(mem, p.arrLongs, true); //a single trial read
      }
      p.printRow();
      mem.freeMemory();
    }
  }

  //Must do writes first
  private static final long trial_MemoryDirect(final NativeMemory mem, final int arrLongs,
      final boolean read) {
    final long checkSum = (arrLongs * (arrLongs - 1L)) / 2L;
    final long startTime_nS, stopTime_nS;
    if (read) {
      //Timing interval for a single trial
      long trialSum = 0;
      startTime_nS = System.nanoTime();
      for (int i = 0; i < arrLongs; i++) { trialSum += mem.getLong(i << 3); }
      stopTime_nS = System.nanoTime();
      if (trialSum != checkSum) {
        throw new IllegalStateException("Bad checksum: " + trialSum + " != " + checkSum);
      }
    } else { //write
      startTime_nS = System.nanoTime();
      for (int i = 0; i < arrLongs; i++) { mem.putLong(i << 3, i); }
      stopTime_nS = System.nanoTime();
    }
    return stopTime_nS - startTime_nS;
  }

  /*************************************/
  //  NATIVE MEMORY FROM LIBRARY - HEAP UNSAFE
  /*************************************/

  private void testMemoryHeapUnsafe() {
    Point p = new Point(ppo_, minGI_ - 1, 1 << lgMinLongs_, 1 << lgMaxTrials_); //just below start
    Point.printHeader();
    while ((p = getNextPoint(p)) != null) { //an array size point
      final byte[] array = new byte[p.arrLongs << 3];
      final NativeMemory mem = new NativeMemory(array);
      //Do all write trials first
      p.sumWriteTrials_nS = 0;
      for (int t = 0; t < p.trials; t++) { //do writes first
        p.sumWriteTrials_nS += trial_MemoryHeapUnsafe(mem, p.arrLongs, false); //a single trial write
      }
      //Do all read trials
      p.sumReadTrials_nS  = 0;
      for (int t = 0; t < p.trials; t++) {
        p.sumReadTrials_nS += trial_MemoryHeapUnsafe(mem, p.arrLongs, true); //a single trial read
      }
      p.printRow();
    }
  }

  //Must do writes first
  private static final long trial_MemoryHeapUnsafe(final NativeMemory mem, final int arrLongs,
      final boolean read) {
    final Object memObj = mem.array();
    final long memAdd = mem.getCumulativeOffset(0L);
    final long checkSum = (arrLongs * (arrLongs - 1L)) / 2L;
    final long startTime_nS, stopTime_nS;
    if (read) {
      //Timing interval for a single trial
      long trialSum = 0;
      startTime_nS = System.nanoTime();
      for (int i = 0; i < arrLongs; i++) {
        trialSum += unsafe.getLong(memObj, memAdd + (i << 3));
        //trialSum += mem.getLong(i << 3);
      }
      stopTime_nS = System.nanoTime();
      if (trialSum != checkSum) {
        throw new IllegalStateException("Bad checksum: " + trialSum + " != " + checkSum);
      }
    } else { //write
      startTime_nS = System.nanoTime();
      for (int i = 0; i < arrLongs; i++) {
        unsafe.putLong(memObj, memAdd + (i << 3), i);
        //mem.putLong(i << 3, i);
      }
      stopTime_nS = System.nanoTime();
    }
    return stopTime_nS - startTime_nS;
  }

  /*************************************/
  //  NATIVE MEMORY FROM LIBRARY - DIRECT UNSAFE
  /*************************************/

  private void testMemoryDirectUnsafe() {
    Point p = new Point(ppo_, minGI_ - 1, 1 << lgMinLongs_, 1 << lgMaxTrials_); //just below start
    Point.printHeader();
    while ((p = getNextPoint(p)) != null) { //an array size point
      final NativeMemory mem = new AllocMemory(p.arrLongs << 3);
      //Do all write trials first
      p.sumWriteTrials_nS = 0;
      for (int t = 0; t < p.trials; t++) { //do writes first
        p.sumWriteTrials_nS += trial_MemoryDirectUnsafe(mem, p.arrLongs, false); //a single trial write
      }
      //Do all read trials
      p.sumReadTrials_nS  = 0;
      for (int t = 0; t < p.trials; t++) {
        p.sumReadTrials_nS += trial_MemoryDirectUnsafe(mem, p.arrLongs, true); //a single trial read
      }
      p.printRow();
      mem.freeMemory();
    }
  }

  //Must do writes first
  private static final long trial_MemoryDirectUnsafe(final NativeMemory mem, final int arrLongs,
      final boolean read) {
    final Object memObj = mem.array();
    final long memAdd = mem.getCumulativeOffset(0L);
    final long checkSum = (arrLongs * (arrLongs - 1L)) / 2L;
    final long startTime_nS, stopTime_nS;
    if (read) {
      //Timing interval for a single trial
      long trialSum = 0;
      startTime_nS = System.nanoTime();
      for (int i = 0; i < arrLongs; i++) {
        trialSum += unsafe.getLong(memObj, memAdd + (i << 3));
        //trialSum += mem.getLong(i << 3);
      }
      stopTime_nS = System.nanoTime();
      if (trialSum != checkSum) {
        throw new IllegalStateException("Bad checksum: " + trialSum + " != " + checkSum);
      }
    } else { //write
      startTime_nS = System.nanoTime();
      for (int i = 0; i < arrLongs; i++) {
        unsafe.putLong(memObj, memAdd + (i << 3), i);
        //mem.putLong(i << 3, i);
      }
      stopTime_nS = System.nanoTime();
    }
    return stopTime_nS - startTime_nS;
  }

  /*************************************/
  //  MEMORY Version 4 - HEAP
  /*************************************/

  private void testMemory4Heap() {
    Point p = new Point(ppo_, minGI_ - 1, 1 << lgMinLongs_, 1 << lgMaxTrials_); //just below start
    Point.printHeader();
    while ((p = getNextPoint(p)) != null) { //an array size point
      final WritableMemory mem = WritableMemory.allocate(p.arrLongs << 3);
      //Do all write trials first
      p.sumWriteTrials_nS = 0;
      for (int t = 0; t < p.trials; t++) { //do writes first
        p.sumWriteTrials_nS += trial_Memory4Heap(mem, p.arrLongs, false); //a single trial write
      }
      //Do all read trials
      p.sumReadTrials_nS  = 0;
      for (int t = 0; t < p.trials; t++) {
        p.sumReadTrials_nS += trial_Memory4Heap(mem, p.arrLongs, true); //a single trial read
      }
      p.printRow();
    }
  }

  //Must do writes first
  private static final long trial_Memory4Heap(final WritableMemory mem, final int arrLongs,
      final boolean read) {
    final long checkSum = (arrLongs * (arrLongs - 1L)) / 2L;
    final long startTime_nS, stopTime_nS;
    if (read) {
      //Timing interval for a single trial
      long trialSum = 0;
      startTime_nS = System.nanoTime();
      for (int i = 0; i < arrLongs; i++) { trialSum += mem.getLong(i << 3); }
      stopTime_nS = System.nanoTime();
      if (trialSum != checkSum) {
        throw new IllegalStateException("Bad checksum: " + trialSum + " != " + checkSum);
      }
    } else { //write
      startTime_nS = System.nanoTime();
      for (int i = 0; i < arrLongs; i++) { mem.putLong(i << 3, i); }
      stopTime_nS = System.nanoTime();
    }
    return stopTime_nS - startTime_nS;
  }

  /*************************************/
  //  MEMORY Version 4 - DIRECT
  /*************************************/

  private void testMemory4Direct() {
    Point p = new Point(ppo_, minGI_ - 1, 1 << lgMinLongs_, 1 << lgMaxTrials_); //just below start
    Point.printHeader();
    while ((p = getNextPoint(p)) != null) { //an array size point
      WritableResourceHandler wh = WritableMemory.allocateDirect(p.arrLongs << 3);
      final WritableMemory mem = wh.get();
      //Do all write trials first
      p.sumWriteTrials_nS = 0;
      for (int t = 0; t < p.trials; t++) { //do writes first
        p.sumWriteTrials_nS += trial_Memory4Direct(mem, p.arrLongs, false); //a single trial write
      }
      //Do all read trials
      p.sumReadTrials_nS  = 0;
      for (int t = 0; t < p.trials; t++) {
        p.sumReadTrials_nS += trial_Memory4Direct(mem, p.arrLongs, true); //a single trial read
      }
      p.printRow();
      wh.close();
    }
  }

  //Must do writes first
  private static final long trial_Memory4Direct(final WritableMemory mem, final int arrLongs,
      final boolean read) {
    final long checkSum = (arrLongs * (arrLongs - 1L)) / 2L;
    final long startTime_nS, stopTime_nS;
    if (read) {
      //Timing interval for a single trial
      long trialSum = 0;
      startTime_nS = System.nanoTime();
      for (int i = 0; i < arrLongs; i++) { trialSum += mem.getLong(i << 3); }
      stopTime_nS = System.nanoTime();
      if (trialSum != checkSum) {
        throw new IllegalStateException("Bad checksum: " + trialSum + " != " + checkSum);
      }
    } else { //write
      startTime_nS = System.nanoTime();
      for (int i = 0; i < arrLongs; i++) { mem.putLong(i << 3, i); }
      stopTime_nS = System.nanoTime();
    }
    return stopTime_nS - startTime_nS;
  }

  /*************************************/

  /**
   * Start the testing
   */
  public void go() {
    final long startMillis = System.currentTimeMillis();
    //    println("Test Long Array On Heap");
    //    testHeapArrayByIndex();
    //    println("\nTest Direct Memory By Unsafe");
    //    testNativeArrayByUnsafe();
    //    println("\nTest ByteBuffer Heap");
    //    testByteBufferHeap();
    //    println("\nTest ByteBuffer Direct");
    //    testByteBufferDirect();
    //    println("\nTest LongBuffer Heap");
    //    testLongBufferHeap();
    //    println("\nTest LongBuffer Direct");
    //    testLongBufferDirect();
    println("\nTest Memory Heap");
    testMemoryHeap();
    println("\nTest Memory Direct");
    testMemoryDirect();
    //    println("\nTest Memory Heap Unsafe");
    //    testMemoryHeapUnsafe();
    //    println("\nTest Memory Direct Unsafe");
    //    testMemoryDirectUnsafe();
    println("\nTest Memory 4 Heap");
    testMemory4Heap();
    println("\nTest Memory 4 Direct");
    testMemory4Direct();

    final long testMillis = System.currentTimeMillis() - startMillis;
    println("Total Time: " + milliSecToString(testMillis));
  }

  /**
   * MAIN
   * @param args not used
   */
  public static void main(final String[] args) {
    final MemoryPerformance test = new MemoryPerformance();
    test.go();
  }

  //Handy utils

  public static void println(final String s) { System.out.println(s); }

  /**
   * copied from com.yahoo.sketches.TestingUtil which is a test class not in the main jar
   * @param mS milliseconds
   * @return string
   */
  public static String milliSecToString(final long mS) {
    final long rem_mS = (long)(mS % 1000.0D);
    final long rem_sec = (long)(mS / 1000.0D % 60.0D);
    final long rem_min = (long)(mS / 60000.0D % 60.0D);
    final long hr = (long)(mS / 3600000.0D);
    final String mSstr = Util.zeroPad(Long.toString(rem_mS), 3);
    final String secStr = Util.zeroPad(Long.toString(rem_sec), 2);
    final String minStr = Util.zeroPad(Long.toString(rem_min), 2);
    return String.format("%d:%2s:%2s.%3s", hr, minStr, secStr, mSstr);
  }
}
