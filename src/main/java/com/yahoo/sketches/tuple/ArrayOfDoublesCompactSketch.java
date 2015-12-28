/*
 * Copyright 2015, Yahoo! Inc.
 * Licensed under the terms of the Apache License 2.0. See LICENSE file at the project root for terms.
 */
package com.yahoo.sketches.tuple;

public abstract class ArrayOfDoublesCompactSketch extends ArrayOfDoublesSketch {

  public static final byte serialVersionUID = 1;

  static final int SERIAL_VERSION_BYTE = 0;
  static final int SKETCH_TYPE_BYTE = 1;
  static final int FLAGS_BYTE = 2;
  static final int NUM_VALUES_BYTE = 3;
  static final int RETAINED_ENTRIES_INT = 4;
  static final int THETA_LONG = 8;
  static final int ENTRIES_START = 16;

}
