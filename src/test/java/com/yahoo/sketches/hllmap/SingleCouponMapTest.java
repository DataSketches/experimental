package com.yahoo.sketches.hllmap;

import org.testng.Assert;
import org.testng.annotations.Test;

public class SingleCouponMapTest {

  @Test
  public void getEstimateNoneKey() {
    SingleCouponMap map = SingleCouponMap.getInstance(1000, 4, 1.2F);
    byte[] key = new byte[] {0, 0, 0, 1};
    Assert.assertEquals(map.getEstimate(key), 0.0);
  }

  @Test
  public void oneKeyOneValue() {
    int entries = 16;
    int keySizeBytes = 4;
    float rf = 1.2F;
    SingleCouponMap map = SingleCouponMap.getInstance(entries, keySizeBytes, rf);
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
    float rf = 2F;
    SingleCouponMap map = SingleCouponMap.getInstance(entries, keySizeBytes, rf);

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
  }

  public static void main(String[] args) {
    SingleCouponMapTest test = new SingleCouponMapTest();
    test.resize();
  }

}
