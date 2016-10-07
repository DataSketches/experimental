/*
 * Copyright 2016, Yahoo! Inc. Licensed under the terms of the
 * Apache License 2.0. See LICENSE file at the project root for terms.
 */

package com.yahoo.sketches.hllmap;

import java.math.BigInteger;

public class UvcTableModel {
  private final double resizeFactor1_ = 1.8;
  private final double resizeFactor2_ = 1.2;
  private final double tableLoadFactor_ = 0.7;
  private final int resizeFactorThreshold_ = 1 << 30;

  private final int slotBytes_;

  private int capSlots_;
  private int usedSlots_;
  private int resizeThresholdSlots_;

  public UvcTableModel(int initialCapBytes, int keyBytes, int valBytes) {
    slotBytes_ = keyBytes + valBytes;
    capSlots_ = nextPrimeCapSlots(initialCapBytes, slotBytes_);
    usedSlots_ = 0;
    resizeThresholdSlots_ = (int) (capSlots_ * tableLoadFactor_);
  }
  /**
   * Returns capacity slots
   * @param initCapBytes the given initial capacity in bytes
   * @param slotBytes the given slot size in bytes
   * @return capacity slots
   */
  private static int nextPrimeCapSlots(int initCapBytes, int slotBytes) {
    int initSlots = initCapBytes / slotBytes;
    BigInteger p = BigInteger.valueOf(initSlots);
    int curCapSlots = p.nextProbablePrime().intValueExact();
    checkSlots(curCapSlots, slotBytes);
    return curCapSlots;
  }

  /**
   * Returns true if table was resized.
   * @return true if table was resized.
   */
  public boolean addSlot() {
    usedSlots_++;
    if (usedSlots_ > resizeThresholdSlots_) {
      double mult = (capSlots_ * slotBytes_ < resizeFactorThreshold_)
          ? resizeFactor1_ : resizeFactor2_;
      capSlots_ = nextPrimeCapSlots((int)(capSlots_*slotBytes_ * mult), slotBytes_);
      checkSlots(capSlots_, slotBytes_);
      resizeThresholdSlots_ = (int) (capSlots_ * tableLoadFactor_);
      return true;
    }
    return false;
  }

  private static void checkSlots(int capSlots, int slotBytes) {
    long capBytes = (long)capSlots * slotBytes;
    if (capBytes > Integer.MAX_VALUE) {
      throw new IllegalArgumentException("capacity bytes too large: " + capBytes);
    }
  }

  public int getCapSlots() {
    return capSlots_;
  }

  public int getCapBytes() {
    return capSlots_ * slotBytes_;
  }

  public int getUsedBytes() {
    return usedSlots_ * slotBytes_;
  }

  public void printStats() {
    String s1 = "Bytes: "+ Integer.toHexString(getUsedBytes());
    String s2 = "Cap  : "+ Integer.toHexString(getCapBytes());
    String s3 = "Slots: "+ Integer.toString(getCapSlots());
    println(s1 + "\t" + s2 + "\t" + s3);
  }

  public static void main(String[] args) {
    UvcTableModel model = new UvcTableModel(1 << 30, 4, 4);
    model.printStats();
    for (int i = 1; i< 30; i++) {
      model.addSlot();
      model.printStats();
    }
  }


  static void println(String s) { System.out.println(s); }
}
