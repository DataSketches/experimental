package com.yahoo.sketches.hllmap;

import static com.yahoo.sketches.hllmap.MapDistribution.BASE_GROWTH_FACTOR;
import static com.yahoo.sketches.hllmap.MapDistribution.BASE_TGT_ENTRIES;
import static com.yahoo.sketches.hllmap.MapDistribution.HLL_RESIZE_FACTOR;
import static com.yahoo.sketches.hllmap.MapDistribution.NUM_LEVELS;
import static com.yahoo.sketches.hllmap.MapDistribution.NUM_TRAVERSE_LEVELS;

@SuppressWarnings("unused")
public class UniqueCountMap {

  private final int targetSizeBytes_;
  private final int keySizeBytes_;
  private final int k_;

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

  //@param keySizeBytes must be at least 4 bytes.
  public UniqueCountMap(final int targetSizeBytes, final int keySizeBytes, final int k) {
    Util.checkK(k);
    Util.checkKeySizeBytes(keySizeBytes);
    k_ = k;
    //TODO: figure out how to distribute that size between the levels
    targetSizeBytes_ = targetSizeBytes;
    keySizeBytes_ = keySizeBytes;
    baseLevelMap = SingleCouponMap.getInstance(BASE_TGT_ENTRIES, keySizeBytes, BASE_GROWTH_FACTOR);
    intermediateLevelMaps = new CouponMap[NUM_LEVELS];
  }

  // This class will decide the transition points of when to promote between types of maps.
  public double update(final byte[] key, final byte[] identifier) {
    if (key == null) return Double.NaN;
    if (key.length != keySizeBytes_) {
      throw new IllegalArgumentException("Key must be " + keySizeBytes_ + " bytes long");
    }
    if (identifier == null) return getEstimate(key);
    short coupon = (short) Map.coupon16(identifier, k_);

    final int baseLevelIndex = baseLevelMap.findOrInsertKey(key);
    if (baseLevelIndex < 0) {
      //this is a new key for the baseLevelMap. Set the coupon, keep the state bit clear.
      baseLevelMap.setCoupon(~baseLevelIndex, coupon, false);
      return 1;
    }
    final short baseLevelMapCoupon = baseLevelMap.getCoupon(baseLevelIndex);
    if (baseLevelMap.isCoupon(baseLevelIndex)) {
      if (baseLevelMapCoupon == coupon) return 1; //duplicate
      // promote from the base level
      baseLevelMap.setCoupon(baseLevelIndex, (short) 1, true); //set coupon = Level 1; state = 1
      if (intermediateLevelMaps[0] == null) {
        intermediateLevelMaps[0] = new CouponTraverseMap(keySizeBytes_, 2);
      }
      intermediateLevelMaps[0].update(key, baseLevelMapCoupon);
      intermediateLevelMaps[0].update(key, coupon);
      return 2;
    }

    int level = baseLevelMapCoupon;
    while (level <= NUM_LEVELS) {
      final CouponMap map = intermediateLevelMaps[level - 1];
      final int index = map.findOrInsertKey(key);
      final double numValues = map.findOrInsertCoupon(index, coupon);
      if (numValues > 0) return numValues;
      // promote to the next level
      level++;
      baseLevelMap.setCoupon(baseLevelIndex, (short) level, true); //very dangerous; state = 1
      final int newLevelCapacity = 1 << level;
      if (level <= NUM_LEVELS) {
        if (intermediateLevelMaps[level - 1] == null) {
          if (level <= NUM_TRAVERSE_LEVELS) {
            intermediateLevelMaps[level - 1] = new CouponTraverseMap(keySizeBytes_, newLevelCapacity);
          } else {
            intermediateLevelMaps[level - 1] = CouponHashMap.getInstance(13, keySizeBytes_, newLevelCapacity, k_, 2F);
          }
        }
        final Map newMap = intermediateLevelMaps[level - 1];
        final CouponsIterator it = map.getCouponsIterator(key);
        while (it.next()) {
          newMap.update(key, it.getValue());
        }
        double hipEst = map.getEstimate(key); //get the HIP estimate
        map.deleteKey(index);
        double est = newMap.update(key, coupon);
        return est;
      } else { // promoting to the last level
        if (lastLevelMap == null) {
          lastLevelMap = HllMap.getInstance(100, keySizeBytes_, k_, HLL_RESIZE_FACTOR);
        }
        final CouponsIterator it = map.getCouponsIterator(key);
        while (it.next()) {
          lastLevelMap.update(key, it.getValue());
        }
        map.deleteKey(index);
        lastLevelMap.updateEstimate(key, -numValues);
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
    final short level = baseLevelMap.getCoupon(index);
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
