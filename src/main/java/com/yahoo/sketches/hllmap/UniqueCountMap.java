package com.yahoo.sketches.hllmap;

import static com.yahoo.sketches.hllmap.MapDistribution.BASE_GROWTH_FACTOR;
import static com.yahoo.sketches.hllmap.MapDistribution.BASE_TGT_ENTRIES;
import static com.yahoo.sketches.hllmap.MapDistribution.HLL_RESIZE_FACTOR;
import static com.yahoo.sketches.hllmap.MapDistribution.NUM_LEVELS;
import static com.yahoo.sketches.hllmap.MapDistribution.NUM_TRAVERSE_LEVELS;
import static com.yahoo.sketches.hllmap.Util.fmtLong;

public class UniqueCountMap {
  public static final String LS = System.getProperty("line.separator");
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
  public UniqueCountMap(final int keySizeBytes, final int k) {
    Util.checkK(k);
    Util.checkKeySizeBytes(keySizeBytes);
    k_ = k;
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
    short coupon = (short) Map.coupon16(identifier);

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
        intermediateLevelMaps[0] = CouponTraverseMap.getInstance(keySizeBytes_, 2);
      }
      intermediateLevelMaps[0].update(key, baseLevelMapCoupon);
      intermediateLevelMaps[0].update(key, coupon);
      return 2;
    }

    int level = baseLevelMapCoupon;
    if (level <= NUM_LEVELS) {
      final CouponMap map = intermediateLevelMaps[level - 1];
      final int index = map.findOrInsertKey(key);
      final double estimate = map.findOrInsertCoupon(index, coupon);
      if (estimate > 0) return estimate;
      // promote to the next level
      level++;
      baseLevelMap.setCoupon(baseLevelIndex, (short) level, true); //very dangerous; state = 1
      final int newLevelCapacity = 1 << level;
      if (level <= NUM_LEVELS) {
        //System.out.println("promoting to level " + level + " with capacity of " + newLevelCapacity);
        if (intermediateLevelMaps[level - 1] == null) {
          if (level <= NUM_TRAVERSE_LEVELS) {
            intermediateLevelMaps[level - 1] = CouponTraverseMap.getInstance(keySizeBytes_, newLevelCapacity);
          } else {
            intermediateLevelMaps[level - 1] = CouponHashMap.getInstance(keySizeBytes_, newLevelCapacity);
          }
        }
        final CouponMap newMap = intermediateLevelMaps[level - 1];
        final CouponsIterator it = map.getCouponsIterator(key);
        final int newMapIndex = newMap.findOrInsertKey(key);
        //int num = 0;
        while (it.next()) {
          final double est = newMap.findOrInsertCoupon(newMapIndex, it.getValue());
          assert(est > 0);
          //num++;
        }
        newMap.updateEstimate(newMapIndex, -estimate);
        map.deleteKey(index);
        //System.out.println("promoted coupons: " + num + ", estimate: " + -estimate);
        final double newEstimate = newMap.findOrInsertCoupon(newMapIndex, coupon);
        assert(newEstimate > 0);  // this must be positive since we have just promoted
        return newEstimate;
      } else { // promoting to the last level
        //System.out.println("promoting to the last level");
        if (lastLevelMap == null) {
          lastLevelMap = HllMap.getInstance(100, keySizeBytes_, k_, HLL_RESIZE_FACTOR);
        }
        final CouponsIterator it = map.getCouponsIterator(key);
        final int lastLevelIndex = lastLevelMap.findOrInsertKey(key);
        //int num = 0;
        while (it.next()) {
          lastLevelMap.findOrInsertCoupon(lastLevelIndex, it.getValue());
          //num++;
        }
        //System.out.println("promoted coupons: " + num + ", estimate: " + -estimate);
        lastLevelMap.updateEstimate(lastLevelIndex, -estimate);
        map.deleteKey(index);
        final double newEstimate = lastLevelMap.findOrInsertCoupon(lastLevelIndex, coupon);
        assert(newEstimate > 0);  // this must be positive since we have just promoted
        return newEstimate;
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

  public long getMemoryUsageBytes() {
    long total = baseLevelMap.getMemoryUsageBytes();
    for (int i = 0; i < intermediateLevelMaps.length; i++) {
      if (intermediateLevelMaps[i] != null) {
        total += intermediateLevelMaps[i].getMemoryUsageBytes();
      }
    }
    if (lastLevelMap != null) {
      total += lastLevelMap.getMemoryUsageBytes();
    }
    return total;
  }

  public int getActiveLevels() {
    int levels = 1;
    int iMapsLen = intermediateLevelMaps.length;
    for (int i = 0; i < iMapsLen; i++) {
      if (intermediateLevelMaps[i] != null) levels++;
    }
    if (lastLevelMap != null) levels++;
    return levels;
  }

  @Override
  public String toString() {
    String ksb = fmtLong(keySizeBytes_);
    String hllk = fmtLong(k_);
    String lvls  = fmtLong(getActiveLevels());
    String mub = fmtLong(getMemoryUsageBytes());

    StringBuilder sb = new StringBuilder();
    String thisSimpleName = this.getClass().getSimpleName();
    sb.append("## ").append(thisSimpleName).append(" SUMMARY: ").append(LS);
    sb.append("    Key Size Bytes            : ").append(ksb).append(LS);
    sb.append("    HLL k                     : ").append(hllk).append(LS);
    sb.append("    Active Levels             : ").append(lvls).append(LS);
    sb.append("    Memory Usage Bytes        : ").append(mub).append(LS);
    sb.append(LS);
    sb.append(baseLevelMap.toString());
    sb.append(LS);
    for (int i = 0; i < intermediateLevelMaps.length; i++) {
      CouponMap cMap = intermediateLevelMaps[i];
      if (cMap != null) {
        sb.append(cMap.toString());
        sb.append(LS);
      }
    }
    if (lastLevelMap != null) {
      sb.append(lastLevelMap.toString());
      sb.append(LS);
    }
    sb.append("## ").append("END SKETCH SUMMARY");
    return sb.toString();
  }
}
