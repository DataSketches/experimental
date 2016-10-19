package com.yahoo.sketches.hllmap;

import org.testng.Assert;
import org.testng.annotations.Test;

public class CouponHashMapTest {
  static final int k_ = 512;

  @Test
  public void getEstimateNovelKey() {
    CouponHashMap map = CouponHashMap.getInstance(4, 16, k_);
    byte[] key = new byte[] {0, 0, 0, 0};
    Assert.assertEquals(map.getEstimate(key), 0.0);
  }

  @Test
  public void oneKeyOneValue() {
    CouponHashMap map = CouponHashMap.getInstance(4, 16, k_);
    byte[] key = new byte[] {0, 0, 0, 0};
    double estimate = map.update(key, 1);
    Assert.assertEquals(estimate, 1.0);
    Assert.assertEquals(map.getEstimate(key), 1.0);
  }

  @Test
  public void delete() {
    CouponHashMap map = CouponHashMap.getInstance(4, 16, k_);
    double estimate = map.update("1234".getBytes(), 1);
    Assert.assertEquals(estimate, 1.0);
    int index1 = map.findKey("1234".getBytes());
    Assert.assertTrue(index1 >= 0);
    map.deleteKey(index1);
    int index2 = map.findKey("1234".getBytes());
    // should be complement of the same index as before
    Assert.assertEquals(~index2, index1);
    Assert.assertEquals(map.getEstimate("1234".getBytes()), 0.0);
  }

  @Test
  public void growAndShrink() {
    CouponHashMap map = CouponHashMap.getInstance(4, 16, k_);
    long sizeBytes1 = map.getMemoryUsageBytes();
    for (int i = 0; i < 1000; i ++) {
      byte[] key = String.format("%4s", i).getBytes();
      map.update(key, Map.coupon16(new byte[] {1}));
    }
    long sizeBytes2 = map.getMemoryUsageBytes();
    Assert.assertTrue(sizeBytes2 > sizeBytes1);
    for (int i = 0; i < 1000; i ++) {
      byte[] key = String.format("%4s", i).getBytes();
      int index = map.findKey(key);
      Assert.assertTrue(index >= 0);
      map.deleteKey(index);
    }
    long sizeBytes3 = map.getMemoryUsageBytes();
    Assert.assertTrue(sizeBytes3 < sizeBytes2);
  }

}
