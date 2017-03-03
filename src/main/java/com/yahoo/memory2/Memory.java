/*
 * Copyright 2017, Yahoo! Inc. Licensed under the terms of the
 * Apache License 2.0. See LICENSE file at the project root for terms.
 */

package com.yahoo.memory2;


public abstract class Memory {

  //BYTE BUFFER

  //MAP

  //REGIONS

  /**
   * Returns a read only region of this Memory.
   * @param offsetBytes the starting offset
   * @param capacityBytes the capacity of the region
   * @return a read only region of this Memory
   */
  public abstract Memory region(long offsetBytes, long capacityBytes);


  //PRIMITIVE getXXX() and getXXXArray() //TODO

  /**
   * Gets the boolean value at the given offset
   * @param offsetBytes offset bytes relative to this Memory start
   * @return the boolean at the given offset
   */
  public abstract boolean getBoolean(long offsetBytes);

  /**
   * Gets the boolean array at the given offset
   * @param offsetBytes offset bytes relative to this Memory start
   * @param dstArray The preallocated destination array.
   * @param dstOffset offset in array units
   * @param length number of array units to transfer
   */
  public abstract void getBooleanArray(long offsetBytes, boolean[] dstArray, int dstOffset,
      int length);

  /**
   * Gets the byte value at the given offset
   * @param offsetBytes offset bytes relative to this Memory start
   * @return the byte at the given offset
   */
  public abstract byte getByte(long offsetBytes);

  /**
   * Gets the byte array at the given offset
   * @param offsetBytes offset bytes relative to this Memory start
   * @param dstArray The preallocated destination array.
   * @param dstOffset offset in array units
   * @param length number of array units to transfer
   */
  public abstract void getByteArray(long offsetBytes, byte[] dstArray, int dstOffset,
      int length);

  /**
   * Gets the char value at the given offset
   * @param offsetBytes offset bytes relative to this Memory start
   * @return the char at the given offset
   */
  public abstract char getChar(long offsetBytes);

  /**
   * Gets the char array at the given offset
   * @param offsetBytes offset bytes relative to this Memory start
   * @param dstArray The preallocated destination array.
   * @param dstOffset offset in array units
   * @param length number of array units to transfer
   */
  public abstract void getCharArray(long offsetBytes, char[] dstArray, int dstOffset,
      int length);

  /**
   * Gets the double value at the given offset
   * @param offsetBytes offset bytes relative to this Memory start
   * @return the double at the given offset
   */
  public abstract double getDouble(long offsetBytes);

  /**
   * Gets the double array at the given offset
   * @param offsetBytes offset bytes relative to this Memory start
   * @param dstArray The preallocated destination array.
   * @param dstOffset offset in array units
   * @param length number of array units to transfer
   */
  public abstract void getDoubleArray(long offsetBytes, double[] dstArray, int dstOffset,
      int length);

  /**
   * Gets the float value at the given offset
   * @param offsetBytes offset bytes relative to this Memory start
   * @return the float at the given offset
   */
  public abstract float getFloat(long offsetBytes);

  /**
   * Gets the float array at the given offset
   * @param offsetBytes offset bytes relative to this Memory start
   * @param dstArray The preallocated destination array.
   * @param dstOffset offset in array units
   * @param length number of array units to transfer
   */
  public abstract void getFloatArray(long offsetBytes, float[] dstArray, int dstOffset,
      int length);

  /**
   * Gets the int value at the given offset
   * @param offsetBytes offset bytes relative to this Memory start
   * @return the int at the given offset
   */
  public abstract int getInt(long offsetBytes);

  /**
   * Gets the int array at the given offset
   * @param offsetBytes offset bytes relative to this Memory start
   * @param dstArray The preallocated destination array.
   * @param dstOffset offset in array units
   * @param length number of array units to transfer
   */
  public abstract void getIntArray(long offsetBytes, int[] dstArray, int dstOffset,
      int length);

  /**
   * Gets the long value at the given offset
   * @param offsetBytes offset bytes relative to this Memory start
   * @return the long at the given offset
   */
  public abstract long getLong(long offsetBytes);

  /**
   * Gets the long array at the given offset
   * @param offsetBytes offset bytes relative to this Memory start
   * @param dstArray The preallocated destination array.
   * @param dstOffset offset in array units
   * @param length number of array units to transfer
   */
  public abstract void getLongArray(long offsetBytes, long[] dstArray, int dstOffset, int length);

  /**
   * Gets the short value at the given offset
   * @param offsetBytes offset bytes relative to this Memory start
   * @return the short at the given offset
   */
  public abstract short getShort(long offsetBytes);

  /**
   * Gets the short array at the given offset
   * @param offsetBytes offset bytes relative to this Memory start
   * @param dstArray The preallocated destination array.
   * @param dstOffset offset in array units
   * @param length number of array units to transfer
   */
  public abstract void getShortArray(long offsetBytes, short[] dstArray, int dstOffset,
      int length);

  //OTHER PRIMITIVE READ METHODS: copy, isYYYY(), areYYYY() //TODO

  /**
   * Copies bytes from a source range of this Memory to a destination range of the given Memory
   * using the same low-level system copy function as found in
   * {@link java.lang.System#arraycopy(Object, int, Object, int, int)}.
   * @param srcOffsetBytes the source offset for this Memory
   * @param destination the destination Memory, which may not be Read-Only.
   * @param dstOffsetBytes the destintaion offset
   * @param lengthBytes the number of bytes to copy
   */
  public abstract void copy(long srcOffsetBytes, WritableMemory destination, long dstOffsetBytes,
      long lengthBytes);

  /**
   * Returns true if all bits defined by the bitMask are clear
   * @param offsetBytes offset bytes relative to this Memory start
   * @param bitMask bits set to one will be checked
   * @return true if all bits defined by the bitMask are clear
   */
  public abstract boolean isAllBitsClear(long offsetBytes, byte bitMask);

  /**
   * Returns true if all bits defined by the bitMask are set
   * @param offsetBytes offset bytes relative to this Memory start
   * @param bitMask bits set to one will be checked
   * @return true if all bits defined by the bitMask are set
   */
  public abstract boolean isAllBitsSet(long offsetBytes, byte bitMask);

  /**
   * Returns true if any bits defined by the bitMask are clear
   * @param offsetBytes offset bytes relative to this Memory start
   * @param bitMask bits set to one will be checked
   * @return true if any bits defined by the bitMask are clear
   */
  public abstract boolean isAnyBitsClear(long offsetBytes, byte bitMask);

  /**
   * Returns true if any bits defined by the bitMask are set
   * @param offsetBytes offset bytes relative to this Memory start
   * @param bitMask bits set to one will be checked
   * @return true if any bits defined by the bitMask are set
   */
  public abstract boolean isAnyBitsSet(long offsetBytes, byte bitMask);

  //OTHER READ METHODS //TODO

  /**
   * Gets the capacity of this Memory in bytes
   * @return the capacity of this Memory in bytes
   */
  public abstract long getCapacity();

  /**
   * Returns the cumulative offset in bytes of this Memory including the given offsetBytes.
   *
   * @param offsetBytes the given offset in bytes
   * @return the cumulative offset in bytes of this Memory including the given offsetBytes.
   */
  public abstract long getCumulativeOffset(final long offsetBytes);

  /**
   * Returns true if this Memory is backed by an on-heap primitive array
   * @return true if this Memory is backed by an on-heap primitive array
   */
  public abstract boolean hasArray();

  /**
   * Returns true if this Memory is backed by a ByteBuffer
   * @return true if this Memory is backed by a ByteBuffer
   */
  public abstract boolean hasByteBuffer();

  /**
   * Returns true if the backing memory is direct (off-heap) memory.
   * @return true if the backing memory is direct (off-heap) memory.
   */
  public abstract boolean isDirect();

  /**
   * Returns true if this Memory is read only
   * @return true if this Memory is read only
   */
  public abstract boolean isReadOnly(); //TODO may not need

  /**
   * Returns true if this Memory is valid() and has not been closed.
   * @return true if this Memory is valid() and has not been closed.
   */
  public abstract boolean isValid();

  /**
   * Returns a formatted hex string of a range of this Memory.
   * Used primarily for testing.
   * @param header descriptive header
   * @param offsetBytes offset bytes relative to this Memory start
   * @param lengthBytes number of bytes to convert to a hex string
   * @return a formatted hex string in a human readable array
   */
  public abstract String toHexString(String header, long offsetBytes, int lengthBytes);

}
