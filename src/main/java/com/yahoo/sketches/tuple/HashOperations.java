/*
 * Copyright 2015, Yahoo! Inc.
 * Licensed under the terms of the Apache License 2.0. See LICENSE file at the project root for terms.
 */
package com.yahoo.sketches.tuple;

import com.yahoo.sketches.memory.Memory;

/**
 * Helper class for the common hash table methods.
 * 
 * @author Lee Rhodes
 * @author Kevin Lang
 */
final class HashOperations {

  private static final int STRIDE_HASH_BITS = 7; 
  static final int STRIDE_MASK = (1 << STRIDE_HASH_BITS) - 1;

  private HashOperations() {}

  // make odd and independent of index assuming lgArrLongs lowest bits of the hash were used for index
  private static int getStride(long hash, int lgArrLongs) {
    return (2 * (int) ((hash >> (lgArrLongs)) & STRIDE_MASK)) + 1;
  }

  /**
   * This is a classical Knuth-style Open Addressing, Double Hash search scheme.
   * 
   * @param hashTable The hash table to search. Must be a power of 2 in size.
   * @param lgArrLongs <a href="{@docRoot}/resources/dictionary.html#lgArrLongs">See lgArrLongs</a>.
   * lgArrLongs &le; log2(hashTable.length).
   * @param hash A hash value to search for. Must not be zero.
   * @return Current probe index if found, -1 if not found.
   */
  static int hashSearch(long[] hashTable, int lgArrLongs, long hash) {
    if (hash == 0) throw new IllegalArgumentException("Given hash cannot be zero: "+hash);
    int arrayMask = (1 << lgArrLongs) - 1; // current Size -1
    int stride = getStride(hash, lgArrLongs);
    int curProbe = (int) (hash & arrayMask);
    // search for duplicate or zero
    while (hashTable[curProbe] != 0) {
      if (hashTable[curProbe] == hash) return curProbe; // a duplicate
      curProbe = (curProbe + stride) & arrayMask;
    }
    return -1;
  }

  /**
   * This is a classical Knuth-style Open Addressing, Double Hash insert scheme.
   * 
   * @param hashTable the hash table to insert into.
   * @param lgArrLongs <a href="{@docRoot}/resources/dictionary.html#lgArrLongs">See lgArrLongs</a>.
   * lgArrLongs &le; log2(hashTable.length).
   * @param hash hash value that must not be zero and if not a duplicate will be inserted into the
   * array into an empty slot
   * @return index if found, -(index + 1) if inserted
   */
  static int hashSearchOrInsert(long[] hashTable, int lgArrLongs, long hash) {
    int arrayMask = (1 << lgArrLongs) - 1; // current Size -1
    int stride = getStride(hash, lgArrLongs);
    int curProbe = (int) (hash & arrayMask);
    // search for duplicate or zero
    while (hashTable[curProbe] != 0) {
      if (hashTable[curProbe] == hash) return curProbe; // a duplicate
      // not a duplicate and not zero, continue searching
      curProbe = (curProbe + stride) & arrayMask;
    }
    // must be zero, so insert
    hashTable[curProbe] = hash;
    return ~curProbe;
  }

  /**
   * This is a classical Knuth-style Open Addressing, Double Hash insert scheme.
   * This method assumes that the input hash is not a duplicate.
   * Useful for rebuilding tables to avoid unnecessary comparisons.
   *
   * @param hashTable the hash table to insert into.
   * @param lgArrLongs <a href="{@docRoot}/resources/dictionary.html#lgArrLongs">See lgArrLongs</a>.
   * lgArrLongs &le; log2(hashTable.length).
   * @param hash value that must not be zero and will be inserted into the array into an empty slot.
   * @return index of insertion.
   */
  static int hashInsertOnly(long[] hashTable, int lgArrLongs, long hash) {
    int arrayMask = (1 << lgArrLongs) - 1; // current Size -1
    int stride = getStride(hash, lgArrLongs);
    int curProbe = (int) (hash & arrayMask);
    while (hashTable[curProbe] != 0) {
      curProbe = (curProbe + stride) & arrayMask;
    }
    hashTable[curProbe] = hash;
    return curProbe;
  }

  /**
   * This is a classical Knuth-style Open Addressing, Double Hash insert scheme, but inserts
   * values directly into a Memory.
   * 
   * @param mem The Memory hash table to insert into.
   * @param lgArrLongs <a href="{@docRoot}/resources/dictionary.html#lgArrLongs">See lgArrLongs</a>.
   * lgArrLongs &le; log2(hashTable.length).
   * @param hash A hash value that must not be zero and if not a duplicate will be inserted into the
   * array into an empty slot.
   * @param memOffsetBytes offset in the memory where the hash array starts
   * @return index if found, -(index + 1) if inserted
   */
  static int hashSearchOrInsert(Memory mem, int lgArrLongs, long hash, int memOffsetBytes) {
    int arrayMask = (1 << lgArrLongs) - 1; // current Size -1
    int stride = getStride(hash, lgArrLongs);
    int curProbe = (int) (hash & arrayMask);
    int curProbeOffsetBytes = (curProbe << 3) + memOffsetBytes; 
    long curArrayHash = mem.getLong(curProbeOffsetBytes);
    // search for duplicate or zero
    while (curArrayHash != 0) {
      if (curArrayHash == hash) return curProbe; // curArrayHash is a duplicate
      // curArrayHash is not a duplicate and not zero, continue searching
      curProbe = (curProbe + stride) & arrayMask;
      curProbeOffsetBytes = (curProbe << 3) + memOffsetBytes;
      curArrayHash = mem.getLong(curProbeOffsetBytes);
    }
    // must be zero, so insert
    mem.putLong(curProbeOffsetBytes, hash);
    return ~curProbe;
  }

  /**
   * This is a classical Knuth-style Open Addressing, Double Hash search scheme.
   * 
   * @param mem The Memory hash table to search.
   * @param lgArrLongs <a href="{@docRoot}/resources/dictionary.html#lgArrLongs">See lgArrLongs</a>.
   * lgArrLongs &le; log2(hashTable.length).
   * @param hash A hash value to search for. Must not be zero.
   * @param memOffsetBytes offset in the memory where the hash array starts
   * @return index if found, -1 if not found.
   */
  static int hashSearch(Memory mem, int lgArrLongs, long hash, int memOffsetBytes) {
    int arrayMask = (1 << lgArrLongs) - 1;
    int stride = getStride(hash, lgArrLongs);
    int curProbe = (int) (hash & arrayMask);
    int curProbeOffsetBytes = (curProbe << 3) + memOffsetBytes; 
    long curArrayHash = mem.getLong(curProbeOffsetBytes);
    while (curArrayHash != 0) {
      if (curArrayHash == hash) return curProbe;
      curProbe = (curProbe + stride) & arrayMask;
      curProbeOffsetBytes = (curProbe << 3) + memOffsetBytes;
      curArrayHash = mem.getLong(curProbeOffsetBytes);
    }
    return -1;
  }

  /**
   * This is a classical Knuth-style Open Addressing, Double Hash insert scheme, but inserts
   * values directly into a Memory.
   * This method assumes that the input hash is not a duplicate.
   * Useful for rebuilding tables to avoid unnecessary comparisons.
   *
   * @param mem The Memory hash table to insert into.
   * @param lgArrLongs <a href="{@docRoot}/resources/dictionary.html#lgArrLongs">See lgArrLongs</a>.
   * lgArrLongs &le; log2(hashTable.length).
   * @param hash value that must not be zero and will be inserted into the array into an empty slot.
   * @param memOffsetBytes offset in the memory where the hash array starts
   * @return index of insertion.
   */
  static int hashInsertOnly(Memory mem, int lgArrLongs, long hash, int memOffsetBytes) {
    int arrayMask = (1 << lgArrLongs) - 1; // current Size -1
    int stride = getStride(hash, lgArrLongs);
    int curProbe = (int) (hash & arrayMask);
    int curProbeOffsetBytes = (curProbe << 3) + memOffsetBytes; 
    long curArrayHash = mem.getLong(curProbeOffsetBytes);
    while (curArrayHash != 0L) {
      curProbe = (curProbe + stride) & arrayMask;
      curProbeOffsetBytes = (curProbe << 3) + memOffsetBytes;
      curArrayHash = mem.getLong(curProbeOffsetBytes);
    }
    mem.putLong(curProbeOffsetBytes, hash);
    return curProbe;
  }

  /**
   * @param thetaLong must be greater than zero otherwise throws an exception.
   * <a href="{@docRoot}/resources/dictionary.html#thetaLong">See Theta Long</a>
   */
  static void checkThetaCorruption(final long thetaLong) {
    //if any one of the groups go negative it fails.
    if (( thetaLong | (thetaLong-1) ) < 0L ) {
      throw new IllegalStateException(
          "Data Corruption: thetaLong was negative or zero: "+ "ThetaLong: "+thetaLong);
    }
  }

  /**
   * @param hash must be greater than -1 otherwise throws an exception.
   * Note a hash of zero is normally ignored, but a negative hash is never allowed.
   */
  static void checkHashCorruption(final long hash) {
    //if any one of the groups go negative it fails.
    if ( hash < 0L ) {
      throw new IllegalArgumentException(
          "Data Corruption: hash was negative: "+ "Hash: "+hash);
    }
  }

  /**
   * Return true (continue) if hash is greater than or equal to thetaLong, or if hash == 0, 
   * or if hash == Long.MAX_VALUE.
   * @param thetaLong must be greater than the hash value
   * <a href="{@docRoot}/resources/dictionary.html#thetaLong">See Theta Long</a>
   * @param hash must be less than thetaLong and not less than or equal to zero.
   * @return true (continue) if hash is greater than or equal to thetaLong, or if hash == 0, 
   * or if hash == Long.MAX_VALUE.
   */
  static boolean continueCondition(final long thetaLong, final long hash) {
    //if any one of the groups go negative it returns true
    return (( (hash-1L) | (thetaLong - hash -1L)) < 0L );
  }

  /**
   * Checks for invalid values of both a hash value and of a theta value.
   * @param thetaLong cannot be negative or zero, otherwise it throws an exception
   * @param hash cannot be negative, otherwise it throws an exception
   */
  static void checkHashAndThetaCorruption(final long thetaLong, final long hash) {
    //if any one of the groups go negative it fails.
    if (( hash | thetaLong | (thetaLong-1L) ) < 0L ) {
      throw new IllegalStateException(
          "Data Corruption: Either hash was negative or thetaLong was negative or zero: "+
          "Hash: "+hash+", ThetaLong: "+thetaLong);
    }
  }

}
