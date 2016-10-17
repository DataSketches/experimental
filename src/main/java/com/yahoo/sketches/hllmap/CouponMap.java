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
  }

  abstract int findKey(byte[] key);
  abstract int findOrInsertKey(byte[] key);
  abstract void deleteKey(int index);
  abstract void deleteKey(byte[] key);

  abstract double findOrInsertCoupon(int index, short coupon);
  abstract int getCouponCount(int index);
  abstract CouponsIterator getCouponsIterator(byte[] key);

}
