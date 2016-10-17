/*
 * Copyright 2016, Yahoo! Inc. Licensed under the terms of the
 * Apache License 2.0. See LICENSE file at the project root for terms.
 */

package com.yahoo.sketches.hllmap;

public final class MapDistribution {

  // excluding the first and the last levels
  public static final int NUM_LEVELS = 8; //total of traverse + coupon map levels
  public static final int NUM_TRAVERSE_LEVELS = 3;
  public static final float HLL_RESIZE_FACTOR = 2.0F;

  public static final float BASE_GROWTH_FACTOR = 1.2F;
  public static final float INTERMEDIATE_GROWTH_FACTOR = 2.0F;
  public static final float FINAL_GROWTH_FACTOR = 2.0F;

  public static final int BASE_TGT_ENTRIES = 1000;
  public static final int INTERMEDIATE_TGT_ENTRIES = 100;
  public static final int FINAL_TGT_ENTRIES = 100;

}
