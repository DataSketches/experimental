package com.yahoo.sketches.hllmap;

import com.yahoo.sketches.SketchesArgumentException;
import com.yahoo.sketches.hash.MurmurHash3;

//prime size, double hash, no deletes.
@SuppressWarnings("unused")
class HllMap extends Map {
  private static final double LOAD_FACTOR = 15.0/16.0;
  private final int k_;
  private final int hllArrLongs_;

  private int tableEntries_; //Full size of the table
  private int capacityEntries_;  //max capacity entries defined by Load factor
  private int curCountEntries_;  //current count of valid entries
  private float growthFactor_;  //e.g., 1.2 to 2.0

  //Arrays
  private byte[] keys_; //keys of zero are allowed
  private long[] hllArr_;
  private double[] invPow2Sum1_;
  private double[] invPow2Sum2_;
  private float[] hipEstAccum_;
  private byte[] validBit_;

  /**
   * Private constructor used to set all finals
   * @param keySizeBytes size of key in bytes
   * @param k size of HLL sketch
   */
  private HllMap(final int keySizeBytes, int k) {
    super(keySizeBytes);
    k_ = k;
    hllArrLongs_ = k/10 + 1;
  }

  HllMap getInstance(int targetSizeBytes, int keySizeBytes, int k, float growthFactor) {
    if ((k != 1024) && (k != 512)) {
      throw new SketchesArgumentException("K must be either 1024 or 512: " + k);
    }
    if (growthFactor <= 1.0) {
      throw new SketchesArgumentException("growthFactor must be > 1.0: " + growthFactor);
    }

    int entries = (int)(targetSizeBytes / (keySizeBytes + (hllArrLongs_ * 8) + 20 + 0.125));
    tableEntries_ = Util.nextPrime(entries);
    capacityEntries_ = (int)(tableEntries_ * LOAD_FACTOR);
    curCountEntries_ = 0;
    growthFactor_ = growthFactor;

    keys_ = new byte[tableEntries_ * keySizeBytes_];
    hllArr_ = new long[tableEntries_ * hllArrLongs_];
    invPow2Sum1_ = new double[tableEntries_];
    invPow2Sum2_ = new double[tableEntries_];
    hipEstAccum_ = new float[tableEntries_];
    validBit_ = new byte[tableEntries_/8 + 1];

    return new HllMap(keySizeBytes, k);
  }

private final void growSize() {
  int newTableEntries = Util.nextPrime((int)(tableEntries_ * growthFactor_));
  int newCapacityEntries = (int)(newTableEntries * LOAD_FACTOR);

  byte[] newKeys = new byte[newTableEntries * keySizeBytes_];
  long[] newHllArr = new long[newTableEntries * hllArrLongs_];
  double[] newInvPow2Sum1 = new double[newTableEntries];
  double[] newInvPow2Sum2 = new double[newTableEntries];
  float[] newHipEstAccum = new float[newTableEntries];
  byte[] newValidBit = new byte[newTableEntries/8 + 1];

  for (int oldIndex = 0; oldIndex < tableEntries_; oldIndex++) {
    if (Util.isBitZero(validBit_, oldIndex)) continue;

    byte[] key = Util.getBytes(keys_, keySizeBytes_, oldIndex); //get the key
    int newIndex = index(key, newTableEntries);
    Util.putBytes(newKeys, keySizeBytes_, newIndex, key);//put the key
    //put the rest of the row
    System.arraycopy(
        hllArr_, oldIndex * hllArrLongs_, newHllArr, newIndex * hllArrLongs_, hllArrLongs_);
    newInvPow2Sum1[newIndex] = invPow2Sum1_[oldIndex];
    newInvPow2Sum2[newIndex] = invPow2Sum2_[oldIndex];
    newHipEstAccum[newIndex] = hipEstAccum_[oldIndex];
    Util.setBitToOne(newValidBit, oldIndex);
  }
  //restore into sketch
  tableEntries_ = newTableEntries;
  capacityEntries_ = (int)(tableEntries_ * LOAD_FACTOR);
  //curCountEntries_, growthFactor_  unchanged

  keys_ = newKeys;
  hllArr_ = newHllArr;
  invPow2Sum1_ = newInvPow2Sum1;
  invPow2Sum2_ = newInvPow2Sum2;
  hipEstAccum_ = newHipEstAccum;
  validBit_ = newValidBit;
}

  @Override
  public double update(byte[] key, byte[] identifier) {
    int outerIndex = index(key, tableEntries_);
    if (Util.isBitZero(validBit_, outerIndex)) {
      throw new SketchesArgumentException("key not recognized: "+
          Util.bytesToString(key, false, false, ":"));
    }
    boolean updated = insertIntoHllArray(key, identifier, hllArr_, outerIndex, k_); //update HLL array
    //TODO update the HIP registers
    //TODO get the estimate
    return 0;
  }

  @Override
  public double getEstimate(byte[] key) {
    //TODO
    return 0;
  }

  @Override
  void couponUpdate(byte[] key, int coupon) { //coupon either short or int
    //TODO
  }

  private static final int index(byte[] key, int tableEntries) {
    long keyHash = MurmurHash3.hash(key, 0L)[0] >>> 1;
    return (int)(keyHash % tableEntries);
  }

  /**
   * Returns a composit index: byte 0: longIndex; byte 1: shift; byte 2: hll value.
   * @param id the identifier to be hashed
   * @param k the power of 2 size of the HLL array
   * @return the composit index
   */
  private static final int hllIndex(byte[] id, int k) {
    long[] hash = MurmurHash3.hash(id, 0L);
    int rawHllAdd = (int)((hash[0] >>> 1) % k); //lower 64 for address
    int longIdx = (rawHllAdd/10) & 0XFF;
    int shift = ((rawHllAdd % 10) * 6) & 0XFF;
    int value = ((Long.numberOfLeadingZeros(hash[1])) & 0X3F) + 1;
    return longIdx | (shift << 8) | (value << 16);
  }

  private static final boolean insertIntoHllArray(
      byte[] key, byte[] identifier, long[] hllArr, int outerIndex, int k) {
    int compositIdx = hllIndex(identifier, k);
    int longIdx = compositIdx & 0XFF;
    int shift = (compositIdx >>> 8) & 0XFF;
    int value = (compositIdx >>> 16) & 0X3F;
    long container = hllArr[outerIndex + longIdx];
    int oldValue = (int)(container >>> shift) & 0X3F;
    if (value <= oldValue) return false;
    container &= ~(0X3FF << shift);
    container |=  value << shift;
    hllArr[outerIndex + longIdx] = container;
    return true;
  }
}
