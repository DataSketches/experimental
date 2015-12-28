/*
 * Copyright 2015, Yahoo! Inc.
 * Licensed under the terms of the Apache License 2.0. See LICENSE file at the project root for terms.
 */
package com.yahoo.sketches.tuple;

import java.nio.ByteBuffer;

public interface SummaryFactory<S extends Summary> {
  public S newSummary();
  public SummarySetOperations<S> getSummarySetOperations();
  public S deserializeSummaryFromByteBuffer(ByteBuffer buffer);
  public ByteBuffer serializeToByteBuffer();
  // For deserialization there is a convention to have a constructor which takes ByteBuffer
}
