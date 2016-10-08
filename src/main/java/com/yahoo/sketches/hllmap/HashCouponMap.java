package com.yahoo.sketches.hllmap;

// Outer hash: prime size, double hash, with deletes, 1-byte count per key, 255 is marker for "dirty"

// rebuilding TraverseCouponMap and HashCouponMap: can grow or shrink
// keep numValid and numInvalid
// grow if numValid + numInvalid > 0.9 * capacity
// shrink if numValid < 0.5 * capacity
// new size T ~= (10/7) * numValid
// BigInteger nextPrime() can be used
@SuppressWarnings("unused")
class HashCouponMap extends CouponMap {

  HashCouponMap(final int keySizeBytes) {

    //Inner hash table:
    // Linear probing, OASH, threshold = 0.75
    // Probably starts after Traverse > 8.  Need to be able to adjust this.

  }

}
