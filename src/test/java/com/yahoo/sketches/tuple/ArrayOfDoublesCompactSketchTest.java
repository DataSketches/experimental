/*
 * Copyright 2015, Yahoo! Inc.
 * Licensed under the terms of the Apache License 2.0. See LICENSE file at the project root for terms.
 */
package com.yahoo.sketches.tuple;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.testng.annotations.Test;
import org.testng.Assert;

public class ArrayOfDoublesCompactSketchTest {
  //@Test
  public void emptyFromNonPublicConstructorNullArray() {
    ArrayOfDoublesCompactSketch sketch = new ArrayOfDoublesCompactSketch(null, null, Long.MAX_VALUE);
    Assert.assertTrue(sketch.isEmpty());
    Assert.assertFalse(sketch.isEstimationMode());
    Assert.assertEquals(sketch.getEstimate(), 0.0);
    Assert.assertEquals(sketch.getLowerBound(1), 0.0);
    Assert.assertEquals(sketch.getUpperBound(1), 0.0);
    Assert.assertEquals(sketch.getRetainedEntries(), 0);
    Assert.assertEquals(sketch.getThetaLong(), Long.MAX_VALUE);
    Assert.assertEquals(sketch.getTheta(), 1.0);
    Assert.assertNull(sketch.getValues());
  }

  @Test
  public void emptyFromNonPublicConstructor() {
    long[] keys = new long[0];
    double[][] values = new double[0][0];
    ArrayOfDoublesCompactSketch sketch = new ArrayOfDoublesCompactSketch(keys, values, Long.MAX_VALUE);
    Assert.assertTrue(sketch.isEmpty());
    Assert.assertFalse(sketch.isEstimationMode());
    Assert.assertEquals(sketch.getEstimate(), 0.0);
    Assert.assertEquals(sketch.getLowerBound(1), 0.0);
    Assert.assertEquals(sketch.getUpperBound(1), 0.0);
    Assert.assertEquals(sketch.getRetainedEntries(), 0);
    Assert.assertEquals(sketch.getThetaLong(), Long.MAX_VALUE);
    Assert.assertEquals(sketch.getTheta(), 1.0);
    Assert.assertNull(sketch.getValues());
  }

  @Test
  public void emptyFromQuickSelectSketch() {
    UpdatableArrayOfDoublesQuickSelectSketch qss = new UpdatableArrayOfDoublesQuickSelectSketch(8, 1);
    ArrayOfDoublesCompactSketch sketch = qss.compact();
    Assert.assertTrue(sketch.isEmpty());
    Assert.assertFalse(sketch.isEstimationMode());
    Assert.assertEquals(sketch.getEstimate(), 0.0);
    Assert.assertEquals(sketch.getLowerBound(1), 0.0);
    Assert.assertEquals(sketch.getUpperBound(1), 0.0);
    Assert.assertEquals(sketch.getRetainedEntries(), 0);
    Assert.assertEquals(sketch.getThetaLong(), Long.MAX_VALUE);
    Assert.assertEquals(sketch.getTheta(), 1.0);
    Assert.assertNull(sketch.getValues());
  }

  @Test
  public void exactModeFromQuickSelectSketch() {
    UpdatableArrayOfDoublesQuickSelectSketch qss = new UpdatableArrayOfDoublesQuickSelectSketch(8, 1);
    qss.update(1, new double[] {1.0});
    qss.update(2, new double[] {1.0});
    qss.update(3, new double[] {1.0});
    qss.update(1, new double[] {1.0});
    qss.update(2, new double[] {1.0});
    qss.update(3, new double[] {1.0});
    ArrayOfDoublesCompactSketch sketch = qss.compact();
    Assert.assertFalse(sketch.isEmpty());
    Assert.assertFalse(sketch.isEstimationMode());
    Assert.assertEquals(sketch.getEstimate(), 3.0);
    Assert.assertEquals(sketch.getLowerBound(1), 3.0);
    Assert.assertEquals(sketch.getUpperBound(1), 3.0);
    Assert.assertEquals(sketch.getRetainedEntries(), 3);
    Assert.assertEquals(sketch.getThetaLong(), Long.MAX_VALUE);
    Assert.assertEquals(sketch.getTheta(), 1.0);
    double[][] values = sketch.getValues();
    Assert.assertEquals(values.length, 3);
    for (double[] array: values) Assert.assertEquals(array[0], 2.0);
  }

  @Test
  public void serializeDeserializeSmallExact() {
    UpdatableArrayOfDoublesQuickSelectSketch qss = new UpdatableArrayOfDoublesQuickSelectSketch(32, 1);
    qss.update("a", new double[] {1.0});
    qss.update("b", new double[] {1.0});
    qss.update("c", new double[] {1.0});
    ArrayOfDoublesCompactSketch sketch1 = qss.compact();
    ByteBuffer buffer = sketch1.serializeToByteBuffer();
    Assert.assertTrue(buffer.order().equals(ByteOrder.nativeOrder()));
    buffer.rewind();
    ArrayOfDoublesCompactSketch sketch2 = new ArrayOfDoublesCompactSketch(buffer);
    Assert.assertFalse(sketch2.isEmpty());
    Assert.assertFalse(sketch2.isEstimationMode());
    Assert.assertEquals(sketch2.getEstimate(), 3.0);
    Assert.assertEquals(sketch2.getLowerBound(1), 3.0);
    Assert.assertEquals(sketch2.getUpperBound(1), 3.0);
    Assert.assertEquals(sketch2.getRetainedEntries(), 3);
    Assert.assertEquals(sketch2.getThetaLong(), Long.MAX_VALUE);
    Assert.assertEquals(sketch2.getTheta(), 1.0);
    double[][] values = sketch2.getValues();
    Assert.assertEquals(values.length, 3);
    for (double[] array: values) Assert.assertEquals(array[0], 1.0);
  }

  @Test
  public void serializeDeserializeEstimation() {
    UpdatableArrayOfDoublesQuickSelectSketch qss = new UpdatableArrayOfDoublesQuickSelectSketch(4096, 1);
    for (int i = 0; i < 8192; i++) qss.update(i, new double[] {1.0});
    ArrayOfDoublesCompactSketch sketch1 = qss.compact();
    ArrayOfDoublesCompactSketch sketch2 = new ArrayOfDoublesCompactSketch((ByteBuffer)sketch1.serializeToByteBuffer().rewind());
    Assert.assertFalse(sketch2.isEmpty());
    Assert.assertTrue(sketch2.isEstimationMode());
    Assert.assertEquals(sketch2.getEstimate(), sketch1.getEstimate());
    Assert.assertEquals(sketch2.getThetaLong(), sketch1.getThetaLong());
  }
}
