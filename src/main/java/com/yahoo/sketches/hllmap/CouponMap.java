package com.yahoo.sketches.hllmap;

import com.yahoo.sketches.hash.MurmurHash3;


//Always holds all keys.
// prime size, double hash, no deletes, 1-bit state array
// state: 0: empty or valid; empty if coupon is 0, otherwise valid.
// state: 1: original coupon has been promoted, current coupon contains a table # instead.
// same growth algorithm as for the next levels, except no shrink. Constants may be specific.
@SuppressWarnings("unused")
class CouponMap extends Map {

  private int currentSizeEntries_;
  private byte[] keys_;
  private short[] values_;
  private byte[] state_;

  /**
   *
   * @param targetSizeBytes
   * @param keySizeBytes
   */
  CouponMap(final int targetSizeBytes, final int keySizeBytes) {
    super(keySizeBytes);
    final int numSlots = targetSizeBytes / (keySizeBytes + Short.BYTES);
    currentSizeEntries_ = Util.nextPrime(numSlots);
    keys_ = new byte[currentSizeEntries_ * keySizeBytes_];
    values_ = new short[currentSizeEntries_];
    state_ = new byte[(int) Math.ceil(currentSizeEntries_ / 8.0)];
  }

  @Override
  public double update(byte[] key, byte[] identifier) {
    return 0;
  }

  @Override
  public double getEstimate(byte[] key) {
    return 0;
  }

  @Override
  int couponUpdate(byte[] key, short coupon) {
    return 0;
  }

  // returns index if the key is found, negative index otherwise so that insert can be done there
  int find(final byte[] key) {
    final long[] hash = MurmurHash3.hash(key, SEED);
    int index = getIndex(hash[0], currentSizeEntries_);
    while (values_[index] != 0) {
      if (keyEquals(key, index)) return index;
      index = (index + getStride(hash[0], currentSizeEntries_)) % currentSizeEntries_;
    }
    return ~index;
  }

  int findOrInsert(final byte[] key) {
    final int index = find(key);
    if (index < 0) setKey(~index, key);
    return index;
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

  @Override
  MapValuesIterator getValuesIterator(byte[] key) {
    // TODO Auto-generated method stub
    return null;
  }

}
