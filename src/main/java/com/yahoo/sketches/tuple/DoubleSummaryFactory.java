/*
 * Copyright 2015, Yahoo! Inc.
 * Licensed under the terms of the Apache License 2.0. See LICENSE file at the project root for terms.
 */
package com.yahoo.sketches.tuple;

import com.yahoo.sketches.memory.Memory;
import com.yahoo.sketches.memory.NativeMemory;
import com.yahoo.sketches.tuple.DoubleSummary.Mode;

/**
 * This is a factory for DoubleSummary. It supports three modes of operation of DoubleSummary:
 * Sum, Min and Max.
 */
public class DoubleSummaryFactory implements SummaryFactory<DoubleSummary> {

  private DoubleSummary.Mode summaryMode_;

  /**
   * Creates an instance of DoubleSummaryFactory with default mode
   */
  public DoubleSummaryFactory() {
    summaryMode_ = DoubleSummary.Mode.Sum;
  }

  /**
   * Creates an instance of DoubleSummaryFactory with a given mode
   * @param summaryMode summary mode
   */
  public DoubleSummaryFactory(DoubleSummary.Mode summaryMode) {
    summaryMode_ = summaryMode;
  }

  /**
   * Creates a new instance of a DoubleSummary
   * @return new DoubleSummary
   */
  @Override
  public DoubleSummary newSummary() {
    return new DoubleSummary(summaryMode_);
  }

  /**
   * This is to obtain methods of producing unions and intersections of two DoubleSummary objects
   * @return DoubleSummarySetOperations object
   */
  @Override
  public DoubleSummarySetOperations getSummarySetOperations() {
    return new DoubleSummarySetOperations(summaryMode_);
  }

  private static final int SERIALIZED_SIZE_BYTES = 1;
  private static final int MODE_BYTE = 0;

  /**
   * @return serialized representation of the DoubleSummaryFactory
   */
  @Override
  public byte[] toByteArray() {
    byte[] bytes = new byte[SERIALIZED_SIZE_BYTES];
    Memory mem = new NativeMemory(bytes);
    mem.putByte(MODE_BYTE, (byte) summaryMode_.ordinal());
    return bytes;
  }

  /**
   * Creates an instance of the DoubleSummaryFactory given a serialized representation
   * @param mem Memory object with serialized DoubleSummaryFactory
   * @return DeserializedResult object, which contains a DoubleSummaryFactory object and number of bytes read from the Memory
   */
  public static DeserializeResult<DoubleSummaryFactory> fromMemory(Memory mem) {
    return new DeserializeResult<DoubleSummaryFactory>(new DoubleSummaryFactory(Mode.values()[mem.getByte(MODE_BYTE)]), SERIALIZED_SIZE_BYTES);
  }

  /**
   * This is to create an instance of a DoubleSummary given a serialized representation
   * @param mem Memory object with serialized representation of a DoubleSummary
   * @return DeserializedResult object, which contains a DoubleSummary object and number of bytes read from the Memory
   */
  @Override
  public DeserializeResult<DoubleSummary> summaryFromMemory(Memory mem) {
    return DoubleSummary.fromMemory(mem);
  }

}
