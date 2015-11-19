package com.yahoo.sketches.tuple;

import java.nio.ByteBuffer;

public interface Summary {
  public <S extends Summary> S copy();
  public ByteBuffer serializeToByteBuffer();
  // For deserialization there is a convention to have a constructor which takes ByteBuffer
}
