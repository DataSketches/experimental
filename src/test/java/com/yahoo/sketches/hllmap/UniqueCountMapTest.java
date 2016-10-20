package com.yahoo.sketches.hllmap;

import static com.yahoo.sketches.hllmap.MapTestingUtil.TAB;

import org.testng.Assert;
import org.testng.annotations.Test;

public class UniqueCountMapTest {
  private static final int k_ = 512;

  @Test
  public void nullKey() {
    UniqueCountMap map = new UniqueCountMap(4, k_);
    double estimate = map.update(null, null);
    Assert.assertEquals(estimate, Double.NaN);
    Assert.assertEquals(map.getEstimate(null), Double.NaN);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void wrongSizeKeyUpdate() {
    UniqueCountMap map = new UniqueCountMap(4, k_);
    byte[] key = new byte[] {0};
    map.update(key, null);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void wrongSizeKeyGetEstimate() {
    UniqueCountMap map = new UniqueCountMap(4, k_);
    byte[] key = new byte[] {0};
    map.getEstimate(key);
  }

  @Test
  public void emptyMapNullValue() {
    UniqueCountMap map = new UniqueCountMap(4, k_);
    double estimate = map.update("1234".getBytes(), null);
    Assert.assertEquals(estimate, 0.0);
  }

  @Test
  public void oneEntry() {
    UniqueCountMap map = new UniqueCountMap(4, k_);
    double estimate = map.update("1234".getBytes(), "a".getBytes());
    Assert.assertEquals(estimate, 1.0, 0.01);
  }

  @Test
  public void duplicateEntry() {
    UniqueCountMap map = new UniqueCountMap(4, k_);
    byte[] key = "1234".getBytes();
    double estimate = map.update(key, "a".getBytes());
    Assert.assertEquals(estimate, 1.0);
    estimate = map.update(key, "a".getBytes());
    Assert.assertEquals(estimate, 1.0);
    estimate = map.update(key, null);
    Assert.assertEquals(estimate, 1.0);
  }

  @Test
  public void oneKeyTwoValues() {
    UniqueCountMap map = new UniqueCountMap(4, k_);
    double estimate = map.update("1234".getBytes(), "a".getBytes());
    Assert.assertEquals(estimate, 1.0);
    estimate = map.update("1234".getBytes(), "b".getBytes());
    Assert.assertEquals(estimate, 2.0, 0.02);
  }

  @Test
  public void oneKeyThreeValues() {
    UniqueCountMap map = new UniqueCountMap(4, k_);
    byte[] key = "1234".getBytes();
    double estimate = map.update(key, "a".getBytes());
    Assert.assertEquals(estimate, 1.0);
    estimate = map.update(key, "b".getBytes());
    Assert.assertEquals(estimate, 2.0);
    estimate = map.update(key, "c".getBytes());
    Assert.assertEquals(estimate, 3.0);
  }

  @Test
  public void oneKeyManyValues() {
    UniqueCountMap map = new UniqueCountMap(4, k_);
    byte[] key = "1234".getBytes();
    byte[] id = new byte[4];
    for (int i = 1; i <= 1000; i++) {
      id = MapTestingUtil.intToBytes(i, id);
      double estimate = map.update(key, id);
      if (i % 100 == 0) {
        double err = (estimate/i -1.0) * 100;
        String eStr = String.format("%.3f%%", err);
        println("i: "+i + "\t Est: " + estimate + TAB + eStr);
      }
      Assert.assertEquals(estimate, i, i * 0.10);
      Assert.assertEquals(map.getEstimate(key), estimate);
    }
  }
  static void println(String s) { System.out.println(s); }
  static void print(String s) { System.out.print(s); }
}
