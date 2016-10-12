package com.yahoo.sketches.hllmap;

import java.util.Arrays;

import com.yahoo.sketches.hash.MurmurHash3;


//Always holds all keys.
// prime size, double hash, no deletes, 1-bit state array
// state: 0: empty or valid; empty if coupon is 0, otherwise valid.
// state: 1: original coupon has been promoted, current coupon contains a table # instead.
// same growth algorithm as for the next levels, except no shrink. Constants may be specific.

class SingleCouponMap extends Map {

  private int currentSizeKeys_;
  private byte[] keys_;
  private short[] values_;
  private byte[] state_;
  private int numKeys_;

  /**
   *
   * @param targetSizeBytes
   * @param keySizeBytes
   */
  SingleCouponMap(final int targetSizeBytes, final int keySizeBytes) {
    super(keySizeBytes);
    final int numSlots = targetSizeBytes / (keySizeBytes + Short.BYTES);
    currentSizeKeys_ = Util.nextPrime(numSlots);
    keys_ = new byte[currentSizeKeys_ * keySizeBytes_];
    values_ = new short[currentSizeKeys_];
    state_ = new byte[(int) Math.ceil(currentSizeKeys_ / 8.0)];
  }

  @Override
  double update(byte[] key, int coupon) {
    int index = findOrInsertKey(key);
    if (index < 0) {
      setValue(~index, (short) coupon, true);
      return 1;
    }
    return ~values_[index];
  }

  @Override
  double getEstimate(byte[] key) {
    final int index = findKey(key);
    if (index < 0) return 0;
    if (isCoupon(index)) return 1;
    return ~values_[index];
  }

  // returns index if the key is found, negative index otherwise so that insert can be done there
  int findKey(final byte[] key) {
    final long[] hash = MurmurHash3.hash(key, SEED);
    int index = getIndex(hash[0], currentSizeKeys_);
    while (values_[index] != 0) {
      if (keyEquals(key, index)) return index;
      index = (index + getStride(hash[1], currentSizeKeys_)) % currentSizeKeys_;
    }
    return ~index;
  }

  int findOrInsertKey(final byte[] key) {
    int index = findKey(key);
    if (index < 0) {
      if (resizeIfNeeded()) {
        index = findKey(key);
      }
      setKey(~index, key);
      numKeys_++;
    }
    return index;
  }

  int insertKey(final byte[] key) {
    final long[] hash = MurmurHash3.hash(key, SEED);
    int index = getIndex(hash[0], currentSizeKeys_);
    while (values_[index] != 0) {
      index = (index + getStride(hash[1], currentSizeKeys_)) % currentSizeKeys_;
    }
    setKey(index, key);
    numKeys_++;
    return index;
  }

  // returns true if resized
  private boolean resizeIfNeeded() {
    if (numKeys_ > currentSizeKeys_ * 0.9) {
      final byte[] oldKeys = keys_;
      final short[] oldValues = values_;
      final byte[] oldState = state_;
      final int oldSizeKeys = currentSizeKeys_;
      currentSizeKeys_ = Util.nextPrime((int) (10.0 / 7 * numKeys_));
      keys_ = new byte[currentSizeKeys_ * keySizeBytes_];
      values_ = new short[currentSizeKeys_];
      state_ = new byte[(int) Math.ceil(currentSizeKeys_ / 8.0)];
      numKeys_ = 0;
      for (int i = 0; i < oldSizeKeys; i++) {
        if (oldValues[i] != 0) {
          final byte[] key = Arrays.copyOfRange(oldKeys, i * keySizeBytes_, i * keySizeBytes_ + keySizeBytes_);
          final int index = insertKey(key);
          values_[index] = oldValues[i];
          if (getBit(oldState, i)) setBit(state_, index);
        }
      }
      return true;
    }
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

  boolean isCoupon(final int index) {
    return !getBit(state_, index);
  }

  short getValue(final int index) {
    return values_[index];
  }

  // assumes that the state bit is never cleared
  void setValue(final int index, final short value, final boolean isCoupon) {
    values_[index] = value;
    if (!isCoupon) {
      setBit(state_, index);
    }
  }

}
