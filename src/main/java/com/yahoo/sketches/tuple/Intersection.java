/*
 * Copyright 2015, Yahoo! Inc.
 * Licensed under the terms of the Apache License 2.0. See LICENSE file at the project root for terms.
 */
package com.yahoo.sketches.tuple;

import static java.lang.Math.min;

import java.lang.reflect.Array;

public class Intersection<S extends Summary> {

  private SummaryFactory<S> summaryFactory_;
  private QuickSelectSketch<S> sketch_;
  private boolean isEmpty_;
  private long theta_;
  private boolean isFirstCall_;

  public Intersection(SummaryFactory<S> summaryFactory) {
    summaryFactory_ = summaryFactory;
    isEmpty_ = false; // universal set at the start
    theta_ = Long.MAX_VALUE;
    isFirstCall_ = true;
  }

  // assumes that constructor of QuickSelectSketch bumps the requested size up to the nearest power of 2
  void update(Sketch<S> sketchIn) {
    boolean isFirstCall = isFirstCall_;
    isFirstCall_ = false;
    if (sketchIn == null) {
      isEmpty_ = true;
      sketch_ = null;
      return;
    }
    theta_ = min(theta_, sketchIn.getThetaLong());
    isEmpty_ |= sketchIn.isEmpty();
    if (sketchIn.getRetainedEntries() == 0) return;
    if (isFirstCall) {
      sketch_ = new QuickSelectSketch<S>(sketchIn.getRetainedEntries(), 0, summaryFactory_);
      for (int i = 0; i < sketchIn.keys_.length; i++) {
        if (sketchIn.summaries_[i] != null) sketch_.insert(sketchIn.keys_[i], sketchIn.summaries_[i]);
      }
    } else {
      int matchSize = min(sketch_.getRetainedEntries(), sketchIn.getRetainedEntries());
      long[] matchKeys = new long[matchSize];
      @SuppressWarnings("unchecked")
      S[] matchSummaries = (S[]) Array.newInstance(summaryFactory_.newSummary().getClass(), matchSize);
      int matchCount = 0;
      for (int i = 0; i < sketchIn.keys_.length; i++) {
        if (sketchIn.summaries_[i] != null) {
          S summary = sketch_.find(sketchIn.keys_[i]);
          if (summary != null) {
            matchKeys[matchCount] = sketchIn.keys_[i];
            matchSummaries[matchCount] = summaryFactory_.getSummarySetOperations().intersection(summary, sketchIn.summaries_[i]);
            matchCount++;
          }
        }
      }
      sketch_ = null;
      if (matchCount > 0) {
        sketch_ = new QuickSelectSketch<S>(matchCount, 0, summaryFactory_);
        for (int i = 0; i < matchCount; i++) sketch_.insert(matchKeys[i], matchSummaries[i]);
      }
    }
    if (sketch_ != null) {
      sketch_.setThetaLong(theta_);
      sketch_.setIsEmpty(isEmpty_);
    }
  }

  CompactSketch<S> getResult() {
    if (isFirstCall_) throw new IllegalStateException("getResult() with no intervening intersections is not a legal result.");
    if (sketch_ == null) return new CompactSketch<S>(null, null, theta_, isEmpty_);
    return sketch_.compact();
  }

  void reset() {
    isEmpty_ = false;
    theta_ = Long.MAX_VALUE;
    sketch_ = null;
    isFirstCall_ = true;
  }
}
