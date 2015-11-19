package com.yahoo.sketches.tuple;

public interface SummarySetOperations<S extends Summary> {
  public S union(S a, S b);
  public S intersection(S a, S b);
}
