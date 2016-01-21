/*
 * Copyright 2015, Yahoo! Inc.
 * Licensed under the terms of the Apache License 2.0. See LICENSE file at the project root for terms.
 */
package com.yahoo.sketches.tuple;

/**
 * Top level for compact sketches. Compact sketches are never created directly.
 * They are created as a result of the compact() method on a QuickSelectSketch
 * or the getResult() method of a set operation like Union, Intersection or AnotB.
 * Compact sketch consists of a compact list (i.e. no intervening spaces) of hash values,
 * corresponding list of double values, and a value for theta. The lists may or may
 * not be ordered. Compact sketch is read-only.
 */

public abstract class ArrayOfDoublesCompactSketch extends ArrayOfDoublesSketch {

  public static final byte serialVersionUID = 1;

  static final int RETAINED_ENTRIES_INT = 4;
  static final int THETA_LONG = 8;
  static final int ENTRIES_START = 16;

}
