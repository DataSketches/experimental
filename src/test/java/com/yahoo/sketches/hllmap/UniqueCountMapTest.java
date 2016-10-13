package com.yahoo.sketches.hllmap;

import org.testng.Assert;
import org.testng.annotations.Test;

public class UniqueCountMapTest {

  @Test
  public void nullKey() {
    UniqueCountMap map = new UniqueCountMap(1000, 4);
    double estimate = map.update(null, null);
    Assert.assertEquals(estimate, Double.NaN);
  }

  @Test
  public void emptyMapNullValue() {
    UniqueCountMap map = new UniqueCountMap(1000, 4);
    double estimate = map.update("1234".getBytes(), null);
    Assert.assertEquals(estimate, 0.0);
  }

  @Test
  public void oneEntry() {
    UniqueCountMap map = new UniqueCountMap(1000, 4);
    double estimate = map.update("1234".getBytes(), "a".getBytes());
    Assert.assertEquals(estimate, 1.0, 0.01);
  }

  @Test
  public void duplicateEntry() {
    UniqueCountMap map = new UniqueCountMap(1000, 4);
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
    UniqueCountMap map = new UniqueCountMap(1000, 4);
    double estimate = map.update("1234".getBytes(), "a".getBytes());
    Assert.assertEquals(estimate, 1.0);
    estimate = map.update("1234".getBytes(), "b".getBytes());
    Assert.assertEquals(estimate, 2.0, 0.02);
  }

  @Test
  public void oneKeyThreeValues() {
    UniqueCountMap map = new UniqueCountMap(1000, 4);
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
    UniqueCountMap map = new UniqueCountMap(200, 4);
    byte[] key = "1234".getBytes();
    for (int i = 1; i <= 1000; i++) {
      double estimate = map.update(key, Integer.toString(i).getBytes());
      Assert.assertEquals(estimate, i, i * 0.1);
      Assert.assertEquals(map.getEstimate(key), estimate);
    }
  }

}
