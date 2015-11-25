package com.yahoo.sketches.tuple;

public class Union<S extends Summary> {
  private int nomEntries_;
  private SummaryFactory<S> summaryFactory_;
  private QuickSelectSketch<S> sketch_;
  private long theta_; // need to maintain outside of the sketch

  public Union(int nomEntries, SummaryFactory<S> summaryFactory) {
    nomEntries_ = nomEntries;
    summaryFactory_ = summaryFactory;
    sketch_ = new QuickSelectSketch<S>(nomEntries, summaryFactory);
    theta_ = sketch_.getThetaLong();
  }

  public void update(Sketch<S> sketchIn) {
    if (sketchIn == null || sketchIn.isEmpty()) return;
    if (sketchIn.theta_ < theta_) theta_ = sketchIn.theta_;
    for (int i = 0; i < sketchIn.keys_.length; i++) {
      if (sketchIn.summaries_[i] != null) {
        sketch_.merge(sketchIn.keys_[i], sketchIn.summaries_[i]);
      }
    }
  }

  public CompactSketch<S> getResult() {
    if (theta_ < sketch_.theta_) {
      sketch_.setThetaLong(theta_);
      sketch_.rebuild();
    }
    return sketch_.compact();
  }

  public void reset() {
    sketch_ = new QuickSelectSketch<S>(nomEntries_, summaryFactory_);
  }
}
