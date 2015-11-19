package com.yahoo.sketches.tuple;

import java.nio.ByteBuffer;

public interface SummaryFactory<S extends Summary> {
  public S newSummary();
  public SummarySetOperations<S> getSummarySetOperations();
  public S deserializeSummaryFromByteBuffer(ByteBuffer buffer);
  public ByteBuffer serializeToByteBuffer();
  // For deserialization there is a convention to have a constructor which takes ByteBuffer
}
