package com.yahoo.sketches.hllmap;

import static com.yahoo.sketches.hllmap.Util.fmtDouble;
import static com.yahoo.sketches.hllmap.Util.fmtLong;

import java.util.Arrays;

import com.yahoo.sketches.SketchesArgumentException;
import com.yahoo.sketches.hash.MurmurHash3;


//Always holds all keys.
// prime size, double hash, no deletes, 1-bit state array
// same growth algorithm as for the next levels, except no shrink. Constants may be specific.

class SingleCouponMap extends Map {
  public static final String LS = System.getProperty("line.separator");
  private static final double LOAD_FACTOR = 15.0/16.0;

  private final double entrySizeBytes_;

  private int tableEntries_;
  private int capacityEntries_;
  private int curCountEntries_;
  private float growthFactor_;

  //Arrays
  private byte[] keysArr_;
  private short[] couponsArr_;

  // state: 0: empty or valid; empty if coupon is 0, otherwise valid.
  // state: 1: original coupon has been promoted, current coupon contains a table # instead.
  private byte[] stateArr_;

  private SingleCouponMap(final int keySizeBytes, int tableEntries) {
    super(keySizeBytes);
    double byteFraction = Math.ceil(tableEntries / 8.0) / tableEntries;
    entrySizeBytes_ = keySizeBytes + Short.BYTES + byteFraction;
  }

  static SingleCouponMap getInstance(int tgtEntries, int keySizeBytes, float growthFactor) {
    Util.checkGrowthFactor(growthFactor); //optional
    Util.checkTgtEntries(tgtEntries); //optional
    int tableEntries = Util.nextPrime(tgtEntries);

    SingleCouponMap map = new SingleCouponMap(keySizeBytes, tableEntries);

    map.tableEntries_ = tableEntries;
    map.capacityEntries_ = (int)(tableEntries * LOAD_FACTOR);
    map.curCountEntries_ = 0;
    map.growthFactor_ = growthFactor;

    map.keysArr_ = new byte[tableEntries * map.keySizeBytes_];
    map.couponsArr_ = new short[tableEntries];
    map.stateArr_ = new byte[(int) Math.ceil(tableEntries / 8.0)];
    return map;
  }

  @Override
  double update(byte[] key, int coupon) {
    final int entryIndex = findOrInsertKey(key);
    if (entryIndex < 0) { // insert
      setCoupon(~entryIndex, (short) coupon, false);
      return 1.0;
    }
    int coupon2 = couponsArr_[entryIndex];
    //depends on the fact that a valid coupon can never be a small number.
    if (coupon == coupon2) {
      return 1.0;
    }
    return -couponsArr_[entryIndex]; //indicates coupon contains table #
  }

  @Override
  double getEstimate(byte[] key) {
    final int entryIndex = findKey(key);
    if (entryIndex < 0) return 0;
    if (isCoupon(entryIndex)) return 1;
    return ~couponsArr_[entryIndex]; //indicates coupon contains table #
  }

  /**
   * Returns entryIndex if the given key is found. The coupon may be valid or contain a table index.
   * If not found, returns one's complement entryIndex
   * of an empty slot for insertion, which may be over a deleted key.
   * @param key the given key
   * @return the entryIndex
   */
  int findKey(final byte[] key) {
    final long[] hash = MurmurHash3.hash(key, SEED);
    int entryIndex = getIndex(hash[0], tableEntries_);
    final int stride = getStride(hash[1], tableEntries_);
    final int loopIndex = entryIndex;

    do {
      if (couponsArr_[entryIndex] == 0) {
        return ~entryIndex; //empty
      }
      if (Map.arraysEqual(key, 0, keysArr_, entryIndex * keySizeBytes_, keySizeBytes_)) {
        return entryIndex;
      }
      entryIndex = (entryIndex + stride) % tableEntries_;
    } while (entryIndex != loopIndex);
    throw new SketchesArgumentException("Key not found and no empty slots!");
  }

  int findOrInsertKey(final byte[] key) {
    int entryIndex = findKey(key);
    if (entryIndex < 0) {
      if (curCountEntries_ + 1 > capacityEntries_) {
        resize();
        entryIndex = findKey(key);
        assert(entryIndex < 0);
      }
      //will return negative: was not found, inserted
      System.arraycopy(key, 0, keysArr_, ~entryIndex * keySizeBytes_, keySizeBytes_);
      curCountEntries_++;
    }
    return entryIndex;
  }

  boolean isCoupon(final int entryIndex) {
    return !isBitSet(stateArr_, entryIndex);
  }

  short getCoupon(final int entryIndex) {
    return couponsArr_[entryIndex];
  }

  // assumes that the state bit is never cleared
  void setCoupon(final int entryIndex, final short coupon, final boolean isLevel) {
    couponsArr_[entryIndex] = coupon;
    if (isLevel) {
      setBit(stateArr_, entryIndex);
    } else {
      clearBit(stateArr_, entryIndex);
    }
  }

  @Override
  double getEntrySizeBytes() {
    return entrySizeBytes_;
  }

  @Override
  int getTableEntries() {
    return tableEntries_;
  }

  @Override
  int getCapacityEntries() {
    return capacityEntries_;
  }

  @Override
  int getCurrentCountEntries() {
    return curCountEntries_;
  }

  @Override
  long getMemoryUsageBytes() {
    long arrays = keysArr_.length
        + (long)couponsArr_.length * Short.BYTES
        + stateArr_.length;
    long other = 4 * 4 + 8;
    return arrays + other;
  }

  @Override
  public String toString() {
    String te = fmtLong(getTableEntries());
    String ce = fmtLong(getCapacityEntries());
    String cce = fmtLong(getCurrentCountEntries());
    String esb = fmtDouble(getEntrySizeBytes());
    String mub = fmtLong(getMemoryUsageBytes());

    StringBuilder sb = new StringBuilder();
    String thisSimpleName = this.getClass().getSimpleName();
    sb.append("### ").append(thisSimpleName).append(" SUMMARY: ").append(LS);
    sb.append("    Max Coupons Per Entry     : ").append(1).append(LS);
    sb.append("    Capacity Coupons Per Entry: ").append(1).append(LS);
    sb.append("    Table Entries             : ").append(te).append(LS);
    sb.append("    Capacity Entries          : ").append(ce).append(LS);
    sb.append("    Current Count Entries     : ").append(cce).append(LS);
    sb.append("    Entry Size Bytes          : ").append(esb).append(LS);
    sb.append("    Memory Usage Bytes        : ").append(mub).append(LS);
    sb.append("### END SKETCH SUMMARY").append(LS);
    return sb.toString();
  }

  private void resize() {
    final byte[] oldKeysArr = keysArr_;
    final short[] oldCouponsArr = couponsArr_;
    final byte[] oldStateArr = stateArr_;
    final int oldTableEntries = tableEntries_;
    tableEntries_ = Util.nextPrime((int) (oldTableEntries * growthFactor_));
    capacityEntries_ = (int)(tableEntries_ * LOAD_FACTOR);
    //System.out.println("resizing from " + oldTableEntries + " to " + tableEntries_);
    keysArr_ = new byte[tableEntries_ * keySizeBytes_];
    couponsArr_ = new short[tableEntries_];
    stateArr_ = new byte[(int) Math.ceil(tableEntries_ / 8.0)];
    for (int i = 0; i < oldTableEntries; i++) {
      if (oldCouponsArr[i] != 0) {
        final byte[] key =
            Arrays.copyOfRange(oldKeysArr, i * keySizeBytes_, i * keySizeBytes_ + keySizeBytes_);

        int entryIndex = findKey(key); //should be one's complement
        if (entryIndex < 0) {
          insertEntry(~entryIndex, key, oldCouponsArr[i], isBitSet(oldStateArr, i));
        } else {
          throw new SketchesArgumentException("Key should not have existed.");
        }
      }
    }
  }

  // insert key and coupon at entryIndex. We know that the key does not exist in the table.
  private void insertEntry(final int entryIndex, final byte[] key, final int coupon,
      final boolean setStateOne) {
    System.arraycopy(key, 0, keysArr_, entryIndex * keySizeBytes_, keySizeBytes_);
    setCoupon(entryIndex, (short)coupon, setStateOne);
  }
}
