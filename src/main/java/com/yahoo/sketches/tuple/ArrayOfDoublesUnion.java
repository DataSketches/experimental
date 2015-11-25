package com.yahoo.sketches.tuple;

public class ArrayOfDoublesUnion {
  private int nomEntries_;
  private int numValues_;
  private ArrayOfDoublesQuickSelectSketch sketch_;

  public ArrayOfDoublesUnion(int nomEntries, int numValues) {
    nomEntries_ = nomEntries;
    numValues_ = numValues;
    sketch_ = new ArrayOfDoublesQuickSelectSketch(nomEntries, numValues);
  }

  public void update(ArrayOfDoublesSketch sketchIn) {
    if (sketchIn == null || sketchIn.isEmpty()) return;
    if (sketchIn.theta_ < sketch_.theta_) sketch_.theta_ = sketchIn.theta_;
    sketch_.merge(sketchIn);
  }

  public ArrayOfDoublesCompactSketch getResult() {
    return sketch_.compact();
  }

  public void reset() {
    sketch_ = new ArrayOfDoublesQuickSelectSketch(nomEntries_, numValues_);
  }
}
