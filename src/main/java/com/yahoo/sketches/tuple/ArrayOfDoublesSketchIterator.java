/*
 * Copyright 2015, Yahoo! Inc.
 * Licensed under the terms of the Apache License 2.0. See LICENSE file at the project root for terms.
 */
package com.yahoo.sketches.tuple;

/**
 * Interface for iterating over ArrayOfDoublesSketch
 */
interface ArrayOfDoublesSketchIterator {
  boolean next();
  long getKey();
  double[] getValues();
}
