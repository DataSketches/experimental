package com.yahoo.sketches.hllmap;

import com.yahoo.sketches.hash.MurmurHash3;

class SingleCouponMap extends CouponMap {

  private static final int INITIAL_SIZE_ENTRIES = 7;
  private static final long SEED = 1234567890L;
  
  private final int keySizeBytes_;
  private int currentSizeEntries;
  private byte[] keys_;
  private short[] values_;
  private byte[] state_;

  SingleCouponMap(final int keySizeBytes) {
    keySizeBytes_ = keySizeBytes;
    keys_ = new byte[INITIAL_SIZE_ENTRIES * keySizeBytes_];
    values_ = new short[INITIAL_SIZE_ENTRIES];
    state_ = new byte[(int) Math.ceil(INITIAL_SIZE_ENTRIES / 8.0)];
  }

  int find(final byte[] key) {
    final long[] hash = MurmurHash3.hash(key, SEED);
    return 0;
  }

}
