package com.yahoo.sketches.hllmap;

// prime size, double hash, with deletes, 1-bit state array
// state: 0: empty always, don't need to look at 1st coupon. Coupons could be dirty.
// state: 1: valid entry or dirty, during rebuild, look at the first coupon to tell
// state: 1: first coupon > 0 means valid entry; first coupon == 0: dirty (we set to 0 when deleted)

// rebuilding TraverseCouponMap and HashCouponMap: can grow or shrink
// keep numValid and numInvalid
// grow if numValid + numInvalid > 0.9 * capacity
// shrink if numValid < 0.5 * capacity
// new size T ~= (10/7) * numValid
// BigInteger nextPrime() can be used


//@SuppressWarnings("unused")
class CouponTraverseMap extends Map {

  CouponTraverseMap(final int keySizeBytes) {
    super(keySizeBytes);
  }

  @Override
  public double update(byte[] key, byte[] identifier) {
    return 0;
  }

  @Override
  public double getEstimate(byte[] key) {
    return 0;
  }

  @Override
  void couponUpdate(byte[] key, int coupon) {

  }

}
