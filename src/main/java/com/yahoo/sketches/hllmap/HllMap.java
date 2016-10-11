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
  private byte[] keysArr_; //keys of zero are allowed
  private long[] hllsArr_;
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
  }

  HllMap getInstance(int targetSizeBytes, int keySizeBytes, int k, float growthFactor) {
    if ((k != 1024) && (k != 512)) {
      throw new SketchesArgumentException("K must be either 1024 or 512: " + k);
    }
    if (growthFactor <= 1.0) {
      throw new SketchesArgumentException("growthFactor must be > 1.0: " + growthFactor);
    }

    int entries = (int)(targetSizeBytes / (keySizeBytes + (hllArrLongs_ * 8) + 24 + 0.125));
    tableEntries_ = Util.nextPrime(entries);
    capacityEntries_ = (int)(tableEntries_ * LOAD_FACTOR);
    curCountEntries_ = 0;
    growthFactor_ = growthFactor;

    keysArr_ = new byte[tableEntries_ * keySizeBytes_];
    hllsArr_ = new long[tableEntries_ * hllArrLongs_];
    invPow2SumHiArr_ = new double[tableEntries_];
    invPow2SumLoArr_ = new double[tableEntries_];
    hipEstAccumArr_ = new double[tableEntries_];
    validBitArr_ = new byte[tableEntries_/8 + 1];

    return new HllMap(keySizeBytes, k);
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

      byte[] key = Util.getBytes(keysArr_, keySizeBytes_, oldIndex); //get the old key
      int newIndex = outerSearchForEmpty(key, newTableEntries, newValidBit);
      Util.putBytes(newKeys, keySizeBytes_, newIndex, key); //put the key
      //put the rest of the row
      System.arraycopy(
          hllsArr_, oldIndex * hllArrLongs_, newHllArr, newIndex * hllArrLongs_, hllArrLongs_);
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
    hllsArr_ = newHllArr;
    invPow2SumHiArr_ = newInvPow2Sum1; //init to k
    invPow2SumLoArr_ = newInvPow2Sum2; //init to 0
    hipEstAccumArr_ = newHipEstAccum;  //init to 0
    validBitArr_ = newValidBit;
  }

  @Override
  public double getEstimate(byte[] key) {
    int index = outerSearchForKey(keysArr_, key, validBitArr_);
    if (index == -1) {
      return 0; //TODO Did we agree on this ?
    }
    return hipEstAccumArr_[index];
  }

  //This update only updates keys that alredy exist in the outer map
  @Override
  public double update(byte[] key, byte[] identifier) {
    int outerIndex = outerSearchForKey(keysArr_, key, validBitArr_);
    if (outerIndex == -1) {
      throw new SketchesArgumentException("key not found: "+
          Util.bytesToString(key, false, false, ":"));
    }
    //matching key found

    boolean updated = updateHll(hllsArr_, outerIndex, k_, identifier); //update HLL array
    if (updated) {
      //TODO update the HIP registers and estimate
    }

    return hipEstAccumArr_[outerIndex];
  }

  /**
   * Returns the outer address for the given key given the array of keys, if found.
   * Otherwise, returns -1;
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
      return -1;
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
        return -1;
      }

      if (Util.equals(key, 0, keyArr, index * keyLen, keyLen)) { //check for key match
        return index;
      }
    } while (index != loopIndex);
    return -1;
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

  /**
   * Returns the HLL array index and value as a 16-bit coupon given the identifier to be hashed
   * and k.
   * @param identifier the given identifier
   * @param k the size of the HLL array and cannot exceed 1024
   * @return the HLL array index and value
   */
  private static final int coupon16(byte[] identifier, int k) {
    long[] hash = MurmurHash3.hash(identifier, 0L);
    int hllIdx = (int) (((hash[0] >>> 1) % k) & 0X3FF); //hash[0] for 10-bit address
    int lz = Long.numberOfLeadingZeros(hash[1]);
    int value = ((lz > 62)? 62 : lz) + 1;
    return (value << 10) | hllIdx;
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

  private final boolean updateHll(
      long[] hllArr, int outerIndex, int k, byte[] identifier) {
    int coupon = coupon16(identifier, k);
    int hllIdx = coupon & 0X3FF;            //lower 10 bits
    int newValue = (coupon >>> 10) & 0X3F;  //upper 6 bits

    int shift = hllShift(hllIdx);
    int longIdx = hllLongIdx(hllIdx);

    long hllLong = hllArr[outerIndex + longIdx];
    int oldValue = (int)(hllLong >>> shift) & 0X3F;

    if (newValue <= oldValue) return false;
    // newValue > oldValue
    //update hipEstAccum before updating invPow2Sum
    double invPow2Sum = invPow2SumHiArr_[outerIndex];
    if (oldValue < 32) {

    } else {

    }
    if (newValue < 32) {

    } else {

    }


    hllLong &= ~(0X3FF << shift);  //zero out the 6-bit field
    hllLong |=  newValue << shift; //insert
    hllArr[outerIndex + longIdx] = hllLong;
    return true;
  }

  /****Testing***********/

  static void bktProbList(int hllValue) {
    //brute force
    Util.println("Brute force");
    double sum = 0;
    for (int i = 1; i <= hllValue; i++) {
      sum = 1.0 / Math.pow(2, i);
      Util.println(sum
          +"\t" + DoubleBits.doubleToBitString(sum)
          + "\t" + DoubleBits.base2Exponent(sum)
          + "\t" + DoubleBits.exponentToIntBits(sum));
    }
    Util.println("\nShifting");
    for (int i = 1; i <= hllValue; i++) {
      sum = bktProb(i);
      Util.println(sum
          +"\t" + DoubleBits.doubleToBitString(sum)
          + "\t" + DoubleBits.base2Exponent(sum)
          + "\t" + DoubleBits.exponentToIntBits(sum));
    }
  }

  static double bktProb(int hllValue) {
    long v = (1023L - hllValue) << 52;
    return Double.longBitsToDouble(v);
  }

  static void deltaBktProb(int v1, int v2) {
    double val1 = bktProb(v1);
    double val2 = bktProb(v2);

    double del = (v1 > v2)? val2 - val1 : (v1 == v2)? 0 : val1 - val2;
    printDbl(val1);
    printDbl(val2);
    printDbl(del);
  }

  static void printDbl(double d) {
    Util.println(d
        +"\t" + DoubleBits.doubleToBitString(d)
        + "\t" + DoubleBits.base2Exponent(d)
        + "\t" + DoubleBits.exponentToIntBits(d));
  }

  public static void main(String[] args) {
    bktProbList(64);
    //deltaBktProb(6, 4);
  }

  @Override
  int couponUpdate(byte[] key, short coupon) {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  MapValuesIterator getValuesIterator(byte[] key) {
    // TODO Auto-generated method stub
    return null;
  }

}
