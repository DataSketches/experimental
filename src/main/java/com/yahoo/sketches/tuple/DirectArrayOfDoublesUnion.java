package com.yahoo.sketches.tuple;

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
