package com.yahoo.sketches.hllmap;

public class UniqueCountMap {

  // coupon is a 16-bit value similar to HLL sketch value: 10-bit address,
  // 6-bit number of leading zeroes in a 64-bit hash of the key

  // prime size, double hash, no deletes, same growth rule as for the next levels
  private final SingleCouponMap baseMap;

  // prime size, double hash, potentially with deletes, 1-bit state array
  // state: 0 - empty, 1 - valid entry or dirty, look at the first coupon to tell
  // first coupon > 0 means valid entry, otherwise dirty
  private TraverseCouponMap[] traverseMaps;

  // prime size, double hash, potentially with deletes, 1-byte count per key, 255 is marker for "dirty"
  private HashCouponMap[] hashMaps;

  // rebuilding TraverseCouponMap and HashCouponMap: can grow or shrink
  // keep numValid and numInvalid
  // grow if numValid + numInvalid > 0.9 * capacity
  // shrink if numValid < 0.5 * capacity
  // new size T ~= (10/7) * numValid
  // BigInteger nextPrime() can be used

  // this map has a fixed size
  // needs to keep 2 double values and 1 float value for HIP estimator
  private HllMap hllMap;
  
  public UniqueCountMap(int keySizeBytes) {
    baseMap = new SingleCouponMap(keySizeBytes);
  }

  public double update(byte[] key, byte[] value) {
    return 0;
  }

}
