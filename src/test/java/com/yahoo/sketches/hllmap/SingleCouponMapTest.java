package com.yahoo.sketches.hllmap;

import org.testng.Assert;
import org.testng.annotations.Test;

public class SingleCouponMapTest {

  @Test
  public void getEstimateNovelKey() {
    SingleCouponMap map = new SingleCouponMap(1000, 1);
    byte[] key = new byte[] {0};
    Assert.assertEquals(map.getEstimate(key), 0.0);
  }

  @Test
  public void oneKeyOneValue() {
    SingleCouponMap map = new SingleCouponMap(1000, 1);
    byte[] key = new byte[] {0};
    double estimate = map.update(key, 1);
    Assert.assertEquals(estimate, 1.0);
    Assert.assertEquals(map.getEstimate(key), 1.0);
  }

  @Test
  public void resize() {
    SingleCouponMap map = new SingleCouponMap(1000, 4);
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
