package com.yahoo.sketches.hllmap;

import com.yahoo.sketches.SketchesArgumentException;

/**
 * Base class of all the maps. Defines the basic API for all maps
 */
public abstract class Map {

  static final long SEED = 1234567890L;

  final int keySizeBytes_;

  Map(int keySizeBytes) {
    if (keySizeBytes < 1) {
      throw new SketchesArgumentException("keyBytes must be > 0: " + keySizeBytes);
    }
    keySizeBytes_ = keySizeBytes;
  }

  int getKeySizeBytes() {
    return keySizeBytes_;
  }

  /**
   * Update this map with a key and a coupon.
   * Return the cardinality estimate of all identifiers that have been associated with this key,
   * including this update.
   * @param key the dimensional criteria for measuring cardinality
   * @param coupon the property associated with the key for which cardinality is to be measured.
   * @return the cardinality estimate of all identifiers that have been associated with this key,
   * including this update.
   */
  abstract double update(byte[] key, int coupon);

  /**
   * Returns the estimate of the cardinality of identifiers associated with the given key.
   * @param key the given key
   * @return the estimate of the cardinality of identifiers associated with the given key.
   */
  abstract double getEstimate(byte[] key);

  abstract MapValuesIterator getValuesIterator(byte[] key);

  private static final int STRIDE_HASH_BITS = 7;
  public static final int STRIDE_MASK = (1 << STRIDE_HASH_BITS) - 1;

  static int getIndex(final long hash, final int numEntries) {
    return (int) ((hash >>> 1) % numEntries);
  }

  // make odd and independent of index assuming that the highest bits are not used for the index
  static int getStride(final long hash, final int numEntries) {
    return (2 * (int) ((hash >>> (64 - STRIDE_HASH_BITS)) & STRIDE_MASK)) + 1;
  }

  static boolean getBit(final byte[] bits, final int index) {
    final int offset = index / 8;
    final int mask = 1 << (index % 8);
    return (bits[offset] & mask) > 0;
  }

  static void clearBit(final byte[] bits, final int index) {
    final int offset = index / 8;
    final int mask = 1 << (index % 8);
    bits[offset] &= ~mask;
  }

  static void setBit(final byte[] bits, final int index) {
    final int offset = index / 8;
    final int mask = 1 << (index % 8);
    bits[offset] |= mask;
  }

}
