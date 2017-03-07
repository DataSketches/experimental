/*
 * Copyright 2017, Yahoo! Inc. Licensed under the terms of the
 * Apache License 2.0. See LICENSE file at the project root for terms.
 */

package com.yahoo.memory4;

import java.nio.ByteBuffer;

final class MemoryState {
  private long nativeBaseOffset_ = 0L; //Direct ByteBuffer includes the slice() offset here.
  private Object unsafeObj_ = null; //##Array objects are held here.
  private long unsafeObjHeader_ = 0L; //##Heap ByteBuffer includes the slice() offset here.
  private ByteBuffer byteBuf_ = null; //Holding this until we are done with it.
  private long regionOffset_ = 0L;
  private long capacity_ = 0L;//##
  private long cumBaseOffset_ = 0L; //##Holds the cum offset to the start of data.
  private MemoryRequest memReq_ = null; //##

  //##Assert protection against casting Memory to WritableMemory.
  private boolean nonNativeEndian_ = false; //##
  private boolean positional_ = false;
  private boolean locked_ = false;

  //These can be extenally driven
  private StepBoolean readOnly_ = new StepBoolean(false); //initial state is writable
  private StepBoolean valid = new StepBoolean(true); //## initial state is valid

  MemoryState() {}

  void lock() {
    this.cumBaseOffset_ = regionOffset_
        + ((unsafeObj_ == null) ? nativeBaseOffset_ : unsafeObjHeader_);
    this.locked_ = true;
  }

  long getNativeBaseOffset() {
    return nativeBaseOffset_;
  }

  Object getUnsafeObject() {
    return unsafeObj_;
  }

  long getUnsafeObjectHeader() {
    return unsafeObjHeader_;
  }

  ByteBuffer getByteBuffer() {
    return byteBuf_;
  }

  long getRegionOffset() {
    return regionOffset_;
  }

  long getCapacity() {
    return capacity_;
  }

  long getCumBaseOffset() {
    return cumBaseOffset_;
  }

  MemoryRequest getMemoryRequest() {
    return memReq_;
  }

  boolean isReadOnly() {
    return readOnly_.get();
  }

  boolean isValid() {
    return valid.get();
  }

  boolean isNotNativeEndian() {
    return nonNativeEndian_;
  }

  boolean isPostional() {
    return positional_;
  }

  boolean isLocked() {
    return locked_;
  }

  void putNativeBaseOffset(final long nativeBaseOffset) {
    checkLocked();
    this.nativeBaseOffset_ = nativeBaseOffset;
  }

  void putUnsafeObject(final Object unsafeObj) {
    checkLocked();
    this.unsafeObj_ = unsafeObj;
  }

  void putUnsafeObjectHeader(final long unsafeObjHeader) {
    checkLocked();
    this.unsafeObjHeader_ = unsafeObjHeader;
  }

  void putByteBuffer(final ByteBuffer byteBuf) {
    checkLocked();
    this.byteBuf_ = byteBuf;
  }

  void putRegionOffset(final long regionOffset) {
    checkLocked();
    this.regionOffset_ = regionOffset;
  }

  void putCapacity(final long capacity) {
    checkLocked();
    this.capacity_ = capacity;
  }

  void putMemoryRequest(final MemoryRequest memReq) {
    checkLocked();
    this.memReq_ = memReq;
  }

  void setReadOnly() {
    checkLocked();
    this.readOnly_.change();
  }

  void setInvalid() {
    this.valid.change();
  }

  void setNonNativeEndian(final boolean nonNativeEndian) {
    checkLocked();
    this.nonNativeEndian_ = nonNativeEndian;
  }

  void setPositional(final boolean positional) { //this one can change.
    this.positional_ = positional;
  }

  private void checkLocked() {
    if (locked_) { throw new ReadOnlyMemoryException("Attempted write."); }
  }

}
