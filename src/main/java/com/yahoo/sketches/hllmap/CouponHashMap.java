package com.yahoo.sketches.hllmap;

// Outer hash: prime size, double hash, with deletes, 1-byte count per key, 255 is marker for "dirty"

// rebuilding TraverseCouponMap and CouponHashMap: can grow or shrink
// keep numValid and numInvalid
// grow if numValid + numInvalid > 0.9 * capacity
// shrink if numValid < 0.5 * capacity
// new size T ~= (10/7) * numValid
// BigInteger nextPrime() can be used

//@SuppressWarnings("unused")
class CouponHashMap extends CouponMap {

  CouponHashMap(final int keySizeBytes) {
    super(keySizeBytes);

    //Inner hash table:
    // Linear probing, OASH, threshold = 0.75
    // Probably starts after Traverse > 8.  Need to be able to adjust this.

  }

  @Override
  double update(byte[] key, int coupon) {
    return 0;
  }

  @Override
  double getEstimate(byte[] key) {
    return 0;
  }

  @Override
  MapValuesIterator getValuesIterator(byte[] key) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  int findKey(byte[] key) {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  int findOrInsertKey(byte[] key) {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  void deleteKey(int index) {
    // TODO Auto-generated method stub
  }

  @Override
  int findOrInsertValue(int index, short coupon) {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  int countValues(int index) {
    // TODO Auto-generated method stub
    return 0;
  }

}
