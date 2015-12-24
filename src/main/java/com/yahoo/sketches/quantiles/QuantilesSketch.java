package com.yahoo.sketches.quantiles;

public abstract class QuantilesSketch {
  static final int MIN_BASE_BUF_SIZE = 4; //This is somewhat arbitrary
  @SuppressWarnings("unused")
  static final double DUMMY_VALUE = -99.0;  // just for debugging
  
  
  
  
  /**
   * Checks the validity of the split points. They must be unique, monotonically increasing and
   * not NaN.
   * @param splitPoints array
   */
  static final void validateSplitPoints(double[] splitPoints) {
    for (int j = 0; j < splitPoints.length - 1; j++) {
      if (splitPoints[j] < splitPoints[j+1]) { continue; }
      throw new IllegalArgumentException(
          "SplitPoints must be unique, monotonically increasing and not NaN.");
    }
  }

}
