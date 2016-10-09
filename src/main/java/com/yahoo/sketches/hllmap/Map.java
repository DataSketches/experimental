package com.yahoo.sketches.hllmap;

import com.yahoo.sketches.SketchesArgumentException;

/**
 * Base class of all the maps. Defines the basic API for all maps
 */
public abstract class Map {
  final int keySizeBytes_;


  Map(int keySizeBytes) {
    if (keySizeBytes < 1) {
      throw new SketchesArgumentException("keyBytes must be > 0: " + keySizeBytes);
    }
    keySizeBytes_ = keySizeBytes;
  }

  /**
   * Update this map with a key and an identifier.
   * Return the cardinality estimate of all identifiers that have been associated with this key,
   * including this update.
   * @param key the dimensional criteria for measuring cardinality
   * @param identifier the property associated with the key for which cardinality is to be measured.
   * @return the cardinality estimate of all identifiers that have been associated with this key,
   * including this update.
   */
  public abstract double update(byte[] key, byte[] identifier);

  /**
   * Returns the estimate of the cardinality of identifiers associated with the given key.
   * @param key the given key
   * @return the estimate of the cardinality of identifiers associated with the given key.
   */
  public abstract double getEstimate(byte[] key);

  /**
   * Update this map with a key and a coupon.
   * @param key the given key
   * @param coupon a valid coupon.
   */
  abstract void couponUpdate(byte[] key, int coupon);

}
