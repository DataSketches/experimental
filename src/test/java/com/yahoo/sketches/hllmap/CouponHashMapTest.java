package com.yahoo.sketches.hllmap;

import org.testng.Assert;
import org.testng.annotations.Test;

public class CouponHashMapTest {

  @Test
  public void getEstimateNovelKey() {
    CouponHashMap map = new CouponHashMap(1, 8);
    byte[] key = new byte[] {0};
    Assert.assertEquals(map.getEstimate(key), 0.0);
  }

  @Test
  public void oneKeyOneValue() {
    CouponHashMap map = new CouponHashMap(1, 8);
    byte[] key = new byte[] {0};
    double estimate = map.update(key, 1);
    Assert.assertEquals(estimate, 1.0);
    Assert.assertEquals(map.getEstimate(key), 1.0);
  }

  @Test
  public void resize() {
    CouponHashMap map = new CouponHashMap(4, 8);
    for (int i = 0; i < 1000; i++) {
      byte[] key = String.format("%4s", i).getBytes();
      double estimate = map.update(key, 1);
      Assert.assertEquals(estimate, 1.0);
    }
    for (int i = 0; i < 1000; i++) {
      byte[] key = String.format("%4s", i).getBytes();
      double estimate = map.getEstimate(key);
      Assert.assertEquals(estimate, 1.0);
    }
  }

}
