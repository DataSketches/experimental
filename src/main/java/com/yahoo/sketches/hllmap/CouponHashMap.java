package com.yahoo.sketches.hllmap;

import java.util.Arrays;

import com.yahoo.sketches.hash.MurmurHash3;

// Outer hash: prime size, double hash, with deletes, 1-byte count per key, 255 is marker for "dirty"

// rebuilding TraverseCouponMap and CouponHashMap: can grow or shrink
// keep numValid and numInvalid
// grow if numValid + numInvalid > 0.9 * capacity
// shrink if numValid < 0.5 * capacity
// new size T ~= (10/7) * numValid
// BigInteger nextPrime() can be used

//Inner hash table:
// Linear probing, OASH, threshold = 0.75
// Probably starts after Traverse > 8.  Need to be able to adjust this.

class CouponHashMap extends CouponMap {

  private static final double GROW_THRESHOLD = 0.9;
  private static final double INNER_HASH_MAP_RATIO = 0.75;
  private static final byte DELETED_KEY_MARKER = (byte) 255;

  private final int numValuesPerKey_;
  private int currentSizeKeys_;
  private byte[] keys_;
  private short[] values_;
  private byte[] counts_;
  private int numActiveKeys_;
  private int numDeletedKeys_;

  CouponHashMap(final int keySizeBytes, final int numValuesPerKey) {
    super(keySizeBytes);
    numValuesPerKey_ = numValuesPerKey;
    currentSizeKeys_ = 13;
    keys_ = new byte[currentSizeKeys_ * keySizeBytes_];
    values_ = new short[currentSizeKeys_ * numValuesPerKey_];
    counts_ = new byte[currentSizeKeys_];
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
    while (counts_[index] != 0) {
      if (counts_[index] == DELETED_KEY_MARKER) {
        if (firstDeletedIndex == -1) firstDeletedIndex = index;
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
      numActiveKeys_++;
    }
    return index;
  }

  // for internal use during resize, so no resize check and no deleted key check here
  int insertKey(final byte[] key) {
    final long[] hash = MurmurHash3.hash(key, SEED);
    int index = getIndex(hash[0], currentSizeKeys_);
    while (counts_[index] != 0) {
      index = (index + getStride(hash[1], currentSizeKeys_)) % currentSizeKeys_;
    }
    System.arraycopy(key, 0, keys_, index * keySizeBytes_, keySizeBytes_);
    numActiveKeys_++;
    return index;
  }

  @Override
  void deleteKey(final int index) {
    counts_[index] = DELETED_KEY_MARKER;
    numDeletedKeys_++;
  }

  private boolean resizeIfNeeded() {
    if (numActiveKeys_ + numDeletedKeys_ > currentSizeKeys_ * GROW_THRESHOLD) {
      final byte[] oldKeys = keys_;
      final short[] oldValues = values_;
      final byte[] oldCounts = counts_;
      final int oldSizeKeys = currentSizeKeys_;
      currentSizeKeys_ = Util.nextPrime((int) (10.0 / 7 * numActiveKeys_));
      System.out.println("resizing from " + oldSizeKeys + " to " + currentSizeKeys_);
      keys_ = new byte[currentSizeKeys_ * keySizeBytes_];
      values_ = new short[currentSizeKeys_ * numValuesPerKey_];
      counts_ = new byte[currentSizeKeys_];
      numActiveKeys_ = 0;
      numDeletedKeys_ = 0;
      for (int i = 0; i < oldSizeKeys; i++) {
        if (oldCounts[i] != 0) {
          final byte[] key = Arrays.copyOfRange(oldKeys, i * keySizeBytes_, i * keySizeBytes_ + keySizeBytes_);
          final int index = insertKey(key);
          System.arraycopy(oldValues, i * numValuesPerKey_, values_, index * numValuesPerKey_, numValuesPerKey_);
          counts_[index] = oldCounts[i];
        }
      }
      return true;
    }
    return false;
  }

  @Override
  int findOrInsertValue(final int index, final short value) {
    final int offset = index * numValuesPerKey_;
    int valueIndex = value % numValuesPerKey_;
    while (values_[offset + valueIndex] != 0) {
      if (values_[offset + valueIndex] == value) return (counts_[index] & 0xff);
      valueIndex++;
    }
    if ((counts_[index] & 0xff) + 1 > INNER_HASH_MAP_RATIO * numValuesPerKey_) {
      return -(counts_[index] & 0xff);
    }
    values_[offset + valueIndex] = value;
    return ((++counts_[index]) & 0xff);
  }

  @Override
  int countValues(final int index) {
    return (counts_[index] & 0xff);
  }

  @Override
  MapValuesIterator getValuesIterator(final byte[] key) {
    final int index = findKey(key);
    if (index < 0) return null;
    return new MapValuesIterator(values_, index * numValuesPerKey_, numValuesPerKey_);
  }

}
