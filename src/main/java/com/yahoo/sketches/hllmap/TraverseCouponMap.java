package com.yahoo.sketches.hllmap;

// prime size, double hash, potentially with deletes, 1-bit state array
// state: 0 - empty, 1 - valid entry or dirty, look at the first coupon to tell
// first coupon > 0 means valid entry, otherwise dirty
class TraverseCouponMap extends CouponMap {

  TraverseCouponMap(final int keySizeBytes) {
  }

}
