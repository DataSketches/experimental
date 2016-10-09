package com.yahoo.sketches.hllmap;

@SuppressWarnings("unused")
public class UniqueCountMap {

  // coupon is a 16-bit value similar to HLL sketch value: 10-bit address,
  // 6-bit number of leading zeroes in a 64-bit hash of the key + 1

  // prime size, double hash, no deletes, 1-bit state array
  // state: 0 - value is a coupon (if used), 1 - value is a level number
  // same growth rule as for the next levels
  private final CouponMap baseLevelMap;

  // TraverseCouponMap or HashCouponMap instances
  private Map[] intermediateLevelMaps;

  // this map has a fixed slotSize (row size). No shrinking.
  // Similar growth algorithm to SingleCouponMap, maybe different constants.
  // needs to keep 2 double values and 1 float value for HIP estimator
  private HllMap lastLevelMap;

  public UniqueCountMap(final int sizeBytes, final int keySizeBytes) {
    // to do: figure out how to distribute that size between the levels
    baseLevelMap = new CouponMap(sizeBytes, keySizeBytes);
  }

  public double update(final byte[] key, final byte[] value) {
   // This class will decide the transition points of when to promote between types of maps.

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
