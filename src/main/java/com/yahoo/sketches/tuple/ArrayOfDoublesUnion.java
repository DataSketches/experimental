/*
 * Copyright 2015, Yahoo! Inc.
 * Licensed under the terms of the Apache License 2.0. See LICENSE file at the project root for terms.
 */
package com.yahoo.sketches.tuple;

public abstract class ArrayOfDoublesUnion {
  protected int nomEntries_;
  protected int numValues_;
  protected ArrayOfDoublesQuickSelectSketch sketch_;
  protected long theta_;

  public void update(ArrayOfDoublesSketch sketchIn) {
    if (sketchIn == null || sketchIn.isEmpty()) return;
    if (sketchIn.getThetaLong() < theta_) theta_ = sketchIn.getThetaLong();
    ArrayOfDoublesSketchIterator it = sketchIn.iterator();
    while (it.next()) {
      sketch_.merge(it.getKey(), it.getValues());
    }
  }

  public ArrayOfDoublesCompactSketch getResult() {
    if (theta_ < sketch_.getThetaLong()) {
      sketch_.setThetaLong(theta_);
      sketch_.rebuild();
    }
    return sketch_.compact();
  }

  public abstract void reset();
}
