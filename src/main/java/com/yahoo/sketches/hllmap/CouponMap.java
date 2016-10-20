/*
 * Copyright 2016, Yahoo! Inc. Licensed under the terms of the
 * Apache License 2.0. See LICENSE file at the project root for terms.
 */

package com.yahoo.sketches.hllmap;

import static com.yahoo.sketches.hllmap.Util.fmtDouble;
import static com.yahoo.sketches.hllmap.Util.fmtLong;

abstract class CouponMap extends Map {
  private static final String LS = System.getProperty("line.separator");
  static final int MIN_NUM_ENTRIES = 157;
  static final double SHRINK_TRIGGER_FACTOR = 0.5;
  static final double GROW_TRIGGER_FACTOR = 15.0 / 16.0;
  static final double TARGET_FILL_FACTOR = 2.0 / 3.0;

  /**
   * @param keySizeBytes
   */
  CouponMap(int keySizeBytes) {
    super(keySizeBytes);
  }

  abstract int findKey(byte[] key);
  abstract int findOrInsertKey(byte[] key);
  abstract void deleteKey(int index);
  abstract void updateEstimate(int index, double estimate);

  abstract double findOrInsertCoupon(int index, short coupon);
  abstract int getCouponCount(int index);
  abstract CouponsIterator getCouponsIterator(byte[] key);

  abstract int getMaxCouponsPerEntry();
  abstract int getCapacityCouponsPerEntry();
  abstract int getActiveEntries();
  abstract int getDeletedEntries();

  @Override
  public String toString() {
    String mcpe = fmtLong(getMaxCouponsPerEntry());
    String ccpe = fmtLong(getCapacityCouponsPerEntry());
    String te = fmtLong(getTableEntries());
    String ce = fmtLong(getCapacityEntries());
    String cce = fmtLong(getCurrentCountEntries());
    String ae = fmtLong(getActiveEntries());
    String de = fmtLong(getDeletedEntries());
    String esb = fmtDouble(getEntrySizeBytes());
    String mub = fmtLong(getMemoryUsageBytes());

    StringBuilder sb = new StringBuilder();
    String thisSimpleName = this.getClass().getSimpleName();
    sb.append("### ").append(thisSimpleName).append(" SUMMARY: ").append(LS);
    sb.append("    Max Coupons Per Entry     : ").append(mcpe).append(LS);
    sb.append("    Capacity Coupons Per Entry: ").append(ccpe).append(LS);
    sb.append("    Table Entries             : ").append(te).append(LS);
    sb.append("    Capacity Entries          : ").append(ce).append(LS);
    sb.append("    Current Count Entries     : ").append(cce).append(LS);
    sb.append("      Active Entries          : ").append(ae).append(LS);
    sb.append("      Deleted Entries         : ").append(de).append(LS);
    sb.append("    Entry Size Bytes          : ").append(esb).append(LS);
    sb.append("    Memory Usage Bytes        : ").append(mub).append(LS);
    sb.append("### END SKETCH SUMMARY").append(LS);
    return sb.toString();
  }

}
