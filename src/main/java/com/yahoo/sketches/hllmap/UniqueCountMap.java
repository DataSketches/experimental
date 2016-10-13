package com.yahoo.sketches.hllmap;

@SuppressWarnings("unused")
public class UniqueCountMap {

  private final int targetSizeBytes_;
  private final int keySizeBytes_;

  // excluding the first and the last levels
  private static final int NUM_LEVELS = 8;
  private static final int NUM_TRAVERSE_LEVELS = 3;
  private static final int HLL_K = 1024;

  // coupon is a 16-bit value similar to HLL sketch value: 10-bit address,
  // 6-bit number of leading zeroes in a 64-bit hash of the key + 1

  // prime size, double hash, no deletes, 1-bit state array
  // state: 0 - value is a coupon (if used), 1 - value is a level number
  // same growth rule as for the next levels
  private final SingleCouponMap baseLevelMap;

  // TraverseCouponMap or HashCouponMap instances
  private final CouponMap[] intermediateLevelMaps;

  // this map has a fixed slotSize (row size). No shrinking.
  // Similar growth algorithm to SingleCouponMap, maybe different constants.
  // needs to keep 2 double values and 1 float value for HIP estimator
  private HllMap lastLevelMap;

  public UniqueCountMap(final int targetSizeBytes, final int keySizeBytes) {
    // to do: figure out how to distribute that size between the levels
    targetSizeBytes_ = targetSizeBytes;
    keySizeBytes_ = keySizeBytes;
    baseLevelMap = new SingleCouponMap(targetSizeBytes, keySizeBytes);
    intermediateLevelMaps = new CouponMap[NUM_LEVELS];
  }

  // This class will decide the transition points of when to promote between types of maps.
  public double update(final byte[] key, final byte[] identifier) {
    if (key == null) return Double.NaN;
    if (key.length != keySizeBytes_) throw new IllegalArgumentException("Key must be " + keySizeBytes_ + " bytes long");
    if (identifier == null) return getEstimate(key);
    short coupon = (short) Util.coupon16(identifier, HLL_K);

    final int baseLevelIndex = baseLevelMap.findOrInsertKey(key);
    if (baseLevelIndex < 0) {
      baseLevelMap.setValue(~baseLevelIndex, coupon, true);
      return 1;
    }
    final short baseLevelMapValue = baseLevelMap.getValue(baseLevelIndex);
    if (baseLevelMap.isCoupon(baseLevelIndex)) {
      if (baseLevelMapValue == coupon) return 1;
      // promote from the base level
      baseLevelMap.setValue(baseLevelIndex, (short) 1, false);
      if (intermediateLevelMaps[0] == null) intermediateLevelMaps[0] = new CouponTraverseMap(keySizeBytes_, 2);
      intermediateLevelMaps[0].update(key, baseLevelMapValue);
      intermediateLevelMaps[0].update(key, coupon);
      return 2;
    }

    int level = baseLevelMapValue;
    while (level <= NUM_LEVELS) {
      final CouponMap map = intermediateLevelMaps[level - 1];
      final double numValues = map.update(key, coupon);
      if (numValues > 0) return numValues;
      // promote to the next level
      level++;
      baseLevelMap.setValue(baseLevelIndex, (short) level, false);
      final int newLevelCapacity = 1 << level;
      if (level <= NUM_LEVELS) {
        if (intermediateLevelMaps[level - 1] == null) {
          if (level <= NUM_TRAVERSE_LEVELS) {
            
            intermediateLevelMaps[level - 1] = new CouponTraverseMap(keySizeBytes_, newLevelCapacity);
          } else {
            intermediateLevelMaps[level - 1] = new CouponHashMap(keySizeBytes_, newLevelCapacity);
          }
        }
        final Map newMap = intermediateLevelMaps[level - 1];
        final MapValuesIterator it = map.getValuesIterator(key);
        int num = 0;
        while (it.next()) {
          newMap.update(key, it.getValue());
          num++;
        }
        return newMap.update(key, coupon);
      } else { // promoting to the last level
        if (lastLevelMap == null) {
          lastLevelMap = HllMap.getInstance(100, keySizeBytes_, HLL_K, 2f);
        }
        final MapValuesIterator it = map.getValuesIterator(key);
        int num = 0;
        while (it.next()) {
          lastLevelMap.update(key, it.getValue());
          num++;
        }
        //System.out.println("estimate: " + map.getEstimate(key));
        lastLevelMap.updateEstimate(key, map.getEstimate(key));
      }
    }
    return lastLevelMap.update(key, coupon);
  }

  public double getEstimate(final byte[] key) {
    if (key == null) return Double.NaN;
    if (key.length != keySizeBytes_) throw new IllegalArgumentException("Key must be " + keySizeBytes_ + " bytes long");
    final int index = baseLevelMap.findKey(key);
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

  static double hipEstimate(final int numberOfCoupons) {
    final int value3L = 3 * (1 << ADDRESS_SIZE_BITS);
    return value3L * Math.log((double) value3L / (value3L - numberOfCoupons));
  }

}
