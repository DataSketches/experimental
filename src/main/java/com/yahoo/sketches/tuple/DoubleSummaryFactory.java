package com.yahoo.sketches.tuple;

/**
 * This is a factory for DoubleSummary. It supports three modes of operation of DoubleSummary:
 * Sum, Min and Max.
 */

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class DoubleSummaryFactory implements SummaryFactory<DoubleSummary> {
  private DoubleSummary.Mode summaryMode_;

  public DoubleSummaryFactory() {
    summaryMode_ = DoubleSummary.Mode.Sum;
  }

  public DoubleSummaryFactory(DoubleSummary.Mode summaryMode) {
    summaryMode_ = summaryMode;
  }

  public DoubleSummaryFactory(ByteBuffer buffer) {
    summaryMode_ = DoubleSummary.Mode.values()[buffer.get()];
  }

  @Override
  public DoubleSummary newSummary() {
    return new DoubleSummary(summaryMode_);
  }

  @Override
  public DoubleSummarySetOperations getSummarySetOperations() {
    return new DoubleSummarySetOperations(summaryMode_);
  }

  @Override
  public DoubleSummary deserializeSummaryFromByteBuffer(ByteBuffer buffer) {
    return new DoubleSummary(buffer);
  }

  public ByteBuffer serializeToByteBuffer() {
    return ByteBuffer.allocate(1).order(ByteOrder.nativeOrder()).put((byte)summaryMode_.ordinal());
  }

  public static DoubleSummaryFactory deserializeFromByteBuffer(ByteBuffer buffer) {
    return new DoubleSummaryFactory(DoubleSummary.Mode.values()[buffer.get()]);
  }
}