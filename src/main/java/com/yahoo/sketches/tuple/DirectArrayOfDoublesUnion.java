/*
 * Copyright 2015, Yahoo! Inc.
 * Licensed under the terms of the Apache License 2.0. See LICENSE file at the project root for terms.
 */
package com.yahoo.sketches.tuple;

/**
 * This implementation keeps data in a given memory.
 * The purpose is to avoid garbage collection.
 */

import com.yahoo.sketches.memory.Memory;

public class DirectArrayOfDoublesUnion extends ArrayOfDoublesUnion {
  private Memory mem_;

  public DirectArrayOfDoublesUnion(int nomEntries, int numValues, Memory dstMem) {
    nomEntries_ = nomEntries;
    numValues_ = numValues;
    sketch_ = new DirectArrayOfDoublesQuickSelectSketch(nomEntries, numValues, dstMem);
    theta_ = sketch_.getThetaLong();
    mem_ = dstMem;
  }

  @Override
  public void reset() {
    sketch_ = new DirectArrayOfDoublesQuickSelectSketch(nomEntries_, numValues_, mem_);
  }
}
