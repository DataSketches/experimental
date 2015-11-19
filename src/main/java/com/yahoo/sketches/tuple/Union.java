package com.yahoo.sketches.tuple;

public class Union<S extends Summary> {
  private int nomEntries_;
  private SummaryFactory<S> summaryFactory_;
  private QuickSelectSketch<S> sketch_;

  public Union(int nomEntries, SummaryFactory<S> summaryFactory) {
    this.nomEntries_ = nomEntries;
    this.summaryFactory_ = summaryFactory;
    sketch_ = new QuickSelectSketch<S>(nomEntries, summaryFactory);
  }

  public void update(Sketch<S> sketchIn) {
    if (sketchIn == null || sketchIn.isEmpty()) return;
    if (sketchIn.theta_ < sketch_.theta_) sketch_.theta_ = sketchIn.theta_;
    for (Entry<S> entry: sketchIn.getEntries()) sketch_.merge(entry.key_, entry.summary_);;
  }

  public CompactSketch<S> getResult() {
    return sketch_.compact();
  }

  public void reset() {
    sketch_ = new QuickSelectSketch<S>(nomEntries_, summaryFactory_);
  }
}
