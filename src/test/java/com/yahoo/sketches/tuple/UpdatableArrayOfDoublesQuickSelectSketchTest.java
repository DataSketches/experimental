/*
 * Copyright 2015, Yahoo! Inc.
 * Licensed under the terms of the Apache License 2.0. See LICENSE file at the project root for terms.
 */
package com.yahoo.sketches.tuple;

import org.testng.annotations.Test;
import org.testng.Assert;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class UpdatableArrayOfDoublesQuickSelectSketchTest {
  @Test
  public void isEmpty() {
    UpdatableArrayOfDoublesQuickSelectSketch sketch = new UpdatableArrayOfDoublesQuickSelectSketch(4, 1);
    Assert.assertTrue(sketch.isEmpty());
    Assert.assertFalse(sketch.isEstimationMode());
    Assert.assertEquals(sketch.getEstimate(), 0.0);
    Assert.assertEquals(sketch.getUpperBound(1), 0.0);
    Assert.assertEquals(sketch.getLowerBound(1), 0.0);
    Assert.assertEquals(sketch.getThetaLong(), Long.MAX_VALUE);
    Assert.assertEquals(sketch.getTheta(), 1.0);
  }

  @Test
  public void isEmptyWithSampling() {
    float samplingProbability = 0.1f;
    UpdatableArrayOfDoublesQuickSelectSketch sketch = new UpdatableArrayOfDoublesQuickSelectSketch(4, samplingProbability, 1);
    Assert.assertTrue(sketch.isEmpty());
    Assert.assertFalse(sketch.isEstimationMode());
    Assert.assertEquals(sketch.getEstimate(), 0.0);
    Assert.assertEquals(sketch.getUpperBound(1), 0.0);
    Assert.assertEquals(sketch.getLowerBound(1), 0.0);
    Assert.assertEquals(sketch.getThetaLong() / (double) Long.MAX_VALUE, (double) samplingProbability);
    Assert.assertEquals(sketch.getTheta(), (double) samplingProbability);
  }

  @Test
  public void sampling() {
    float samplingProbability = 0.001f;
    UpdatableArrayOfDoublesQuickSelectSketch sketch = new UpdatableArrayOfDoublesQuickSelectSketch(4, samplingProbability, 1);
    sketch.update("a", new double[] {1.0});
    Assert.assertFalse(sketch.isEmpty());
    Assert.assertTrue(sketch.isEstimationMode());
    Assert.assertEquals(sketch.getEstimate(), 0.0);
    Assert.assertTrue(sketch.getUpperBound(1) > 0.0);
    Assert.assertEquals(sketch.getLowerBound(1), 0.0, 0.0000001);
    Assert.assertEquals(sketch.getThetaLong() / (double) Long.MAX_VALUE, (double) samplingProbability);
    Assert.assertEquals(sketch.getTheta(), (double) samplingProbability);
  }

  @Test
  public void exactMode() {
    UpdatableArrayOfDoublesQuickSelectSketch sketch = new UpdatableArrayOfDoublesQuickSelectSketch(4096, 1);
    Assert.assertTrue(sketch.isEmpty());
    Assert.assertEquals(sketch.getEstimate(), 0.0);
    for (int i = 1; i <= 4096; i++) sketch.update(i, new double[] {1.0});
    Assert.assertFalse(sketch.isEmpty());
    Assert.assertFalse(sketch.isEstimationMode());
    Assert.assertEquals(sketch.getEstimate(), 4096.0);
    Assert.assertEquals(sketch.getUpperBound(1), 4096.0);
    Assert.assertEquals(sketch.getLowerBound(1), 4096.0);
    Assert.assertEquals(sketch.getThetaLong(), Long.MAX_VALUE);
    Assert.assertEquals(sketch.getTheta(), 1.0);

    double[][] values = sketch.getValues();
    Assert.assertEquals(values.length, 4096);
    int count = 0;
    for (int i = 0; i < values.length; i++) if (values[i] != null) count++;
    Assert.assertEquals(count, 4096);
    for (int i = 0; i < 4096; i++) Assert.assertEquals(values[i][0], 1.0);
  }

  @Test
  // The moment of going into the estimation mode is, to some extent, an implementation detail
  // Here we assume that presenting as many unique values as twice the nominal size of the sketch will result in estimation mode
  public void estimationMode() {
    UpdatableArrayOfDoublesQuickSelectSketch sketch = new UpdatableArrayOfDoublesQuickSelectSketch(4096, 1);
    Assert.assertEquals(sketch.getEstimate(), 0.0);
    for (int i = 1; i <= 8192; i++) sketch.update(i, new double[] {1.0});
    Assert.assertTrue(sketch.isEstimationMode());
    Assert.assertEquals(sketch.getEstimate(), 8192, 8192 * 0.01);
    Assert.assertEquals(sketch.getEstimate(), sketch.getLowerBound(0));
    Assert.assertEquals(sketch.getEstimate(), sketch.getUpperBound(0));
    Assert.assertTrue(sketch.getEstimate() >= sketch.getLowerBound(1));
    Assert.assertTrue(sketch.getEstimate() <= sketch.getUpperBound(1));

    double[][] values = sketch.getValues();
    Assert.assertTrue(values.length >= 4096);
    int count = 0;
    for (double[] array: values) {
      if (array != null) {
        count++;
        Assert.assertEquals(array.length, 1);
        Assert.assertEquals(array[0], 1.0);
      }
    }
    Assert.assertEquals(count, values.length);
  }

  @Test
  public void updatesOfAllKeyTypes() {
    UpdatableArrayOfDoublesQuickSelectSketch sketch = new UpdatableArrayOfDoublesQuickSelectSketch(4096, 1);
    sketch.update(1L, new double[] {1.0});
    sketch.update(2.0, new double[] {1.0});
    sketch.update(new byte[] {3}, new double[] {1.0});
    sketch.update(new int[] {4}, new double[] {1.0});
    sketch.update(new long[] {5L}, new double[] {1.0});
    sketch.update("a", new double[] {1.0});
    Assert.assertEquals(sketch.getEstimate(), 6.0);
  }

  @Test
  public void doubleSum() {
    UpdatableArrayOfDoublesQuickSelectSketch sketch = new UpdatableArrayOfDoublesQuickSelectSketch(8, 1);
    sketch.update(1, new double[] {1.0});
    Assert.assertEquals(sketch.getRetainedEntries(), 1);
    Assert.assertEquals(sketch.getValues()[0][0], 1.0);
    sketch.update(1, new double[] {0.7});
    Assert.assertEquals(sketch.getRetainedEntries(), 1);
    Assert.assertEquals(sketch.getValues()[0][0], 1.7);
    sketch.update(1, new double[] {0.8});
    Assert.assertEquals(sketch.getRetainedEntries(), 1);
    Assert.assertEquals(sketch.getValues()[0][0], 2.5);
  }

  @Test
  public void serializeDeserializeExact() throws Exception {
    UpdatableArrayOfDoublesQuickSelectSketch sketch1 = new UpdatableArrayOfDoublesQuickSelectSketch(32, 1);
    sketch1.update(1, new double[] {1.0});

    UpdatableArrayOfDoublesQuickSelectSketch sketch2 = new UpdatableArrayOfDoublesQuickSelectSketch((java.nio.ByteBuffer)sketch1.serializeToByteBuffer().rewind());

    Assert.assertEquals(sketch2.getEstimate(), 1.0);
    double[][] values = sketch2.getValues();
    Assert.assertEquals(values.length, 1);
    Assert.assertEquals(values[0][0], 1.0);

    // the same key, so still one unique
    sketch2.update(1, new double[] {1.0});
    Assert.assertEquals(sketch2.getEstimate(), 1.0);

    sketch2.update(2, new double[] {1.0});
    Assert.assertEquals(sketch2.getEstimate(), 2.0);
  }

  @Test
  public void serializeDeserializeEstimation() throws Exception {
    UpdatableArrayOfDoublesQuickSelectSketch sketch1 = new UpdatableArrayOfDoublesQuickSelectSketch(4096, 1);
    for (int j = 0; j < 10; j++) {
      for (int i = 0; i < 8192; i++) sketch1.update(i, new double[] {1.0});
    }
    sketch1.trim();
    ByteBuffer buffer = sketch1.serializeToByteBuffer();
    Assert.assertTrue(buffer.order().equals(ByteOrder.nativeOrder()));
    buffer.rewind();
    TestUtil.writeBytesToFile(buffer.array(), "TupleSketchWithDoubleSummary4K.data");

    UpdatableArrayOfDoublesQuickSelectSketch sketch2 = new UpdatableArrayOfDoublesQuickSelectSketch(buffer);
    Assert.assertTrue(sketch2.isEstimationMode());
    Assert.assertEquals(sketch2.getEstimate(), 8192, 8192 * 0.99);
    Assert.assertEquals(sketch1.getTheta(), sketch2.getTheta());
    double[][] values = sketch2.getValues();
    Assert.assertEquals(values.length, 4096);
    for (double[] array: values) Assert.assertEquals(array[0], 10.0);
  }

  @Test
  public void serializeDeserializeSampling() throws Exception {
    int sketchSize = 16384;
    int numberOfUniques = sketchSize;
    UpdatableArrayOfDoublesQuickSelectSketch sketch1 = new UpdatableArrayOfDoublesQuickSelectSketch(sketchSize, 0.5f, 1);
    for (int i = 0; i < numberOfUniques; i++) sketch1.update(i, new double[] {1.0});
    ByteBuffer buffer = sketch1.serializeToByteBuffer();
    Assert.assertTrue(buffer.order().equals(ByteOrder.nativeOrder()));
    buffer.rewind();
    UpdatableArrayOfDoublesQuickSelectSketch sketch2 = new UpdatableArrayOfDoublesQuickSelectSketch(buffer);
    Assert.assertTrue(sketch2.isEstimationMode());
    Assert.assertEquals(sketch2.getEstimate() / (double) numberOfUniques, 1.0, 0.01);
    Assert.assertEquals(sketch2.getRetainedEntries() / (double) numberOfUniques, 0.5, 0.01);
    Assert.assertEquals(sketch1.getTheta(), sketch2.getTheta());
  }

  @Test
  public void unionExactMode() {
    UpdatableArrayOfDoublesQuickSelectSketch sketch1 = new UpdatableArrayOfDoublesQuickSelectSketch(8, 1);
    sketch1.update(1, new double[] {1.0});
    sketch1.update(1, new double[] {1.0});
    sketch1.update(1, new double[] {1.0});
    sketch1.update(2, new double[] {1.0});

    UpdatableArrayOfDoublesQuickSelectSketch sketch2 = new UpdatableArrayOfDoublesQuickSelectSketch(8, 1);
    sketch2.update(2, new double[] {1.0});
    sketch2.update(2, new double[] {1.0});
    sketch2.update(3, new double[] {1.0});
    sketch2.update(3, new double[] {1.0});
    sketch2.update(3, new double[] {1.0});

    ArrayOfDoublesUnion union = new ArrayOfDoublesUnion(8, 1);
    union.update(sketch1);
    union.update(sketch2);
    ArrayOfDoublesCompactSketch result = union.getResult();
    Assert.assertEquals(result.getEstimate(), 3.0);
    double[][] values = result.getValues();
    Assert.assertEquals(values[0][0], 3.0);
    Assert.assertEquals(values[1][0], 3.0);
    Assert.assertEquals(values[2][0], 3.0);
  
    union.reset();
    result = union.getResult();
    Assert.assertTrue(result.isEmpty());
    Assert.assertFalse(result.isEstimationMode());
    Assert.assertEquals(result.getEstimate(), 0.0);
    Assert.assertEquals(result.getUpperBound(1), 0.0);
    Assert.assertEquals(result.getLowerBound(1), 0.0);
    Assert.assertEquals(result.getTheta(), 1.0);
  }

}
