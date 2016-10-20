package com.yahoo.sketches.hllmap;

import static com.yahoo.sketches.hllmap.Util.checkKeySizeBytes;

import com.yahoo.sketches.hash.MurmurHash3;

/**
 * Base class of all the maps. Defines the basic API for all maps
 */
abstract class Map {

  static final long SEED = 1234567890L;
  static final int SIX_BIT_MASK = 0X3F; // 6 bits
  static final int TEN_BIT_MASK = 0X3FF; //10 bits

  final int keySizeBytes_;

  Map(int keySizeBytes) {
    checkKeySizeBytes(keySizeBytes);
    keySizeBytes_ = keySizeBytes;
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

  abstract double getEntrySizeBytes();

  abstract int getTableEntries();

  abstract int getCapacityEntries();

  abstract int getCurrentCountEntries();

  abstract long getMemoryUsageBytes();

  /**
   * Returns <tt>true</tt> if the two specified sub-arrays of bytes are <i>equal</i> to one another.
   * Two arrays are considered equal if all corresponding pairs of elements in the two arrays are
   * equal. In other words, two arrays are equal if and only if they contain the same elements
   * in the same order.
   *
   * @param a one sub-array to be tested for equality
   * @param offsetA the offset in bytes of the start of sub-array <i>a</i>.
   * @param b the other sub-array to be tested for equality
   * @param offsetB the offset in bytes of the start of sub-array <i>b</i>.
   * @param length the length in bytes of the two sub-arrays.
   * @return <tt>true</tt> if the two sub-arrays are equal
   */
  static final boolean arraysEqual(byte[] a, int offsetA, byte[] b, int offsetB, int length) {
    for (int i = 0; i < length; i++) {
      if (a[i + offsetA] != b[i + offsetB]) {
        return false;
      }
    }
    return true;
  }

  /**
   * Returns the HLL array index and value as a 16-bit coupon given the identifier to be hashed
   * and k.
   * @param identifier the given identifier
   * @return the HLL array index and value
   */
  static final int coupon16(byte[] identifier) {
    long[] hash = MurmurHash3.hash(identifier, SEED);
    int hllIdx = (int) (((hash[0] >>> 1) % 1024) & TEN_BIT_MASK); //hash[0] for 10-bit address
    int lz = Long.numberOfLeadingZeros(hash[1]);
    int value = ((lz > 62)? 62 : lz) + 1;
    return (value << 10) | hllIdx;
  }

  static final int coupon16Value(int coupon) {
    return (coupon >>> 10) & SIX_BIT_MASK;
  }

  static final int getIndex(final long hash, final int tableEntries) {
    return (int) ((hash >>> 1) % tableEntries);
  }

  static final int getStride(final long hash, final int tableEntries) {
    return (int) ((hash >>> 1) % (tableEntries - 2L) + 1L);
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
