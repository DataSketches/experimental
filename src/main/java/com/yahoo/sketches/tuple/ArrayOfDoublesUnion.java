package com.yahoo.sketches.tuple;

public class ArrayOfDoublesUnion {
  private int nomEntries_;
  private int numValues_;
  private ArrayOfDoublesQuickSelectSketch sketch_;
  private long theta_;

  public ArrayOfDoublesUnion(int nomEntries, int numValues) {
    nomEntries_ = nomEntries;
    numValues_ = numValues;
    sketch_ = new ArrayOfDoublesQuickSelectSketch(nomEntries, numValues);
    theta_ = sketch_.getThetaLong();
  }

  public void update(ArrayOfDoublesSketch sketchIn) {
    if (sketchIn == null || sketchIn.isEmpty()) return;
    if (sketchIn.getThetaLong() < theta_) theta_ = sketchIn.getThetaLong();
    for (int i = 0; i < sketchIn.keys_.length; i++) {
      if (sketchIn.values_[i] != null) {
        sketch_.merge(sketchIn.keys_[i], sketchIn.values_[i]);
      }
    }
  }

  public ArrayOfDoublesCompactSketch getResult() {
    if (theta_ < sketch_.theta_) {
      sketch_.setThetaLong(theta_);
      sketch_.rebuild();
    }
    return sketch_.compact();
  }

  public void reset() {
    sketch_ = new ArrayOfDoublesQuickSelectSketch(nomEntries_, numValues_);
  }
}
