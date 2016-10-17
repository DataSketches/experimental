package com.yahoo.sketches.hllmap;

import static com.yahoo.sketches.Util.checkIfPowerOf2;

import java.util.Arrays;

import com.yahoo.sketches.SketchesArgumentException;
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

  private static final double OUTER_LOAD_FACTOR = 15.0/16.0;
  private static final double INNER_LOAD_FACTOR = 0.75;
  private static final byte DELETED_KEY_MARKER = (byte) 255;
  private static final int BYTE_MASK = 0XFF;

  private final int k_;
  private final int maxCouponsPerKey_;
  private final int capacityCouponsPerKey_;
  private final int entrySizeBytes_;

  private int tableEntries_;
  private int capacityEntries_;

  private int numActiveKeys_;
  private int numDeletedKeys_;
  private float growthFactor_;

  //Arrays
  private byte[] keysArr_;
  private short[] couponMapArr_;
  private byte[] curCountsArr_; //also acts as a stateArr: 0 empty, 255 deleted
  private float[] invPow2SumArr_;
  private float[] hipEstAccumArr_;

  // qt <- k; hip <- 0;
  // hip +=  k/qt; qt -= 1/2^(val);

  private CouponHashMap(final int keySizeBytes, int k, int maxCouponsPerKey) {
    super(keySizeBytes);
    k_ = k;
    maxCouponsPerKey_ = maxCouponsPerKey;
    capacityCouponsPerKey_ = (int)(maxCouponsPerKey * INNER_LOAD_FACTOR);
    entrySizeBytes_ = keySizeBytes + maxCouponsPerKey + 1 + 8;
  }

  static CouponHashMap getInstance(final int tgtEntries, final int keySizeBytes,
      final int maxCouponsPerKey, final int k, float growthFactor) {
    checkMaxCouponsPerKey(maxCouponsPerKey, k);
    Util.checkGrowthFactor(growthFactor); //optional
    Util.checkTgtEntries(tgtEntries); //optional
    CouponHashMap map = new CouponHashMap(keySizeBytes, k, maxCouponsPerKey);

    int tableEntries = Util.nextPrime(tgtEntries);
    map.tableEntries_ = tableEntries;
    map.capacityEntries_ = (int)(tableEntries * OUTER_LOAD_FACTOR);
    map.numActiveKeys_ = 0;
    map.numDeletedKeys_ = 0;
    map.growthFactor_ = growthFactor;

    map.keysArr_ = new byte[tableEntries * keySizeBytes];
    map.couponMapArr_ = new short[tableEntries * maxCouponsPerKey];
    map.curCountsArr_ = new byte[tableEntries];
    map.invPow2SumArr_ = new float[tableEntries];
    map.hipEstAccumArr_ = new float[tableEntries];
    return map;
  }

  // qt <- k; hip <- 0;
  // hip +=  k/qt; qt -= 1/2^(val);
  @Override
  double update(final byte[] key, final int coupon) {
    int entryIndex = findOrInsertKey(key);
    return findOrInsertCoupon(entryIndex, (short)coupon); //negative when time to promote
  }

  @Override
  void deleteKey(final int entryIndex) {
    clearCouponArea(entryIndex);
    curCountsArr_[entryIndex] = DELETED_KEY_MARKER;
    numDeletedKeys_++;
  }

  void clearCouponArea(final int entryIndex) {
    final int couponAreaIndex = entryIndex * maxCouponsPerKey_;
    for (int i = 0; i < maxCouponsPerKey_; i++) {
      couponMapArr_[couponAreaIndex + i] = 0;
    }
  }

  @Override
  void updateEstimate(final int entryIndex, final double estimate) {
    if (entryIndex < 0) {
      throw new SketchesArgumentException("Key not found.");
    }
    //double curEst = hipEstAccumArr_[index];
    hipEstAccumArr_[entryIndex] = (float) estimate;
  }

  @Override
  double getEstimate(final byte[] key) {
    final int index = findKey(key);
    if (index < 0) return 0;
    //double est = getCouponCount(index);
    double est = hipEstAccumArr_[index];
    return est;
  }

  // returns index if the given key is found
  // if not found, returns one's complement index of an empty slot for insertion
  // which may be over a deleted key
  @Override
  int findKey(final byte[] key) {
    final long[] hash = MurmurHash3.hash(key, SEED);
    int entryIndex = getIndex(hash[0], tableEntries_);
    int firstDeletedIndex = -1;
    while (curCountsArr_[entryIndex] != 0) {
      if (curCountsArr_[entryIndex] == DELETED_KEY_MARKER) {
        if (firstDeletedIndex == -1) {
          firstDeletedIndex = entryIndex;
        }
      } else if (Map.arraysEqual(keysArr_, entryIndex * keySizeBytes_, key, 0, keySizeBytes_)) {
        return entryIndex;
      }
      entryIndex = (entryIndex + getStride(hash[1], tableEntries_)) % tableEntries_;
    }
    return firstDeletedIndex == -1 ? ~entryIndex : ~firstDeletedIndex;
  }

  @Override
  int findOrInsertKey(final byte[] key) {
    int entryIndex = findKey(key);
    if (entryIndex < 0) { //key not found
      entryIndex = ~entryIndex;
      //insert new key
      System.arraycopy(key, 0, keysArr_, entryIndex * keySizeBytes_, keySizeBytes_);
      curCountsArr_[entryIndex]++;
      //initialize HIP:  qt <- k; hip <- 0;
      invPow2SumArr_[entryIndex] = k_;
      hipEstAccumArr_[entryIndex] = 0;
      numActiveKeys_++;
      if (numActiveKeys_ + numDeletedKeys_ > capacityEntries_) {
        resize();
        entryIndex = findKey(key);
      }
    }
    return entryIndex;
  }

  // for internal use by resize, no resize check and no deleted key check here
  // no changes to HIP
  int insertKey(final byte[] key) {
    final long[] hash = MurmurHash3.hash(key, SEED);
    int entryIndex = getIndex(hash[0], tableEntries_);
    while (curCountsArr_[entryIndex] != 0) {
      entryIndex = (entryIndex + getStride(hash[1], tableEntries_)) % tableEntries_;
    }
    System.arraycopy(key, 0, keysArr_, entryIndex * keySizeBytes_, keySizeBytes_);
    numActiveKeys_++;
    return entryIndex;
  }

  /**
   */
  private void resize() {
    final byte[] oldKeysArr = keysArr_;
    final short[] oldCouponMapArr = couponMapArr_;
    final byte[] oldCurCountsArr = curCountsArr_;
    final float[] oldInvPow2SumArr = invPow2SumArr_;
    final float[] oldHipEstAccumArr = hipEstAccumArr_;
    final int oldSizeKeys = tableEntries_;
    tableEntries_ = Util.nextPrime((int) (growthFactor_ * numActiveKeys_));
    capacityEntries_ = (int)(tableEntries_ * OUTER_LOAD_FACTOR);
    System.out.println("resizing from " + oldSizeKeys + " to " + tableEntries_);
    keysArr_ = new byte[tableEntries_ * keySizeBytes_];
    couponMapArr_ = new short[tableEntries_ * maxCouponsPerKey_];
    curCountsArr_ = new byte[tableEntries_];
    invPow2SumArr_ = new float[tableEntries_];
    hipEstAccumArr_ = new float[tableEntries_];
    numActiveKeys_ = 0;
    numDeletedKeys_ = 0;
    for (int i = 0; i < oldSizeKeys; i++) {
      if (oldCurCountsArr[i] != 0) {
        //extract an old valid key
        final byte[] key =
            Arrays.copyOfRange(oldKeysArr, i * keySizeBytes_, i * keySizeBytes_ + keySizeBytes_);
        //insert the key and get its index
        final int index = insertKey(key);
        //copy the coupons array into that index
        System.arraycopy(oldCouponMapArr, i * maxCouponsPerKey_, couponMapArr_,
            index * maxCouponsPerKey_, maxCouponsPerKey_);
        //transfer the count
        curCountsArr_[index] = oldCurCountsArr[i];
        //transfer the HIP registers
        invPow2SumArr_[index] = oldInvPow2SumArr[i];
        hipEstAccumArr_[index] = oldHipEstAccumArr[i];
      }
    }
  }

  //hip +=  k/qt; qt -= 1/2^(val);
  @Override
  double findOrInsertCoupon(final int entryIndex, final short coupon) {
    final int couponMapArrEntryIndex = entryIndex * maxCouponsPerKey_;

    int innerCouponIndex = (coupon & 0xffff) % maxCouponsPerKey_;

    while (couponMapArr_[couponMapArrEntryIndex + innerCouponIndex] != 0) {
      if (couponMapArr_[couponMapArrEntryIndex + innerCouponIndex] == coupon) {
        return hipEstAccumArr_[entryIndex]; //duplicate, returns the estimate
      }
      innerCouponIndex = (innerCouponIndex + 1) % maxCouponsPerKey_; //linear search
    }
    if ((curCountsArr_[entryIndex] & BYTE_MASK) > capacityCouponsPerKey_) {
      //returns the negative estimate, as signal to promote
      return -hipEstAccumArr_[entryIndex];
    }

    couponMapArr_[couponMapArrEntryIndex + innerCouponIndex] = coupon; //insert
    curCountsArr_[entryIndex]++;
    hipEstAccumArr_[entryIndex] += k_/invPow2SumArr_[entryIndex];
    invPow2SumArr_[entryIndex] -= Util.invPow2(coupon16Value(coupon));
    return hipEstAccumArr_[entryIndex]; //returns the estimate
  }

  @Override
  int getCouponCount(final int index) {
    return (curCountsArr_[index] & BYTE_MASK);
  }

  @Override
  CouponsIterator getCouponsIterator(final byte[] key) {
    final int entryIndex = findKey(key);
    if (entryIndex < 0) return null;
    return new CouponsIterator(couponMapArr_, entryIndex * maxCouponsPerKey_, maxCouponsPerKey_);
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
    return numActiveKeys_ + numDeletedKeys_;
  }

  @Override
  public int getMemoryUsageBytes() {
    int arrays = entrySizeBytes_ * tableEntries_;
    int other = 4 * 5;
    return arrays + other;
  }

  private static final void checkMaxCouponsPerKey(final int maxCouponsPerKey, final int k) {
    checkIfPowerOf2(maxCouponsPerKey, "maxCouponsPerKey");
    int cpk = maxCouponsPerKey;
    if ((cpk < 16) || (cpk > 256 || cpk > k)) {
      throw new SketchesArgumentException(
          "Required: 16 <= maxCouponsPerKey <= 256 : "+maxCouponsPerKey);
    }
  }

}
