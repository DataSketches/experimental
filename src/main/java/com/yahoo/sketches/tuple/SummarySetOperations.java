/*
 * Copyright 2015, Yahoo! Inc.
 * Licensed under the terms of the Apache License 2.0. See LICENSE file at the project root for terms.
 */
package com.yahoo.sketches.tuple;

public interface SummarySetOperations<S extends Summary> {
  public S union(S a, S b);
  public S intersection(S a, S b);
}
