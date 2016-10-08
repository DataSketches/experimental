package com.yahoo.sketches.hllmap;

import com.yahoo.sketches.hash.MurmurHash3;


//Always holds all keys.
// prime size, double hash, no deletes, 1-bit state array
// state: 0: empty or valid; empty if coupon is 0, otherwise valid.
// state: 1: original coupon has been promoted, current coupon contains a table # instead.
// same growth algorithm as for the next levels, except no shrink. Constants may be specific.
@SuppressWarnings("unused")
class SingleCouponMap extends CouponMap {

  private static final long SEED = 1234567890L;

  private final int keySizeBytes_;
  private int currentSizeEntries_;
  private byte[] keys_;
  private short[] values_;
  private byte[] state_;

  SingleCouponMap(final int sizeBytes, final int keySizeBytes) {
    final int numSlots = sizeBytes / (keySizeBytes + Short.BYTES);
    keySizeBytes_ = keySizeBytes;
    currentSizeEntries_ = Util.nextPrime(numSlots);
    keys_ = new byte[currentSizeEntries_ * keySizeBytes_];
    values_ = new short[currentSizeEntries_];
    state_ = new byte[(int) Math.ceil(currentSizeEntries_ / 8.0)];
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

  private boolean keyEquals(final byte[] key, final int index) {
    final int offset = index * keySizeBytes_;
    for (int i = 0; i < keySizeBytes_; i++) {
      if (keys_[offset + i] != key[i]) return false;
    }
    return true;
  }

  private static final int STRIDE_HASH_BITS = 7;
  public static final int STRIDE_MASK = (1 << STRIDE_HASH_BITS) - 1;

  private static int getIndex(final long hash, final int numEntries) {
    return (int) ((hash >>> 1) % numEntries);
  }

  // make odd and independent of index assuming that the highest bits are not used for the index
  private static int getStride(final long hash, final int numEntries) {
    return (2 * (int) ((hash >>> (64 - STRIDE_HASH_BITS)) & STRIDE_MASK)) + 1;
  }

}
