package com.yahoo.sketches.tuple;

public interface UpdatableSummary<U> extends Summary {

  void update(U value);

}
