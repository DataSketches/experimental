package com.yahoo.sketches.hllmap;

import org.testng.Assert;
import org.testng.annotations.Test;

public class UniqueCountMapTest {

  @Test
  public void nullKey() {
    UniqueCountMap map = new UniqueCountMap(4);
    double estimate = map.update(null, null);
    Assert.assertEquals(estimate, 0.0);
  }

  @Test
  public void emptyMapNullValue() {
    UniqueCountMap map = new UniqueCountMap(4);
    double estimate = map.update("1234".getBytes(), null);
    Assert.assertEquals(estimate, 0.0);
  }

  //@Test
  public void oneEntry() {
    UniqueCountMap map = new UniqueCountMap(4);
    double estimate = map.update("1234".getBytes(), "a".getBytes());
    Assert.assertEquals(estimate, 1.0);
  }

  //@Test
  public void duplicateEntry() {
    UniqueCountMap map = new UniqueCountMap(4);
    double estimate = map.update("1234".getBytes(), "a".getBytes());
    Assert.assertEquals(estimate, 1.0);
    estimate = map.update("1234".getBytes(), "a".getBytes());
    Assert.assertEquals(estimate, 1.0);
  }

  //@Test
  public void oneKeyTwoValues() {
    UniqueCountMap map = new UniqueCountMap(4);
    double estimate = map.update("1234".getBytes(), "a".getBytes());
    Assert.assertEquals(estimate, 1.0);
    estimate = map.update("1234".getBytes(), "a".getBytes());
    Assert.assertEquals(estimate, 2.0);
  }

}
