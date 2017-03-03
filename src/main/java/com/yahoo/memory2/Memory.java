/*
 * Copyright 2017, Yahoo! Inc. Licensed under the terms of the
 * Apache License 2.0. See LICENSE file at the project root for terms.
 */

package com.yahoo.memory2;

public abstract class Memory {

  //BYTE BUFFER

  //MAP

  //REGIONS

  public abstract Memory region(long offsetBytes, long capacityBytes);


  //PRIMITIVE GETS

  /**
   * Gets the boolean value at the given offset
   * @param offsetBytes offset bytes relative to this Memory start
   * @return the boolean at the given offset
   */
  public abstract boolean getBoolean(long offsetBytes);

  /**
   * Gets the byte value at the given offset
   * @param offsetBytes offset bytes relative to this Memory start
   * @return the byte at the given offset
   */
  public abstract byte getByte(long offsetBytes);

  /**
   * Gets the char value at the given offset
   * @param offsetBytes offset bytes relative to this Memory start
   * @return the char at the given offset
   */
  public abstract char getChar(long offsetBytes);

  /**
   * Gets the short value at the given offset
   * @param offsetBytes offset bytes relative to this Memory start
   * @return the short at the given offset
   */
  public abstract short getShort(long offsetBytes);

  /**
   * Gets the int value at the given offset
   * @param offsetBytes offset bytes relative to this Memory start
   * @return the int at the given offset
   */
  public abstract int getInt(long offsetBytes);

  /**
   * Gets the long value at the given offset
   * @param offsetBytes offset bytes relative to this Memory start
   * @return the long at the given offset
   */
  public abstract long getLong(long offsetBytes);

  /**
   * Gets the float value at the given offset
   * @param offsetBytes offset bytes relative to this Memory start
   * @return the float at the given offset
   */
  public abstract float getFloat(long offsetBytes);

  /**
   * Gets the double value at the given offset
   * @param offsetBytes offset bytes relative to this Memory start
   * @return the double at the given offset
   */
  public abstract double getDouble(long offsetBytes);

  //Primitive Get Arrays

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
   * Gets the byte array at the given offset
   * @param offsetBytes offset bytes relative to this Memory start
   * @param dstArray The preallocated destination array.
   * @param dstOffset offset in array units
   * @param length number of array units to transfer
   */
  public abstract void getByteArray(long offsetBytes, byte[] dstArray, int dstOffset,
      int length);

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
   * Gets the long array at the given offset
   * @param offsetBytes offset bytes relative to this Memory start
   * @param dstArray The preallocated destination array.
   * @param dstOffset offset in array units
   * @param length number of array units to transfer
   */
  public abstract void getLongArray(long offsetBytes, long[] dstArray, int dstOffset, int length);


  public abstract long getCapacity();
  //Plus a number of convenience read methods not listed


  public abstract boolean isValid();

  public abstract String toHexString(String header, long offsetBytes, int lengthBytes);

}
