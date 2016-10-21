/*
 * Copyright 2016, Yahoo! Inc. Licensed under the terms of the
 * Apache License 2.0. See LICENSE file at the project root for terms.
 */

package com.yahoo.sketches.hllmap;

import org.testng.Assert;
import org.testng.annotations.Test;

public class SingleCouponMapTest {

  @Test
  public void getEstimateNoneKey() {
    SingleCouponMap map = SingleCouponMap.getInstance(1000, 4);
    byte[] key = new byte[] {0, 0, 0, 1};
    Assert.assertEquals(map.getEstimate(key), 0.0);
  }

  @Test
  public void oneKeyOneValue() {
    int entries = 16;
    int keySizeBytes = 4;
    SingleCouponMap map = SingleCouponMap.getInstance(entries, keySizeBytes);
    byte[] key = new byte[] {0, 0, 0, 0}; // zero key must work
    byte[] id =  new byte[] {1, 0, 0, 0};
    int coupon = Map.coupon16(id);
    double estimate = map.update(key, coupon);
    Assert.assertEquals(estimate, 1.0);
    Assert.assertEquals(map.getEstimate(key), 1.0);
  }

  @Test
  public void resize() {
    int entries = 17;
    int numKeys = 1000;
    int keySizeBytes = 4;
    SingleCouponMap map = SingleCouponMap.getInstance(entries, keySizeBytes);

    for (int i = 0; i < numKeys; i++) {
      byte[] key = String.format("%4s", i).getBytes();
      byte[] id =  new byte[] {1, 0, 0, 0};
      int coupon = Map.coupon16(id);
      double estimate = map.update(key, coupon);
      Assert.assertEquals(estimate, 1.0);
    }
    for (int i = 0; i < numKeys; i++) {
      byte[] key = String.format("%4s", i).getBytes();
      double estimate = map.getEstimate(key);
      Assert.assertEquals(estimate, 1.0);
    }
    println(map.toString());
    Assert.assertEquals(map.getCurrentCountEntries(), numKeys);
  }

  @Test
  public void manyKeys() {
    SingleCouponMap map = SingleCouponMap.getInstance(2000, 4);
    for (int i = 1; i <= 1000; i++) {
      byte[] key = String.format("%4s", i).getBytes();
      double estimate = map.update(key, 1);
      Assert.assertEquals(estimate, 1.0);
    }
    for (int i = 1; i <= 1000; i++) {
      byte[] key = String.format("%4s", i).getBytes();
      double estimate = map.update(key, 2);
      Assert.assertEquals(estimate, -1.0);
    }
    Assert.assertEquals(map.getCurrentCountEntries(), 1000);
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
