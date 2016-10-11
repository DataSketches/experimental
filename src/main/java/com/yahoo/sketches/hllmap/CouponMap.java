/*
 * Copyright 2016, Yahoo! Inc. Licensed under the terms of the
 * Apache License 2.0. See LICENSE file at the project root for terms.
 */

package com.yahoo.sketches.hllmap;

public abstract class CouponMap extends Map {

  /**
   * @param keySizeBytes
   */
  CouponMap(int keySizeBytes) {
    super(keySizeBytes);
    // TODO Auto-generated constructor stub
  }

  @Override
  double update(byte[] key, int coupon) {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  double getEstimate(byte[] key) {
    // TODO Auto-generated method stub
    return 0;
  }

  abstract MapValuesIterator getValuesIterator(byte[] key);

}
