package com.yahoo.sketches.hllmap;

import org.testng.Assert;
import org.testng.annotations.Test;

public class CouponTraverseMapTest {

  @Test
  public void getEstimateNovelKey() {
    CouponTraverseMap map = new CouponTraverseMap(1, 1);
    byte[] key = new byte[] {0};
    Assert.assertEquals(map.getEstimate(key), 0.0);
  }

  @Test
  public void oneKeyOneValue() {
    CouponTraverseMap map = new CouponTraverseMap(1, 1);
    byte[] key = new byte[] {0};
    double estimate = map.update(key, 1);
    Assert.assertEquals(estimate, 1.0);
    Assert.assertEquals(map.getEstimate(key), 1.0);
  }

  @Test
  public void resize() {
    CouponTraverseMap map = new CouponTraverseMap(4, 1);
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
