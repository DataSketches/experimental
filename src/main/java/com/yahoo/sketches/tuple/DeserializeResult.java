package com.yahoo.sketches.tuple;

public class DeserializeResult<T> {
  private final T object;
  private final int size;

  public DeserializeResult(final T object, int size) {
    this.object = object;
    this.size = size;
  }

  public T getObject() {
    return object;
  }

  public int getSize() {
    return size;
  }
}
