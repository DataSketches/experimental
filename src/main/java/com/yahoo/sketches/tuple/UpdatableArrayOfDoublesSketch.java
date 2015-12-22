package com.yahoo.sketches.tuple;

import static com.yahoo.sketches.Util.DEFAULT_UPDATE_SEED;

import com.yahoo.sketches.hash.MurmurHash3;
import com.yahoo.sketches.memory.Memory;

public abstract class UpdatableArrayOfDoublesSketch extends ArrayOfDoublesSketch {

  /**
   * Updates this sketch with a long key and double values.
   * The values will be stored or added to the ones associated with the key 
   * 
   * @param key The given long key
   * @param values The given values
   */
  public void update(long key, double[] values) {
    update(Util.longToLongArray(key), values);
  }

  /**
   * Updates this sketch with a double key and double values.
   * The values will be stored or added to the ones associated with the key 
   * 
   * @param key The given double key
   * @param value The given values
   */
  public void update(double key, double[] values) {
    update(Util.doubleToLongArray(key), values);
  }

  /**
   * Updates this sketch with a String key and double values.
   * The values will be stored or added to the ones associated with the key 
   * 
   * @param key The given String key
   * @param value The given values
   */
  public void update(String key, double[] values) {
    update(Util.stringToByteArray(key), values);
  }

  /**
   * Updates this sketch with a byte[] key and double values.
   * The values will be stored or added to the ones associated with the key 
   * 
   * @param key The given byte[] key
   * @param value The given values
   */
  public void update(byte[] key, double[] values) {
    if (key == null || key.length == 0) return;
    insertOrIgnore(MurmurHash3.hash(key, DEFAULT_UPDATE_SEED)[0] >>> 1, values);
  }

  /**
   * Updates this sketch with a int[] key and double values.
   * The values will be stored or added to the ones associated with the key 
   * 
   * @param key The given int[] key
   * @param value The given values
   */
  public void update(int[] key, double[] values) {
    if (key == null || key.length == 0) return;
    insertOrIgnore(MurmurHash3.hash(key, DEFAULT_UPDATE_SEED)[0] >>> 1, values);
  }

  /**
   * Updates this sketch with a long[] key and double values.
   * The values will be stored or added to the ones associated with the key 
   * 
   * @param key The given long[] key
   * @param value The given values
   */
  public void update(long[] key, double[] values) {
    if (key == null || key.length == 0) return;
    insertOrIgnore(MurmurHash3.hash(key, DEFAULT_UPDATE_SEED)[0] >>> 1, values);
  }

  public abstract int getNominalEntries();

  public ArrayOfDoublesCompactSketch compact() {
    return new HeapArrayOfDoublesCompactSketch(this);
  }

  public ArrayOfDoublesCompactSketch compact(Memory dstMem) {
    return new DirectArrayOfDoublesCompactSketch(this, dstMem);
  }

  protected abstract void insertOrIgnore(long key, double[] values);

}
