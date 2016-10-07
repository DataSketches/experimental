package com.yahoo.sketches.hllmap;

public class UniqueCountMap {

  // coupon is a 16-bit value similar to HLL sketch value: 10-bit address,
  // 6-bit number of leading zeroes in a 64-bit hash of the key + 1

  // prime size, double hash, no deletes, 1-bit state array
  // state: 0 - value is a coupon (if used), 1 - value is a level number
  // same growth rule as for the next levels
  private final SingleCouponMap baseLevelMap;

  // TraverseCouponMap or HashCouponMap instances
  private CouponMap[] intermediateLevelMaps;

  // rebuilding TraverseCouponMap and HashCouponMap: can grow or shrink
  // keep numValid and numInvalid
  // grow if numValid + numInvalid > 0.9 * capacity
  // shrink if numValid < 0.5 * capacity
  // new size T ~= (10/7) * numValid
  // BigInteger nextPrime() can be used

  // this map has a fixed size
  // needs to keep 2 double values and 1 float value for HIP estimator
  private HllMap lastLevelMap;
  
  public UniqueCountMap(final int keySizeBytes) {
    baseLevelMap = new SingleCouponMap(keySizeBytes);
  }

  public double update(final byte[] key, final byte[] value) {
    return 0;
  }

  static final int ADDRESS_SIZE_BITS = 10;
  static final int ADDRESS_MASK = (1 << ADDRESS_SIZE_BITS) - 1;

  static short computeCoupon(final long[] hash) {
    byte value = (byte) (Long.numberOfLeadingZeros(hash[1]) + 1);
    int index = (int) (hash[0] & ADDRESS_MASK);
    return (short) ((index << ADDRESS_SIZE_BITS) | value);
  }

}
