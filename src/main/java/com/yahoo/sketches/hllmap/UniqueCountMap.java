package com.yahoo.sketches.hllmap;

import com.yahoo.sketches.hash.MurmurHash3;

@SuppressWarnings("unused")
public class UniqueCountMap {

  private int targetSizeBytes_;

  // excluding the first and the last levels
  private static final int NUM_LEVELS = 8;

  // coupon is a 16-bit value similar to HLL sketch value: 10-bit address,
  // 6-bit number of leading zeroes in a 64-bit hash of the key + 1

  // prime size, double hash, no deletes, 1-bit state array
  // state: 0 - value is a coupon (if used), 1 - value is a level number
  // same growth rule as for the next levels
  private final CouponMap baseLevelMap;

  // TraverseCouponMap or HashCouponMap instances
  private final Map[] intermediateLevelMaps;

  // this map has a fixed slotSize (row size). No shrinking.
  // Similar growth algorithm to SingleCouponMap, maybe different constants.
  // needs to keep 2 double values and 1 float value for HIP estimator
  private HllMap lastLevelMap;

  public UniqueCountMap(final int targetSizeBytes, final int keySizeBytes) {
    // to do: figure out how to distribute that size between the levels
    targetSizeBytes_ = targetSizeBytes;
    baseLevelMap = new CouponMap(targetSizeBytes, keySizeBytes);
    intermediateLevelMaps = new Map[NUM_LEVELS];
  }

  // This class will decide the transition points of when to promote between types of maps.
  public double update(final byte[] key, final byte[] value) {
    if (key == null) return Double.NaN;
    if (value == null) return getEstimate(key);
    final long[] valueHash = MurmurHash3.hash(value, Map.SEED);
    short coupon = computeCoupon(valueHash);

    final int baseLevelIndex = baseLevelMap.findOrInsert(key);
    if (baseLevelIndex < 0) {
      baseLevelMap.setValue(~baseLevelIndex, coupon, true);
      return 1;
    }
    final short baseLevelMapValue = baseLevelMap.getValue(baseLevelIndex);
    if (baseLevelMap.isCoupon(baseLevelIndex)) {
      if (baseLevelMapValue == coupon) return 1;
      // promote from the base level
      baseLevelMap.setValue(baseLevelIndex, (short) 1, false);
      if (intermediateLevelMaps[0] == null) intermediateLevelMaps[0] = new CouponTraverseMap(baseLevelMap.getKeySizeBytes(), 2);
      intermediateLevelMaps[0].update(key, baseLevelMapValue);
      intermediateLevelMaps[0].update(key, coupon);
      return 2;
    }

    int currentLevel = baseLevelMapValue;
    while (currentLevel <= NUM_LEVELS) {
      final Map map = intermediateLevelMaps[currentLevel - 1];
      final double numValues = map.update(key, coupon);
      if (numValues > 0) return numValues;
      // promote to the next level
      currentLevel++;
      baseLevelMap.setValue(baseLevelIndex, (short) currentLevel, false);
      if (currentLevel > NUM_LEVELS) break;
      baseLevelMap.setValue(baseLevelIndex, (short) currentLevel, false);
      if (intermediateLevelMaps[currentLevel - 1] == null) intermediateLevelMaps[currentLevel - 1] = new CouponTraverseMap(baseLevelMap.getKeySizeBytes(), 1 << currentLevel);
      final Map newMap = intermediateLevelMaps[currentLevel - 1];
      final MapValuesIterator it = map.getValuesIterator(key);
      while (it.next()) {
        newMap.update(key, it.getValue());
      }
    }
    //if (lastLevelMap == null) lastLevelMap = HllMap.getInstance(targetSizeBytes_, baseLevelMap.getKeySizeBytes(), 512, 2f);
    return lastLevelMap.update(key, coupon);
  }

  public double getEstimate(final byte[] key) {
    final int index = baseLevelMap.find(key);
    if (index < 0) return 0;
    if (baseLevelMap.isCoupon(index)) return 1;
    final short level = baseLevelMap.getValue(index);
    if (level <= NUM_LEVELS) {
      final Map map = intermediateLevelMaps[level - 1];
      return map.getEstimate(key);
    }
    return lastLevelMap.getEstimate(key);
  }

  static final int ADDRESS_SIZE_BITS = 10;
  static final int ADDRESS_MASK = (1 << ADDRESS_SIZE_BITS) - 1;

  static short computeCoupon(final long[] hash) {
    byte value = (byte) (Long.numberOfLeadingZeros(hash[1]) + 1);
    int address = (int) (hash[0] & ADDRESS_MASK);
    return (short) ((value << ADDRESS_SIZE_BITS) | address);
  }

  static double hipEstimate(final int numberOfCoupons) {
    final int value3L = 3 * (1 << ADDRESS_SIZE_BITS);
    return value3L * Math.log((double) value3L / (value3L - numberOfCoupons));
  }

}
