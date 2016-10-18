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

  private static final double GROW_THRESHOLD = 0.9;

  private final int maxCouponsPerKey_;

  private int tableSizeKeys_;
  private byte[] keysArr_;
  private short[] couponsArr_;
  private byte[] stateArr_;
  private int numActiveKeys_;
  private int numDeletedKeys_;

  CouponTraverseMap(final int keySizeBytes, final int maxCouponsPerKey) {
    super(keySizeBytes);
    maxCouponsPerKey_ = maxCouponsPerKey;
    tableSizeKeys_ = 13;
    keysArr_ = new byte[tableSizeKeys_ * keySizeBytes_];
    couponsArr_ = new short[tableSizeKeys_ * maxCouponsPerKey_];
    stateArr_ = new byte[(int) Math.ceil(tableSizeKeys_ / 8.0)];
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
    int index = getIndex(hash[0], tableSizeKeys_);
    int firstDeletedIndex = -1;
    while (getBit(stateArr_, index)) {
      if (couponsArr_[index * maxCouponsPerKey_] == 0) {
        if (firstDeletedIndex == -1) firstDeletedIndex = index;
      } else if (Map.arraysEqual(keysArr_, index * keySizeBytes_, key, 0, keySizeBytes_)) {
        return index;
      }
      index = (index + getStride(hash[1], tableSizeKeys_)) % tableSizeKeys_;
    }
    return firstDeletedIndex == -1 ? ~index : ~firstDeletedIndex;
  }

  @Override
  int findOrInsertKey(final byte[] key) {
    int index = findKey(key);
    if (index < 0) {
      if (numActiveKeys_ + numDeletedKeys_ > tableSizeKeys_ * GROW_THRESHOLD) {
        resize();
        index = findKey(key);
      }
      index = ~index;
      if (getBit(stateArr_, index)) { // reusing slot from a deleted key
        //System.out.println("reusing slot " + index);
        Arrays.fill(couponsArr_, index * maxCouponsPerKey_, (index + 1) *  maxCouponsPerKey_, (short) 0);
        numDeletedKeys_--;
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
    int index = getIndex(hash[0], tableSizeKeys_);
    while (getBit(stateArr_, index)) {
      index = (index + getStride(hash[1], tableSizeKeys_)) % tableSizeKeys_;
    }
    System.arraycopy(key, 0, keysArr_, index * keySizeBytes_, keySizeBytes_);
    setBit(stateArr_, index);
    numActiveKeys_++;
    return index;
  }

  @Override
  void deleteKey(final int index) {
    couponsArr_[index * maxCouponsPerKey_] = 0;
    numDeletedKeys_++;
  }

  private void resize() {
    final byte[] oldKeysArr = keysArr_;
    final short[] oldCouponsArr = couponsArr_;
    final byte[] oldStateArr = stateArr_;
    final int oldSizeKeys = tableSizeKeys_;
    tableSizeKeys_ = Util.nextPrime((int) (10.0 / 7 * numActiveKeys_));
    //System.out.println("resizing from " + oldSizeKeys + " to " + tableSizeKeys_);
    keysArr_ = new byte[tableSizeKeys_ * keySizeBytes_];
    couponsArr_ = new short[tableSizeKeys_ * maxCouponsPerKey_];
    stateArr_ = new byte[(int) Math.ceil(tableSizeKeys_ / 8.0)];
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
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  public int getTableEntries() {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  public int getCapacityEntries() {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  public int getCurrentCountEntries() {
    return numActiveKeys_ + numDeletedKeys_;
  }

  @Override
  public int getMemoryUsageBytes() {
    return keysArr_.length + couponsArr_.length * Short.BYTES + stateArr_.length + 4 * Integer.BYTES;
  }

  @Override
  void updateEstimate(final int index, final double estimate) {
    // not used in this map
  }

}
