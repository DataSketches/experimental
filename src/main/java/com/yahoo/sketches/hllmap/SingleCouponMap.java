package com.yahoo.sketches.hllmap;

import java.util.Arrays;

import com.yahoo.sketches.SketchesArgumentException;
import com.yahoo.sketches.hash.MurmurHash3;


//Always holds all keys.
// prime size, double hash, no deletes, 1-bit state array
// same growth algorithm as for the next levels, except no shrink. Constants may be specific.

class SingleCouponMap extends Map {
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

  @Override //must be an actual coupon
  double update(byte[] key, int coupon) {
    int entryIndex = findKey(key);
    if (entryIndex < 0) { //empty case
      setKey(~entryIndex, key);
      setCoupon(~entryIndex, (short) coupon, false);
      curCountEntries_++;
      if (curCountEntries_ > capacityEntries_) {
        growSize();
      }
      return 1.0;
    }

    int coupon2 = couponsArr_[entryIndex];
    //depends on the fact that a valid coupon can never be a small number.
    if (coupon == coupon2) {
      return 1.0;
    }
    return -couponsArr_[entryIndex]; //indicates coupon contains table # //TODO
  }

  @Override
  double getEstimate(byte[] key) {
    final int entryIndex = findKey(key);
    if (entryIndex < 0) return 0;
    if (isCoupon(entryIndex)) return 1;
    return ~couponsArr_[entryIndex]; //indicates coupon contains table # //TODO
  }

  // Returns index if the key is found.
  // If entry is empty, returns the one's complement index
  // The coupon may be valid or contain a table index.
  int findKey(final byte[] key) {
    final long[] hash = MurmurHash3.hash(key, SEED);
    int entryIndex = getIndex(hash[0], tableEntries_);
    final int stride = getStride(hash[1], tableEntries_);
    final int loopIndex = entryIndex;

    do {
      if (couponsArr_[entryIndex] == 0) {
        return ~entryIndex; //empty
      }
      if (keyEquals(key, entryIndex)) {
        return entryIndex;
      }
      entryIndex = (entryIndex + stride) % tableEntries_;
    } while (entryIndex != loopIndex);
    throw new SketchesArgumentException("Key not found and no empty slots!");
  }

  int findOrInsertKey(final byte[] key) {
    int entryIndex = findKey(key);
    if (entryIndex < 0) {
      //will return negative: was not found, inserted
      setKey(~entryIndex, key);
    }
    curCountEntries_++;
    if (curCountEntries_ > capacityEntries_) {
      growSize();
      entryIndex = findKey(key);
    }
    return entryIndex;
  }

  // insert key and coupon at entryIndex.  We know that the key does not exist in the table.
  void insertEntry(int entryIndex, final byte[] key, int coupon, boolean setStateOne) {
    setKey(entryIndex, key);
    setCoupon(entryIndex, (short)coupon, setStateOne);
  }

  private void growSize() {
    final byte[] oldKeysArr = keysArr_;
    final short[] oldCouponsArr = couponsArr_;
    final byte[] oldStateArr = stateArr_;
    final int oldTableEntries = tableEntries_;
    tableEntries_ = Util.nextPrime((int) (oldTableEntries * growthFactor_));
    capacityEntries_ = (int)(tableEntries_ * LOAD_FACTOR);

    keysArr_ = new byte[tableEntries_ * keySizeBytes_];
    couponsArr_ = new short[tableEntries_];
    stateArr_ = new byte[(int) Math.ceil(tableEntries_ / 8.0)];
    for (int i = 0; i < oldTableEntries; i++) {
      if (oldCouponsArr[i] != 0) {
        final byte[] key =
            Arrays.copyOfRange(oldKeysArr, i * keySizeBytes_, i * keySizeBytes_ + keySizeBytes_);

        int entryIndex = findKey(key); //should be one's complement
        if (entryIndex < 0) {
          insertEntry(~entryIndex, key, oldCouponsArr[i], getBit(oldStateArr, i));
        } else {
          throw new SketchesArgumentException("Key should not have existed.");
        }
      }
    }
  }

  //TODO use System.arraycopy instead
  private void setKey(final int entryIndex, final byte[] key) {
    final int offset = entryIndex * keySizeBytes_;
    for (int i = 0; i < keySizeBytes_; i++) {
      keysArr_[offset + i] = key[i];
    }
  }

  private boolean keyEquals(final byte[] key, final int entryIndex) {
    final int offset = entryIndex * keySizeBytes_;
    for (int i = 0; i < keySizeBytes_; i++) {
      if (keysArr_[offset + i] != key[i]) return false;
    }
    return true;
  }

  boolean isCoupon(final int entryIndex) {
    return !getBit(stateArr_, entryIndex);
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
  public double getEntrySizeBytes() {
    return entrySizeBytes_;
  }

  @Override
  public int getTableEntries() {
    return tableEntries_;
  }

  @Override
  public int getCapacityEntries() {
    return capacityEntries_;
  }

  @Override
  public int getCurrentCountEntries() {
    return curCountEntries_;
  }

  @Override
  public int getMemoryUsageBytes() {
    int arrays =(int) Math.ceil(entrySizeBytes_ * tableEntries_);
    int other = 4 * 4 + 8;
    return arrays + other;
  }

}
