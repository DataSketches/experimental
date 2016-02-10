/*
 * Copyright 2015, Yahoo! Inc.
 * Licensed under the terms of the Apache License 2.0. See LICENSE file at the project root for terms.
 */
package com.yahoo.sketches.tuple;

import static com.yahoo.sketches.Util.DEFAULT_UPDATE_SEED;

import com.yahoo.sketches.memory.Memory;

/**
 * This implementation keeps data in a given memory.
 * The purpose is to avoid garbage collection.
 */
public class DirectArrayOfDoublesUnion extends ArrayOfDoublesUnion {

  private Memory mem_;

  public DirectArrayOfDoublesUnion(int nomEntries, int numValues, Memory dstMem) {
    this(nomEntries, numValues, DEFAULT_UPDATE_SEED, dstMem);
  }

  public DirectArrayOfDoublesUnion(int nomEntries, int numValues, long seed, Memory dstMem) {
    super(new DirectArrayOfDoublesQuickSelectSketch(nomEntries, 3, 1f, numValues, seed, dstMem));
    mem_ = dstMem;
  }

  public DirectArrayOfDoublesUnion(Memory mem) {
    this(mem, DEFAULT_UPDATE_SEED);
  }

  public DirectArrayOfDoublesUnion(Memory mem, long seed) {
    super(new DirectArrayOfDoublesQuickSelectSketch(mem, seed));
    mem_ = mem;
  }

  @Override
  public void reset() {
    sketch_ = new DirectArrayOfDoublesQuickSelectSketch(nomEntries_, numValues_, mem_);
    theta_ = sketch_.getThetaLong();
  }

}
