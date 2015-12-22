package com.yahoo.sketches.tuple;

interface ArrayOfDoublesSketchIterator {
  boolean next();
  long getKey();
  double[] getValues();
}
