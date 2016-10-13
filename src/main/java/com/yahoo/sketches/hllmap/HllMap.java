package com.yahoo.sketches.hllmap;

import static com.yahoo.sketches.hllmap.MapTestingUtil.LS;
import static com.yahoo.sketches.hllmap.MapTestingUtil.TAB;
import static com.yahoo.sketches.hllmap.MapTestingUtil.bytesToInt;
import static com.yahoo.sketches.hllmap.MapTestingUtil.intToBytes;

import com.yahoo.sketches.SketchesArgumentException;
import com.yahoo.sketches.hash.MurmurHash3;

@SuppressWarnings("unused")
class HllMap extends Map {
  private static final double LOAD_FACTOR = 15.0/16.0;
  private static final int SIX_BIT_MASK = 0X3F; // 6 bits
  private static final int TEN_BIT_MASK = 0X3FF; //10 bits
  private static final int EIGHT_BIT_MASK = 0XFF;

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

  public static HllMap getInstance(int tgtEntries, int keySizeBytes, int k, float growthFactor) {
    if (!com.yahoo.sketches.Util.isPowerOf2(k) || (k > 1024) || (k < 16)) {
      throw new SketchesArgumentException("K must be power of 2 and (16 <= k <= 1024): " + k);
    }
    if (growthFactor <= 1.0) {
      throw new SketchesArgumentException("growthFactor must be > 1.0: " + growthFactor);
    }
    if (tgtEntries < 16) {
      throw new SketchesArgumentException("tgtEntries must be >= 16");
    }


    HllMap map = new HllMap(keySizeBytes, k);

    int entries = (int)Math.ceil(tgtEntries / LOAD_FACTOR);

    int tableEntries = Util.nextPrime(entries);
    map.tableEntries_ = tableEntries;
    map.capacityEntries_ = (int)(tableEntries * LOAD_FACTOR);
    map.curCountEntries_ = 0;
    map.growthFactor_ = growthFactor;

    map.keysArr_ = new byte[tableEntries * map.keySizeBytes_];
    map.arrOfHllArr_ = new long[tableEntries * map.hllArrLongs_];
    map.invPow2SumHiArr_ = new double[tableEntries];
    map.invPow2SumLoArr_ = new double[tableEntries];
    map.hipEstAccumArr_ = new double[tableEntries];
    map.validBitArr_ = new byte[tableEntries/8 + 1];

    return map;
  }

  public float getEntrySizeBytes() {
    return entrySizeBytes_;
  }

  public int getCapacityEntries() {
    return capacityEntries_;
  }

  public int getTableEntries() {
    return tableEntries_;
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
    boolean updated = false;
    int outerIndex = outerSearchForKey(keysArr_, key, validBitArr_);
    if (outerIndex < 0) {
      //not found, initialize new row
      int emptyOuterIndex = ~outerIndex;
      System.arraycopy(key, 0, keysArr_, emptyOuterIndex * keySizeBytes_, keySizeBytes_);
      Util.setBitToOne(validBitArr_, emptyOuterIndex);
      invPow2SumHiArr_[emptyOuterIndex] = k_;
      invPow2SumLoArr_[emptyOuterIndex] = 0;
      hipEstAccumArr_[emptyOuterIndex] = 0;
      updated = updateHll(emptyOuterIndex, coupon); //update HLL array, updates HIP
      double est = hipEstAccumArr_[emptyOuterIndex];

      curCountEntries_++;
      if (curCountEntries_ > capacityEntries_) {
        growSize();
      }

      //print("; "+updated + " ");
      return est;
    }
    //matching key found
    updated = updateHll(outerIndex, coupon); //update HLL array
    //print("; "+updated + " ");
    return hipEstAccumArr_[outerIndex];
  }

  void updateEstimate(byte[] key, double estimate) {
    int outerIndex = outerSearchForKey(keysArr_, key, validBitArr_);
    if (outerIndex < 0) {
      throw new SketchesArgumentException("Key not found. ");
    }
    hipEstAccumArr_[outerIndex] = estimate;
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
      int newIndex = outerSearchForEmpty(key, newTableEntries, newValidBit);

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
    return ((hllIdx % 10) * 6) & SIX_BIT_MASK;
  }

  private final boolean updateHll(int outerIndex, int coupon) {
    int hllIdx = coupon & TEN_BIT_MASK;            //lower 10 bits
    int newValue = (coupon >>> 10) & SIX_BIT_MASK;  //upper 6 bits

    int shift = hllShift(hllIdx);
    int longIdx = hllLongIdx(hllIdx);

    long hllLong = arrOfHllArr_[outerIndex + longIdx];
    int oldValue = (int)(hllLong >>> shift) & SIX_BIT_MASK;
    //print("hllIdx: " +hllIdx + ", newV: " + newValue + ", oldV: " + oldValue);
    //print("; invPwr2Sum: "+(invPow2SumHiArr_[outerIndex] + invPow2SumLoArr_[outerIndex]));
    if (newValue <= oldValue) return false;
    // newValue > oldValue

    //update hipEstAccum BEFORE updating invPow2Sum
    double invPow2Sum = invPow2SumHiArr_[outerIndex] + invPow2SumLoArr_[outerIndex];
    double oneOverQ = k_ / invPow2Sum;
    hipEstAccumArr_[outerIndex] += oneOverQ;
    //print("; invPwr2Sum: "+invPow2Sum); //TODO

    //update invPow2Sum
    if (oldValue < 32) { invPow2SumHiArr_[outerIndex] -= Util.invPow2(oldValue); }
    else               { invPow2SumLoArr_[outerIndex] -= Util.invPow2(oldValue); }
    if (newValue < 32) { invPow2SumHiArr_[outerIndex] += Util.invPow2(newValue); }
    else               { invPow2SumLoArr_[outerIndex] += Util.invPow2(newValue); }

    //insert the new value
    hllLong &= ~(0X3FL << shift);  //zero out the 6-bit field
    hllLong |=  ((long)newValue) << shift; //insert
    arrOfHllArr_[outerIndex + longIdx] = hllLong;
    return true;
  }

  static int getHllValue(long[] arrOfHllArr, int outerIndex, int hllIdx) {
    int shift = hllShift(hllIdx);
    int longIdx = hllLongIdx(hllIdx);
    long hllLong = arrOfHllArr[outerIndex + longIdx];
    return (int)(hllLong >>> shift) & SIX_BIT_MASK;
  }

  /****Testing***********/

  void printEntry(byte[] key) {
    if (key.length != 4) throw new SketchesArgumentException("Key must be 4 bytes");
    int keyInt = bytesToInt(key);
    StringBuilder sb = new StringBuilder();
    int outerIndex = outerSearchForKey(keysArr_, key, validBitArr_);
    if (outerIndex < 0) throw new SketchesArgumentException("Not Found: " + keyInt);
    sb.append(keyInt).append(TAB);
    sb.append(Util.isBitOne(validBitArr_, outerIndex)? "1" : "0").append(TAB);
    sb.append(Double.toString(invPow2SumHiArr_[outerIndex])).append(TAB);
    sb.append(Double.toString(invPow2SumLoArr_[outerIndex])).append(TAB);
    sb.append(hipEstAccumArr_[outerIndex]).append(LS);

    sb.append(hllToString(arrOfHllArr_, outerIndex, k_));
    Util.println(sb.toString());
  }

  String hllToString(long[] arrOfhllArr, int outerIndex, int k) {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < k_-1; i++) {
      int v = getHllValue(arrOfHllArr_, outerIndex, i);
      sb.append(v).append(":");
    }
    int v = getHllValue(arrOfHllArr_, outerIndex, k_ - 1);
    sb.append(v);
    return sb.toString();
  }

  private static void test1() {
    int k = 512;
    int u = 100000;
    int initEntries = 16;
    int keySize = 4;
    float rf = (float)1.2;
    HllMap map = HllMap.getInstance(initEntries, keySize, k, rf);
    println("Entry bytes   : " + map.getEntrySizeBytes());
    println("Capacity      : " + map.getCapacityEntries());
    println("Table Entries : " + map.getTableEntries());
    println("Est Arr Size  : " + (map.getEntrySizeBytes() * map.getTableEntries()));
    println("Size of Arrays: "+ map.getSizeOfArrays());

    byte[] key = new byte[4];
    byte[] id = new byte[4];
    double est;
    key = intToBytes(1, key);
    for (int i=1; i<= u; i++) {
      id = intToBytes(i, id);
      int coupon = Util.coupon16(id, k);
      est = map.update(key, coupon);
      if (i % 1000 == 0) {
        double err = (est/i -1.0) * 100;
        String eStr = String.format("%.3f%%", err);
        println("i: "+i + "\t Est: " + est + TAB + eStr);
      }
    }
    println("Table Entries : " + map.getTableEntries());
    println("Cur Count     : " + map.curCountEntries_);
    println("RSE           : " + (1/Math.sqrt(k)));

    //map.printEntry(key);
  }

  public static void main(String[] args) {
    test1();
  }
  static void println(String s) { System.out.println(s); }
  static void print(String s) { System.out.print(s); }
}
