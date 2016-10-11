package com.yahoo.sketches.hllmap;

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

class CouponTraverseMap extends Map {

  private final int numValuesPerKey_;
  private int currentSizeKeys_;
  private byte[] keys_;
  private short[] values_;
  private byte[] state_;
  private int numActiveKeys_;
  private int numDeletedKeys_;

  CouponTraverseMap(final int keySizeBytes, final int numValuesPerKey) {
    super(keySizeBytes);
    numValuesPerKey_ = numValuesPerKey;
    currentSizeKeys_ = 13;
    keys_ = new byte[currentSizeKeys_ * keySizeBytes_];
    values_ = new short[currentSizeKeys_ * numValuesPerKey_];
    state_ = new byte[(int) Math.ceil(currentSizeKeys_ / 8.0)];
  }

  @Override
  public double update(byte[] key, byte[] identifier) {
    return 0;
  }

  @Override
  public double getEstimate(byte[] key) {
    final int index = find(key);
    if (index < 0) return 0;
    return countValues(index);
  }

  @Override
  int couponUpdate(byte[] key, short coupon) {
    final int idx = findOrInsert(key);
    final int numValues = findOrInsertValue(idx, coupon);
    setBit(state_, idx);
    return numValues;
  }

  // returns index if the key is found, negative index otherwise so that insert can be done there
  int find(final byte[] key) {
    final long[] hash = MurmurHash3.hash(key, SEED);
    int index = getIndex(hash[0], currentSizeKeys_);
    while (getBit(state_, index)) {
      if (keyEquals(key, index)) return index;
      index = (index + getStride(hash[0], currentSizeKeys_)) % currentSizeKeys_;
    }
    return ~index;
  }

  int findOrInsert(final byte[] key) {
    int index = find(key);
    if (index < 0) {
      if (resizeIfNeeded()) {
        index = find(key);
      }
      numActiveKeys_++;
      setKey(~index, key);
      return ~index;
    }
    return index;
  }

  // returns true if resized
  private boolean resizeIfNeeded() {

    return false;
  }

  private void setKey(final int index, final byte[] key) {
    final int offset = index * keySizeBytes_;
    for (int i = 0; i < keySizeBytes_; i++) {
      keys_[offset + i] = key[i];
    }
  }

  private boolean keyEquals(final byte[] key, final int index) {
    final int offset = index * keySizeBytes_;
    for (int i = 0; i < keySizeBytes_; i++) {
      if (keys_[offset + i] != key[i]) return false;
    }
    return true;
  }

  int findOrInsertValue(final int index, final short value) {
    final int offset = index * numValuesPerKey_;
    boolean wasFound = false;
    for (int i = 0; i < numValuesPerKey_; i++) {
      if (values_[offset + i] == 0) {
        if (!wasFound) values_[offset + i] = value;
        return i + 1;
      }
      if (values_[offset + i] == value) {
        wasFound = true;
      }
    }
    if (wasFound) return numValuesPerKey_;
    return ~numValuesPerKey_;
  }

  int countValues(final int index) {
    final int offset = index * numValuesPerKey_;
    for (int i = 0; i < numValuesPerKey_; i++) {
      if (values_[offset + i] == 0) {
        return i;
      }
    }
    return numValuesPerKey_;
  }

  @Override
  MapValuesIterator getValuesIterator(final byte[] key) {
    final int index = find(key);
    if (index < 0) return null;
    return new MapValuesIterator(values_, index * numValuesPerKey_, numValuesPerKey_);
  }

}
