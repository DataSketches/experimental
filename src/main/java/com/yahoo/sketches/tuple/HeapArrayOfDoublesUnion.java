/*
 * Copyright 2015, Yahoo! Inc.
 * Licensed under the terms of the Apache License 2.0. See LICENSE file at the project root for terms.
 */
package com.yahoo.sketches.tuple;

import static com.yahoo.sketches.Util.DEFAULT_UPDATE_SEED;

import com.yahoo.sketches.memory.Memory;

/**
 * This is on-heap implementation
 */

public class HeapArrayOfDoublesUnion extends ArrayOfDoublesUnion {

  public HeapArrayOfDoublesUnion(int nomEntries, int numValues) {
    this(nomEntries, numValues, DEFAULT_UPDATE_SEED);
  }

  public HeapArrayOfDoublesUnion(int nomEntries, int numValues, long seed) {
    super(new HeapArrayOfDoublesQuickSelectSketch(nomEntries, 3, 1f, numValues, seed));
  }

  public HeapArrayOfDoublesUnion(Memory mem) {
    this(mem, DEFAULT_UPDATE_SEED);
  }

  public HeapArrayOfDoublesUnion(Memory mem, long seed) {
    super(new HeapArrayOfDoublesQuickSelectSketch(mem, seed));
  }

  @Override
  public void reset() {
    sketch_ = new HeapArrayOfDoublesQuickSelectSketch(nomEntries_, 3, 1f, numValues_, seed_);
    theta_ = sketch_.getThetaLong();
  }

}
