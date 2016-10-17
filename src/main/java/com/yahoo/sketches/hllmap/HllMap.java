package com.yahoo.sketches.hllmap;

import com.yahoo.sketches.SketchesArgumentException;
import com.yahoo.sketches.hash.MurmurHash3;

/*
 * 3) Consider flexible compon size ?
 * 5) change to 2-bit row status codes ?
 */

//@SuppressWarnings("unused")
class HllMap extends Map {
  private static final double LOAD_FACTOR = 15.0/16.0;


  private final int k_;
  private final int hllArrLongs_;
  private final double entrySizeBytes_;

  private int tableEntries_; //Full size of the table
  private int capacityEntries_;  //max capacity entries defined by Load factor
  private int curCountEntries_;  //current count of valid entries
  private float growthFactor_;  //e.g., 1.2 to 2.0

  //Arrays
  private byte[] keysArr_; //keys of zero are allowed
  private long[] arrOfHllArr_;
  private double[] invPow2SumHiArr_;
  private double[] invPow2SumLoArr_;
  private double[] hipEstAccumArr_;
  private byte[] stateArr_;

  /**
   * Private constructor used to set all finals
   * @param keySizeBytes size of key in bytes
   * @param k size of HLL sketch
   */
  private HllMap(final int keySizeBytes, int k, int tableEntries) {
    super(keySizeBytes);
    k_ = k;
    hllArrLongs_ = k/10 + 1;
    double byteFraction = Math.ceil(tableEntries / 8.0) / tableEntries;
    entrySizeBytes_ = keySizeBytes + hllArrLongs_ * 8 + 24 + byteFraction;
  }

  public static HllMap getInstance(int tgtEntries, int keySizeBytes, int k, float growthFactor) {
    Util.checkK(k);
    Util.checkGrowthFactor(growthFactor);
    Util.checkTgtEntries(tgtEntries);
    int tableEntries = Util.nextPrime(tgtEntries);

    HllMap map = new HllMap(keySizeBytes, k, tableEntries);

    map.tableEntries_ = tableEntries;
    map.capacityEntries_ = (int)(tableEntries * LOAD_FACTOR);
    map.curCountEntries_ = 0;
    map.growthFactor_ = growthFactor;

    map.keysArr_ = new byte[tableEntries * map.keySizeBytes_];
    map.arrOfHllArr_ = new long[tableEntries * map.hllArrLongs_];
    map.invPow2SumHiArr_ = new double[tableEntries];
    map.invPow2SumLoArr_ = new double[tableEntries];
    map.hipEstAccumArr_ = new double[tableEntries];
    map.stateArr_ = new byte[(int) Math.ceil(tableEntries / 8.0)];
    return map;
  }

  @Override
  double update(byte[] key, int coupon) {
    int entryIndex = findOrInsertKey(key);
    return findOrInsertCoupon(entryIndex, coupon);

//    if (key == null) return Double.NaN;
//    //boolean updated = false;  may use later
//    int entryIndex = findKey(keysArr_, key, tableEntries_, stateArr_);
//    if (entryIndex < 0) {
//      //not found, initialize new row
//      int emptyEntryIndex = ~entryIndex;
//      System.arraycopy(key, 0, keysArr_, emptyEntryIndex * keySizeBytes_, keySizeBytes_);
//      Util.setBitToOne(stateArr_, emptyEntryIndex);
//      invPow2SumHiArr_[emptyEntryIndex] = k_;
//      invPow2SumLoArr_[emptyEntryIndex] = 0;
//      hipEstAccumArr_[emptyEntryIndex] = 0;
//      ///updated =
////      updateHll(emptyEntryIndex, coupon); //update HLL array, updates HIP
////      double est = hipEstAccumArr_[emptyEntryIndex];
//
//      curCountEntries_++;
//      if (curCountEntries_ > capacityEntries_) {
//        growSize();
//      }
//      //return est;
//    }
//    //matching key found
//    //updated =
//    updateHll(entryIndex, coupon); //update HLL array, updates HIP
//    return hipEstAccumArr_[entryIndex];
  }

  @Override
  double getEstimate(byte[] key) {
    if (key == null) return Double.NaN;
    int entryIndex = findKey(keysArr_, key, tableEntries_, stateArr_);
    if (entryIndex < 0) {
      return 0;
    }
    return hipEstAccumArr_[entryIndex];
  }

  void updateEstimate(int entryIndex, double estimate) {
    hipEstAccumArr_[entryIndex] = estimate;
  }

  private final void growSize() {
    int newTableEntries = Util.nextPrime((int)(tableEntries_ * growthFactor_));
    int newCapacityEntries = (int)(newTableEntries * LOAD_FACTOR);

    byte[] newKeysArr = new byte[newTableEntries * keySizeBytes_];
    long[] newArrOfHllArr = new long[newTableEntries * hllArrLongs_];
    double[] newInvPow2Sum1 = new double[newTableEntries];
    double[] newInvPow2Sum2 = new double[newTableEntries];
    double[] newHipEstAccum = new double[newTableEntries];
    byte[] newStateArr = new byte[(int) Math.ceil(newTableEntries / 8.0)];

    for (int oldIndex = 0; oldIndex < tableEntries_; oldIndex++) {
      if (Util.isBitZero(stateArr_, oldIndex)) continue;
      byte[] key = new byte[keySizeBytes_];
      System.arraycopy(keysArr_, oldIndex * keySizeBytes_, key, 0, keySizeBytes_); //get old key
      int newIndex = findEmpty(key, newTableEntries, newStateArr);

      System.arraycopy(key, 0, newKeysArr, newIndex * keySizeBytes_, keySizeBytes_); //put key
      //put the rest of the row
      System.arraycopy(
          arrOfHllArr_, oldIndex * hllArrLongs_, newArrOfHllArr, newIndex * hllArrLongs_, hllArrLongs_);
      newInvPow2Sum1[newIndex] = invPow2SumHiArr_[oldIndex];
      newInvPow2Sum2[newIndex] = invPow2SumLoArr_[oldIndex];
      newHipEstAccum[newIndex] = hipEstAccumArr_[oldIndex];
      Util.setBitToOne(newStateArr, newIndex);
    }
    //restore into sketch
    tableEntries_ = newTableEntries;
    capacityEntries_ = newCapacityEntries;
    //curCountEntries_, growthFactor_  unchanged

    keysArr_ = newKeysArr;
    arrOfHllArr_ = newArrOfHllArr;
    invPow2SumHiArr_ = newInvPow2Sum1; //init to k
    invPow2SumLoArr_ = newInvPow2Sum2; //init to 0
    hipEstAccumArr_ = newHipEstAccum;  //init to 0
    stateArr_ = newStateArr;
  }

  int findOrInsertKey(final byte[] key) {
    int entryIndex = findKey(keysArr_, key, tableEntries_, stateArr_);
    if (entryIndex < 0) { //key not found
    //not found, initialize new row
      entryIndex = ~entryIndex;
      System.arraycopy(key, 0, keysArr_, entryIndex * keySizeBytes_, keySizeBytes_);
      Util.setBitToOne(stateArr_, entryIndex);
      invPow2SumHiArr_[entryIndex] = k_;
      invPow2SumLoArr_[entryIndex] = 0;
      hipEstAccumArr_[entryIndex] = 0;
      curCountEntries_++;
      if (curCountEntries_ > capacityEntries_) {
        growSize();
      }
    }
    return entryIndex;
  }

  double findOrInsertCoupon(final int entryIndex, final int coupon) {
    updateHll(entryIndex, coupon); //update HLL array, updates HIP
    return hipEstAccumArr_[entryIndex];
  }

  /**
   * Returns the entry index for the given key given the array of keys, if found.
   * Otherwise, returns the one's complement of first empty entry found;
   * @param keyArr the given array of keys
   * @param key the key to search for
   * @param tableEntries the total number of entries in the table.
   * @param stateArr the bit vector that holds valid/empty state of each entry
   * @return the entry index of the given key, or the one's complement of the index if not found.
   */
  private static final int findKey(byte[] keyArr, byte[] key, int tableEntries, byte[] stateArr) {
    int keyLen = key.length;
    final long[] hash = MurmurHash3.hash(key, SEED);
    int entryIndex  = getIndex(hash[0], tableEntries);
    final int stride = getStride(hash[1], tableEntries);
    final int loopIndex = entryIndex;

    do {
      if (Util.isBitZero(stateArr, entryIndex)) { //check if slot is empty
        return ~entryIndex;
      }
      if (arraysEqual(key, 0, keyArr, entryIndex * keyLen, keyLen)) { //check for key match
        return entryIndex;
      }
      entryIndex = (entryIndex + stride) % tableEntries;
    } while (entryIndex != loopIndex);
    throw new SketchesArgumentException("Key not found and no empty slots!");
  }

  /**
   * Find the first empty slot for the given key.
   * Only used by growSize, where it is known that the key does not exist in the table.
   * Throws an exception if no empty slots.
   * @param key the given key
   * @param tableEntries prime size of table
   * @param stateArr the valid bit array
   * @return the first empty slot for the given key
   */
  private static final int findEmpty(byte[] key, int tableEntries, byte[] stateArr) {
    final long[] hash = MurmurHash3.hash(key, SEED);
    int entryIndex  = getIndex(hash[0], tableEntries);
    final int stride = getStride(hash[1], tableEntries);
    final int loopIndex = entryIndex;

    do {
      if (Util.isBitZero(stateArr, entryIndex)) { //check if slot is empty
        return entryIndex;
      }
      entryIndex = (entryIndex + stride) % tableEntries;
    } while (entryIndex != loopIndex);
    throw new SketchesArgumentException("No empty slots.");
  }

  @Override
  public double getEntrySizeBytes() {
    return entrySizeBytes_;
  }

  @Override
  public int getTableEntries() {
    return tableEntries_;
  }

  @Override
  public int getCapacityEntries() {
    return capacityEntries_;
  }

  @Override
  public int getCurrentCountEntries() {
    return curCountEntries_;
  }

  @Override
  public int getMemoryUsageBytes() {
    int arrays = (int) Math.ceil(entrySizeBytes_ * tableEntries_);
    int other = 4 * 6 + 8;
    return arrays + other;
  }

  //These methods are specifically tied to the HLL array layout

  /**
   * Returns the long that contains the hll index.
   * This is a function of the HLL array storage layout.
   * @param hllIdx the given hll index
   * @return the long that contains the hll index
   */
  private static final int hllLongIdx(int hllIdx) {
    return hllIdx/10;
  }

  /**
   * Returns the long shift for the hll value index.
   * This is a function of the HLL array storage layout.
   * @param hllIdx the given hll index
   * @return the long shift for the hll index
   */
  private static final int hllShift(int hllIdx) {
    return ((hllIdx % 10) * 6) & SIX_BIT_MASK;
  }

  private final boolean updateHll(int entryIndex, int coupon) {
    int hllIdx = coupon & TEN_BIT_MASK;             //lower 10 bits
    int newValue = (coupon >>> 10) & SIX_BIT_MASK;  //upper 6 bits

    int shift = hllShift(hllIdx);
    int longIdx = hllLongIdx(hllIdx);

    long hllLong = arrOfHllArr_[entryIndex * hllArrLongs_ + longIdx];
    int oldValue = (int)(hllLong >>> shift) & SIX_BIT_MASK;
    if (newValue <= oldValue) return false;
    // newValue > oldValue

    //update hipEstAccum BEFORE updating invPow2Sum
    double invPow2Sum = invPow2SumHiArr_[entryIndex] + invPow2SumLoArr_[entryIndex];
    double oneOverQ = k_ / invPow2Sum;
    hipEstAccumArr_[entryIndex] += oneOverQ;

    //update invPow2Sum
    if (oldValue < 32) { invPow2SumHiArr_[entryIndex] -= Util.invPow2(oldValue); }
    else               { invPow2SumLoArr_[entryIndex] -= Util.invPow2(oldValue); }
    if (newValue < 32) { invPow2SumHiArr_[entryIndex] += Util.invPow2(newValue); }
    else               { invPow2SumLoArr_[entryIndex] += Util.invPow2(newValue); }

    //insert the new value
    hllLong &= ~(0X3FL << shift);  //zero out the 6-bit field
    hllLong |=  ((long)newValue) << shift; //insert
    arrOfHllArr_[entryIndex * hllArrLongs_ + longIdx] = hllLong;
    return true;
  }

//  static int getHllValue(long[] arrOfHllArr, int entryIndex, int hllIdx) {
//    int shift = hllShift(hllIdx);
//    int longIdx = hllLongIdx(hllIdx);
//    long hllLong = arrOfHllArr[entryIndex + longIdx];
//    return (int)(hllLong >>> shift) & SIX_BIT_MASK;
//  }

//  /****Testing***********/
//
//  void printEntry(byte[] key) {
//    if (key.length != 4) throw new SketchesArgumentException("Key must be 4 bytes");
//    int keyInt = bytesToInt(key);
//    StringBuilder sb = new StringBuilder();
//    int entryIndex = outerSearchForKey(keysArr_, key, validBitArr_);
//    if (entryIndex < 0) throw new SketchesArgumentException("Not Found: " + keyInt);
//    sb.append(keyInt).append(TAB);
//    sb.append(Util.isBitOne(validBitArr_, entryIndex)? "1" : "0").append(TAB);
//    sb.append(Double.toString(invPow2SumHiArr_[entryIndex])).append(TAB);
//    sb.append(Double.toString(invPow2SumLoArr_[entryIndex])).append(TAB);
//    sb.append(hipEstAccumArr_[entryIndex]).append(LS);
//
//    sb.append(hllToString(arrOfHllArr_, entryIndex, k_));
//    Util.println(sb.toString());
//  }
//
//  String hllToString(long[] arrOfhllArr, int entryIndex, int k) {
//    StringBuilder sb = new StringBuilder();
//    for (int i = 0; i < k_-1; i++) {
//      int v = getHllValue(arrOfHllArr_, entryIndex, i);
//      sb.append(v).append(":");
//    }
//    int v = getHllValue(arrOfHllArr_, entryIndex, k_ - 1);
//    sb.append(v);
//    return sb.toString();
//  }

}
