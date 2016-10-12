package com.yahoo.sketches.hllmap;

import static com.yahoo.sketches.hllmap.MapTestingUtil.LS;
import static com.yahoo.sketches.hllmap.MapTestingUtil.TAB;
import static com.yahoo.sketches.hllmap.MapTestingUtil.bytesToInt;

import com.yahoo.sketches.SketchesArgumentException;
import com.yahoo.sketches.hash.MurmurHash3;

@SuppressWarnings("unused")
class HllMap extends Map {
  private static final double LOAD_FACTOR = 15.0/16.0;
  private final int k_;
  private final int hllArrLongs_;
  private final float entrySizeBytes_;

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
  private byte[] validBitArr_;

  /**
   * Private constructor used to set all finals
   * @param keySizeBytes size of key in bytes
   * @param k size of HLL sketch
   */
  private HllMap(final int keySizeBytes, int k) {
    super(keySizeBytes);
    k_ = k;
    hllArrLongs_ = k/10 + 1;
    entrySizeBytes_ = (float) (keySizeBytes + hllArrLongs_ * 8 + 3 * 8 + 0.125);
  }

  HllMap getInstance(int tgtEntries, int keySizeBytes, int k, float growthFactor) {
    if (!com.yahoo.sketches.Util.isPowerOf2(k) || (k > 1024) || (k < 16)) {
      throw new SketchesArgumentException("K must be power of 2 and (16 <= k <= 1024): " + k);
    }
    if (growthFactor <= 1.0) {
      throw new SketchesArgumentException("growthFactor must be > 1.0: " + growthFactor);
    }

    HllMap map = new HllMap(keySizeBytes, k);

    int entries = (int)Math.ceil(tgtEntries / LOAD_FACTOR);
    tableEntries_ = Util.nextPrime(entries);
    capacityEntries_ = (int)(tableEntries_ * LOAD_FACTOR);
    curCountEntries_ = 0;
    growthFactor_ = growthFactor;

    keysArr_ = new byte[tableEntries_ * keySizeBytes_];
    arrOfHllArr_ = new long[tableEntries_ * hllArrLongs_];
    invPow2SumHiArr_ = new double[tableEntries_];
    invPow2SumLoArr_ = new double[tableEntries_];
    hipEstAccumArr_ = new double[tableEntries_];
    validBitArr_ = new byte[tableEntries_/8 + 1];

    return map;
  }

  public float getEntrySizeBytes() {
    return entrySizeBytes_;
  }

  public int getCapacityEntries() {
    return capacityEntries_;
  }

  public int getSizeOfArrays() {
    return keysArr_.length + arrOfHllArr_.length * 8 + tableEntries_ * 24 + validBitArr_.length;
  }

  @Override
  double getEstimate(byte[] key) {
    if (key == null) return Double.NaN;
    int index = outerSearchForKey(keysArr_, key, validBitArr_);
    if (index < 0) {
      return 0;
    }
    return hipEstAccumArr_[index];
  }

  @Override
  double update(byte[] key, int coupon) {
    if (key == null) return Double.NaN;
    boolean updated;
    int outerIndex = outerSearchForKey(keysArr_, key, validBitArr_);
    if (outerIndex < 0) {
      //not found, initialize new row
      int emptyOuterIndex = ~outerIndex;
      System.arraycopy(key, 0, keysArr_, emptyOuterIndex * keySizeBytes_, keySizeBytes_);
      Util.setBitToOne(validBitArr_, emptyOuterIndex);
      invPow2SumHiArr_[emptyOuterIndex] = k_;
      invPow2SumLoArr_[emptyOuterIndex] = 0;
      hipEstAccumArr_[emptyOuterIndex] = 0;
      updated = updateHll(emptyOuterIndex, coupon); //update HLL array
      curCountEntries_++;
      if (curCountEntries_ > capacityEntries_) {
        growSize();
      }
      return 1.0;
    }
    //matching key found
    updated = updateHll(outerIndex, coupon); //update HLL array
    return hipEstAccumArr_[outerIndex];
  }

  private final void growSize() {
    int newTableEntries = Util.nextPrime((int)(tableEntries_ * growthFactor_));
    int newCapacityEntries = (int)(newTableEntries * LOAD_FACTOR);

    byte[] newKeys = new byte[newTableEntries * keySizeBytes_];
    long[] newHllArr = new long[newTableEntries * hllArrLongs_];
    double[] newInvPow2Sum1 = new double[newTableEntries];
    double[] newInvPow2Sum2 = new double[newTableEntries];
    double[] newHipEstAccum = new double[newTableEntries];
    byte[] newValidBit = new byte[newTableEntries/8 + 1];

    for (int oldIndex = 0; oldIndex < tableEntries_; oldIndex++) {
      if (Util.isBitZero(validBitArr_, oldIndex)) continue;
      byte[] key = new byte[keySizeBytes_];
      System.arraycopy(keysArr_, oldIndex * keySizeBytes_, key, 0, keySizeBytes_); //get old key
      int newIndex = outerSearchForEmpty(key, newTableEntries, newValidBit); //TODO

      System.arraycopy(key, 0, keysArr_, newIndex * keySizeBytes_, keySizeBytes_); //put key
      //put the rest of the row
      System.arraycopy(
          arrOfHllArr_, oldIndex * hllArrLongs_, newHllArr, newIndex * hllArrLongs_, hllArrLongs_);
      newInvPow2Sum1[newIndex] = invPow2SumHiArr_[oldIndex];
      newInvPow2Sum2[newIndex] = invPow2SumLoArr_[oldIndex];
      newHipEstAccum[newIndex] = hipEstAccumArr_[oldIndex];
      Util.setBitToOne(newValidBit, oldIndex);
    }
    //restore into sketch
    tableEntries_ = newTableEntries;
    capacityEntries_ = (int)(tableEntries_ * LOAD_FACTOR);
    //curCountEntries_, growthFactor_  unchanged

    keysArr_ = newKeys;
    arrOfHllArr_ = newHllArr;
    invPow2SumHiArr_ = newInvPow2Sum1; //init to k
    invPow2SumLoArr_ = newInvPow2Sum2; //init to 0
    hipEstAccumArr_ = newHipEstAccum;  //init to 0
    validBitArr_ = newValidBit;
  }

  /**
   * Returns the outer address for the given key given the array of keys, if found.
   * Otherwise, returns address twos complement of first empty slot found;
   * @param keyArr the given array of keys
   * @param key the key to search for
   * @return the address of the given key, or -1 if not found
   */
  private static final int outerSearchForKey(byte[] keyArr, byte[] key, byte[] validBit) {
    final int keyLen = key.length;
    final int tableEntries = keyArr.length/keyLen;

    final long[] hash = MurmurHash3.hash(key, 0L);
    int index  = (int) ((hash[0] >>> 1) % tableEntries);

    if (Util.isBitZero(validBit, index)) { //check if slot is empty
      return ~index;
    }

    if (Util.equals(key, 0, keyArr, index * keyLen, keyLen)) { //check for key match
      return index;
    }
    //keep searching
    final int stride = (int) ((hash[1] >>> 1) % (tableEntries - 2L) + 1L);
    final int loopIndex = index;

    do {
      index -= stride;
      if (index < 0) {
        index += tableEntries;
      }
      if (Util.isBitZero(validBit, index)) { //check if slot is empty
        return ~index;
      }

      if (Util.equals(key, 0, keyArr, index * keyLen, keyLen)) { //check for key match
        return index;
      }
    } while (index != loopIndex);
    return ~index;
  }

  /**
   * Find an empty slot for the given key. Throws an exception if no empty slots.
   * @param key the given key
   * @param tableEntries prime size of table
   * @param validBit the valid bit array
   * @return an empty slot for the given key
   */
  private static final int outerSearchForEmpty(byte[] key, int tableEntries, byte[] validBit) {
    final int keyLen = key.length;

    final long[] hash = MurmurHash3.hash(key, 0L);
    int index  = (int) ((hash[0] >>> 1) % tableEntries);

    if (Util.isBitZero(validBit, index)) { //check if slot is empty
      return index;
    }
    //keep searching
    final int stride = (int) ((hash[1] >>> 1) % (tableEntries - 2L) + 1L);
    final int loopIndex = index;

    do {
      index -= stride;
      if (index < 0) {
        index += tableEntries;
      }
      if (Util.isBitZero(validBit, index)) { //check if slot is empty
        return index;
      }
    } while (index != loopIndex);
    throw new SketchesArgumentException("No empty slots.");
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
   * Returns the long shift for the hll index.
   * This is a function of the HLL array storage layout.
   * @param hllIdx the given hll index
   * @return the long shift for the hll index
   */
  private static final int hllShift(int hllIdx) {
    return ((hllIdx % 10) * 6) & 0XFF;
  }

  private final boolean updateHll(int outerIndex, int coupon) {
    int hllIdx = coupon & 0X3FF;            //lower 10 bits
    int newValue = (coupon >>> 10) & 0X3F;  //upper 6 bits

    int shift = hllShift(hllIdx);
    int longIdx = hllLongIdx(hllIdx);

    long hllLong = arrOfHllArr_[outerIndex + longIdx];
    int oldValue = (int)(hllLong >>> shift) & 0X3F;

    if (newValue <= oldValue) return false;
    // newValue > oldValue

    //update hipEstAccum BEFORE updating invPow2Sum
    double invPow2Sum = invPow2SumHiArr_[outerIndex] + invPow2SumLoArr_[outerIndex];
    double oneOverQ = k_ / invPow2Sum;
    hipEstAccumArr_[outerIndex] += oneOverQ;

    //update invPow2Sum
    if (oldValue < 32) { invPow2SumHiArr_[outerIndex] -= Util.invPow2(oldValue); }
    else               { invPow2SumLoArr_[outerIndex] -= Util.invPow2(oldValue); }
    if (newValue < 32) { invPow2SumHiArr_[outerIndex] += Util.invPow2(newValue); }
    else               { invPow2SumLoArr_[outerIndex] += Util.invPow2(newValue); }

    //insert the new value
    hllLong &= ~(0X3FF << shift);  //zero out the 6-bit field
    hllLong |=  newValue << shift; //insert
    arrOfHllArr_[outerIndex + longIdx] = hllLong;
    return true;
  }

  /****Testing***********/

  void printEntry1(byte[] key) {
    if (key.length != 4) throw new SketchesArgumentException("Key must be 4 bytes");
    int keyInt = bytesToInt(key);
    StringBuilder sb = new StringBuilder();
    int outerIndex = outerSearchForKey(keysArr_, key, validBitArr_);
    if (outerIndex < 0) throw new SketchesArgumentException("Not Found: " + keyInt);
    sb.append(keyInt).append(TAB);
    sb.append(Util.isBitOne(validBitArr_, outerIndex)? "1" : "0").append(TAB);
    sb.append(Double.toHexString(invPow2SumHiArr_[outerIndex])).append(TAB);
    sb.append(Double.toHexString(invPow2SumLoArr_[outerIndex])).append(TAB);
    sb.append(hipEstAccumArr_[outerIndex]).append(LS);

    //sb.append()

  }

  String hllToString(long[] hllArrLongs) {
    StringBuilder sb = new StringBuilder();
    for (int i=0; i< k_; i++) {

    }
    return null;
  }


  public static void main(String[] args) {
    //bktProbList(64);
    //deltaBktProb(6, 4);
  }

}
