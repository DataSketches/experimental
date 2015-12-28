/*
 * Copyright 2015, Yahoo! Inc.
 * Licensed under the terms of the Apache License 2.0. See LICENSE file at the project root for terms.
 */
package com.yahoo.sketches.tuple;

/**
 * This summary keeps a double value. On update a predefined operation is performed depending on the mode.
 * Three modes are supported: Sum, Min and Max. The default mode is Sum.
 */

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class DoubleSummary implements UpdatableSummary<Double> {
  public static enum Mode { Sum, Min, Max }

  private double value_;
  private Mode mode_;

  public DoubleSummary() {
    this(0, Mode.Sum);
  }

  public DoubleSummary(Mode mode) {
    mode_ = mode;
    switch (mode) {
    case Sum:
      value_ = 0;
      break;
    case Min:
      value_ = Double.POSITIVE_INFINITY;
      break;
    case Max:
      value_ = Double.NEGATIVE_INFINITY;
      break;
    }
  }

  public DoubleSummary(double value, Mode mode) {
    value_ = value;
    mode_ = mode;
  }

  public DoubleSummary(ByteBuffer buffer) {
    value_ = buffer.getDouble();
    mode_ = Mode.values()[buffer.get()];
  }

  @Override
  public void update(Double value) {
    switch(mode_) {
    case Sum:
      value_ += value.doubleValue();
      break;
    case Min:
      if (value < value_) value_ = value;
      break;
    case Max:
      if (value > value_) value_ = value;
      break;
    }
  }

  @SuppressWarnings("unchecked")
  @Override
  public DoubleSummary copy() {
    return new DoubleSummary(value_, mode_);
  }

  @Override
  public ByteBuffer serializeToByteBuffer() {
    ByteBuffer buffer = ByteBuffer.allocate(9).order(ByteOrder.nativeOrder());
    buffer.putDouble(value_);
    buffer.put((byte)mode_.ordinal());
    return buffer;
  }

  // TODO: remove
  public static DoubleSummary deserializeFromByteBuffer(ByteBuffer buffer) {
    return new DoubleSummary(buffer.getDouble(), Mode.values()[buffer.get()]);
  }

  public double getValue() {
    return value_;
  }
}
