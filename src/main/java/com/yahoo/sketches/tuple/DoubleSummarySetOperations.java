/*
 * Copyright 2015, Yahoo! Inc.
 * Licensed under the terms of the Apache License 2.0. See LICENSE file at the project root for terms.
 */
package com.yahoo.sketches.tuple;

import com.yahoo.sketches.tuple.DoubleSummary.Mode;

/**
 * Methods for producing unions and intersections of two DoubleSummary objects.
 */
public class DoubleSummarySetOperations implements SummarySetOperations<DoubleSummary> {

  private Mode summaryMode_;

  /**
   * Creates an instance given a DoubleSummary update mode.
   * @param summaryMode DoubleSummary update mode.
   */
  public DoubleSummarySetOperations(Mode summaryMode) {
    summaryMode_ = summaryMode;
  }

  /**
   * Method of producing a union of two DoubleSummary objects.
   * @param a DoubleSummary A.
   * @param b DoubleSummary B.
   * @return Union of A and B.
   */
  @Override
  public DoubleSummary union(DoubleSummary a, DoubleSummary b) {
    DoubleSummary result = new DoubleSummary(summaryMode_); 
    if (a != null) result.update(a.getValue());
    if (b != null) result.update(b.getValue());
    return result;
  }

  /**
   * Method of producing an intersection of two DoubleSummary objects.
   * @param a DoubleSummary A.
   * @param b DoubleSummary B.
   * @return Intersection of A and B.
   */
  @Override
  public DoubleSummary intersection(DoubleSummary a, DoubleSummary b) {
    return union(a, b);
  }
}
