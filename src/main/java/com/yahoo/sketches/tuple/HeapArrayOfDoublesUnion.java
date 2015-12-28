/*
 * Copyright 2015, Yahoo! Inc.
 * Licensed under the terms of the Apache License 2.0. See LICENSE file at the project root for terms.
 */
package com.yahoo.sketches.tuple;

public class HeapArrayOfDoublesUnion extends ArrayOfDoublesUnion {

  public HeapArrayOfDoublesUnion(int nomEntries, int numValues) {
    nomEntries_ = nomEntries;
    numValues_ = numValues;
    sketch_ = new HeapArrayOfDoublesQuickSelectSketch(nomEntries, numValues);
    theta_ = sketch_.getThetaLong();
  }

  @Override
  public void reset() {
    sketch_ = new HeapArrayOfDoublesQuickSelectSketch(nomEntries_, numValues_);
  }
}
