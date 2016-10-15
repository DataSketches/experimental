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
  double update(final byte[] key, final int coupon) {
    int index = findOrInsertKey(key);
    return findOrInsertValue(index, (short) coupon);
  }

  @Override
  double getEstimate(final byte[] key) {
    final int index = findKey(key);
    if (index < 0) return 0;
    return countValues(index);
  }

  // returns index if the given key is found
  // if not found, returns two's complement index of an empty slot for insertion
  @Override
  int findKey(final byte[] key) {
    final long[] hash = MurmurHash3.hash(key, SEED);
    int index = getIndex(hash[0], currentSizeKeys_);
    int firstDeletedIndex = -1;
    while (getBit(state_, index)) {
      if (values_[index * numValuesPerKey_] == 0) {
        firstDeletedIndex = index;
      } else if (Util.equals(keys_, index * keySizeBytes_, key, 0, keySizeBytes_)) {
        return index;
      }
      index = (index + getStride(hash[1], currentSizeKeys_)) % currentSizeKeys_;
    }
    return firstDeletedIndex == -1 ? ~index : ~firstDeletedIndex;
  }

  @Override
  int findOrInsertKey(final byte[] key) {
    int index = findKey(key);
    if (index < 0) {
      if (resizeIfNeeded()) {
        index = findKey(key);
      }
      index = ~index;
      System.arraycopy(key, 0, keys_, index * keySizeBytes_, keySizeBytes_);
      setBit(state_, index);
      numActiveKeys_++;
    }
    return index;
  }

  // for internal use during resize, so no resize check here
  int insertKey(final byte[] key) {
    final long[] hash = MurmurHash3.hash(key, SEED);
    int index = getIndex(hash[0], currentSizeKeys_);
    while (getBit(state_, index)) {
      index = (index + getStride(hash[1], currentSizeKeys_)) % currentSizeKeys_;
    }
    System.arraycopy(key, 0, keys_, index * keySizeBytes_, keySizeBytes_);
    setBit(state_, index);
    numActiveKeys_++;
    return index;
  }

  @Override
  void deleteKey(final int index) {
    values_[index * numValuesPerKey_] = 0;
    numDeletedKeys_++;
  }

  // returns true if resized
  private boolean resizeIfNeeded() {
    if (numActiveKeys_ + numDeletedKeys_ > currentSizeKeys_ * GROW_THRESHOLD) {
      final byte[] oldKeys = keys_;
      final short[] oldValues = values_;
      final byte[] oldState = state_;
      final int oldSizeKeys = currentSizeKeys_;
      currentSizeKeys_ = Util.nextPrime((int) (10.0 / 7 * numActiveKeys_));
      keys_ = new byte[currentSizeKeys_ * keySizeBytes_];
      values_ = new short[currentSizeKeys_ * numValuesPerKey_];
      state_ = new byte[(int) Math.ceil(currentSizeKeys_ / 8.0)];
      numActiveKeys_ = 0;
      numDeletedKeys_ = 0;
      for (int i = 0; i < oldSizeKeys; i++) {
        if (getBit(oldState, i) && oldValues[i * numValuesPerKey_] != 0) {
          final byte[] key = Arrays.copyOfRange(oldKeys, i * keySizeBytes_, i * keySizeBytes_ + keySizeBytes_);
          final int index = insertKey(key);
          System.arraycopy(oldValues, i * numValuesPerKey_, values_, index * numValuesPerKey_, numValuesPerKey_);
        }
      }
      return true;
    }
    return false;
  }

  @Override
  int findOrInsertValue(final int index, final short value) {
    final int offset = index * numValuesPerKey_;
    boolean wasFound = false;
    for (int i = 0; i < numValuesPerKey_; i++) {
      if (values_[offset + i] == 0) {
        if (wasFound) return i;
        values_[offset + i] = value;
        return i + 1;
      }
      if (values_[offset + i] == value) {
        wasFound = true;
      }
    }
    if (wasFound) return numValuesPerKey_;
    return -numValuesPerKey_;
  }

  @Override
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
    final int index = findKey(key);
    if (index < 0) return null;
    return new MapValuesIterator(values_, index * numValuesPerKey_, numValuesPerKey_);
  }

}
