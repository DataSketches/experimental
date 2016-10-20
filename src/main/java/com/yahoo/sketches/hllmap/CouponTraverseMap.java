package com.yahoo.sketches.hllmap;

import java.util.Arrays;

import com.yahoo.sketches.hash.MurmurHash3;

// prime size, double hash, with deletes, 1-bit state array
// state: 0: empty always, don't need to look at 1st coupon. Coupons could be dirty.
// state: 1: valid entry or dirty, during rebuild, look at the first coupon to tell
// state: 1: first coupon > 0 means valid entry; first coupon == 0: dirty (we set to 0 when deleted)

// rebuilding TraverseCouponMap and HashCouponMap: can grow or shrink
// keep numValid and numInvalid
// grow if numValid + numInvalid > 0.9 * capacity
// shrink if numValid < 0.5 * capacity
// new size T ~= (10/7) * numValid
// BigInteger nextPrime() can be used

class CouponTraverseMap extends CouponMap {

  private final int maxCouponsPerKey_;
  private final double entrySizeBytes_;

  private int tableEntries_;
  private int capacityEntries_;
  private int numActiveKeys_;
  private int numDeletedKeys_;

  private byte[] keysArr_;
  private short[] couponsArr_;
  private byte[] stateArr_;

  private CouponTraverseMap(final int keySizeBytes, final int maxCouponsPerKey) {
    super(keySizeBytes);
    maxCouponsPerKey_ = maxCouponsPerKey;
    double byteFraction = Math.ceil(MIN_NUM_ENTRIES / 8.0) / MIN_NUM_ENTRIES;
    entrySizeBytes_ = keySizeBytes + maxCouponsPerKey * 2 + byteFraction;
  }

  public static CouponTraverseMap getInstance(final int keySizeBytes, final int maxCouponsPerKey) {
    CouponTraverseMap map = new CouponTraverseMap(keySizeBytes, maxCouponsPerKey);
    map.tableEntries_ = MIN_NUM_ENTRIES;
    map.keysArr_ = new byte[MIN_NUM_ENTRIES * keySizeBytes];
    map.couponsArr_ = new short[MIN_NUM_ENTRIES * maxCouponsPerKey];
    map.stateArr_ = new byte[(int) Math.ceil(MIN_NUM_ENTRIES / 8.0)];
    map.capacityEntries_ = (int)(map.tableEntries_ * GROW_TRIGGER_FACTOR);
    return map;
  }


  @Override
  double update(final byte[] key, final int coupon) {
    int index = findOrInsertKey(key);
    return findOrInsertCoupon(index, (short) coupon);
  }

  @Override
  double getEstimate(final byte[] key) {
    final int index = findKey(key);
    if (index < 0) return 0;
    return getCouponCount(index);
  }

  // returns index if the given key is found
  // if not found, returns one's complement index of an empty slot for insertion
  // which may be over a deleted key
  @Override
  int findKey(final byte[] key) {
    final long[] hash = MurmurHash3.hash(key, SEED);
    int index = getIndex(hash[0], tableEntries_);
    int firstDeletedIndex = -1;
    while (getBit(stateArr_, index)) {
      if (couponsArr_[index * maxCouponsPerKey_] == 0) {
        if (firstDeletedIndex == -1) firstDeletedIndex = index;
      } else if (Map.arraysEqual(keysArr_, index * keySizeBytes_, key, 0, keySizeBytes_)) {
        return index;
      }
      index = (index + getStride(hash[1], tableEntries_)) % tableEntries_;
    }
    return firstDeletedIndex == -1 ? ~index : ~firstDeletedIndex;
  }

  @Override
  int findOrInsertKey(final byte[] key) {
    int index = findKey(key);
    if (index < 0) {
      index = ~index;
      if (getBit(stateArr_, index)) { // reusing slot from a deleted key
        //System.out.println("reusing slot " + index);
        Arrays.fill(couponsArr_, index * maxCouponsPerKey_, (index + 1) *  maxCouponsPerKey_, (short) 0);
        numDeletedKeys_--;
      }
      if (numActiveKeys_ + numDeletedKeys_ + 1 > capacityEntries_) {
        resize();
        index = ~findKey(key);
        assert(index >= 0);
      }
      System.arraycopy(key, 0, keysArr_, index * keySizeBytes_, keySizeBytes_);
      setBit(stateArr_, index);
      numActiveKeys_++;
    }
    return index;
  }

  // for internal use during resize, so no resize check here
  int insertKey(final byte[] key) {
    final long[] hash = MurmurHash3.hash(key, SEED);
    int index = getIndex(hash[0], tableEntries_);
    while (getBit(stateArr_, index)) {
      index = (index + getStride(hash[1], tableEntries_)) % tableEntries_;
    }
    System.arraycopy(key, 0, keysArr_, index * keySizeBytes_, keySizeBytes_);
    setBit(stateArr_, index);
    numActiveKeys_++;
    return index;
  }

  @Override
  void deleteKey(final int index) {
    couponsArr_[index * maxCouponsPerKey_] = 0;
    numActiveKeys_--;
    numDeletedKeys_++;
    if (numActiveKeys_ > MIN_NUM_ENTRIES && numActiveKeys_ < tableEntries_ * SHRINK_TRIGGER_FACTOR) {
      resize();
    }
  }

  private void resize() {
    final byte[] oldKeysArr = keysArr_;
    final short[] oldCouponsArr = couponsArr_;
    final byte[] oldStateArr = stateArr_;
    final int oldSizeKeys = tableEntries_;
    tableEntries_ = Math.max(
      Util.nextPrime((int) (numActiveKeys_ / TARGET_FILL_FACTOR)),
      MIN_NUM_ENTRIES
    );
    capacityEntries_ = (int)(tableEntries_ * GROW_TRIGGER_FACTOR);
    //System.out.println("resizing from " + oldSizeKeys + " to " + tableSizeKeys_);
    keysArr_ = new byte[tableEntries_ * keySizeBytes_];
    couponsArr_ = new short[tableEntries_ * maxCouponsPerKey_];
    stateArr_ = new byte[(int) Math.ceil(tableEntries_ / 8.0)];
    numActiveKeys_ = 0;
    numDeletedKeys_ = 0;
    for (int i = 0; i < oldSizeKeys; i++) {
      if (getBit(oldStateArr, i) && oldCouponsArr[i * maxCouponsPerKey_] != 0) {
        final byte[] key = Arrays.copyOfRange(oldKeysArr, i * keySizeBytes_, i * keySizeBytes_ + keySizeBytes_);
        final int index = insertKey(key);
        System.arraycopy(oldCouponsArr, i * maxCouponsPerKey_, couponsArr_, index * maxCouponsPerKey_, maxCouponsPerKey_);
      }
    }
  }

  @Override
  double findOrInsertCoupon(final int index, final short value) {
    final int offset = index * maxCouponsPerKey_;
    boolean wasFound = false;
    for (int i = 0; i < maxCouponsPerKey_; i++) {
      if (couponsArr_[offset + i] == 0) {
        if (wasFound) return i;
        couponsArr_[offset + i] = value;
        return i + 1;
      }
      if (couponsArr_[offset + i] == value) {
        wasFound = true;
      }
    }
    if (wasFound) return maxCouponsPerKey_;
    return -maxCouponsPerKey_;
  }

  @Override
  int getCouponCount(final int index) {
    final int offset = index * maxCouponsPerKey_;
    for (int i = 0; i < maxCouponsPerKey_; i++) {
      if (couponsArr_[offset + i] == 0) {
        return i;
      }
    }
    return maxCouponsPerKey_;
  }

  @Override
  CouponsIterator getCouponsIterator(final byte[] key) {
    final int index = findKey(key);
    if (index < 0) return null;
    return new CouponsIterator(couponsArr_, index * maxCouponsPerKey_, maxCouponsPerKey_);
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
    return numActiveKeys_ + numDeletedKeys_;
  }

  @Override
  public long getMemoryUsageBytes() {
    return keysArr_.length
        + (long)couponsArr_.length * Short.BYTES
        + stateArr_.length + 4 * Integer.BYTES;
  }

  @Override
  void updateEstimate(final int index, final double estimate) {
    // not used in this map
  }

  @Override
  int getActiveEntries() {
    return numActiveKeys_;
  }

  @Override
  int getDeletedEntries() {
    return numDeletedKeys_;
  }

}
