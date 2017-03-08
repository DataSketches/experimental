/*
 * Copyright 2017, Yahoo! Inc. Licensed under the terms of the
 * Apache License 2.0. See LICENSE file at the project root for terms.
 */

package com.yahoo.memory4;

import java.io.File;
import java.nio.ByteBuffer;

final class MemoryState {
  private long nativeBaseOffset_ = 0L; //Direct ByteBuffer includes the slice() offset here.
  private Object unsafeObj_ = null; //##Array objects are held here.
  private long unsafeObjHeader_ = 0L; //##Heap ByteBuffer includes the slice() offset here.
  private ByteBuffer byteBuf_ = null; //Holding this until we are done with it.
  private File file_ = null; //Holding this until we are done with it.
  private long fileOffset_;
  private long regionOffset_ = 0L;
  private long capacity_ = 0L;//##
  private long cumBaseOffset_ = 0L; //##Holds the cum offset to the start of data.
  private MemoryRequest memReq_ = null; //##

  //##Assert protection against casting Memory to WritableMemory.
  private boolean nonNativeEndian_ = false; //##
  private boolean positional_ = false;

  private StepBoolean resourceIsReadOnly_ = new StepBoolean(false); //initial state is writable
  private StepBoolean valid_ = new StepBoolean(true); //## initial state is valid

  MemoryState() {}

  MemoryState copy() {
    final MemoryState out = new MemoryState();
    out.nativeBaseOffset_ = nativeBaseOffset_;
    out.unsafeObj_ = unsafeObj_;
    out.unsafeObjHeader_ = unsafeObjHeader_;
    out.byteBuf_ = byteBuf_;
    out.file_ = file_;
    out.fileOffset_ = fileOffset_;
    out.regionOffset_ = regionOffset_;
    out.capacity_ = capacity_;
    out.memReq_ = memReq_;
    out.nonNativeEndian_ = nonNativeEndian_;
    out.positional_ = positional_;
    out.resourceIsReadOnly_ = resourceIsReadOnly_;
    out.valid_ = valid_;
    return out;
  }

  private void compute() {
    this.cumBaseOffset_ = regionOffset_
        + ((unsafeObj_ == null) ? nativeBaseOffset_ : unsafeObjHeader_);
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

  File getFile() {
    return file_;
  }

  long getFileOffset() {
    return fileOffset_;
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

  boolean isResourceReadOnly() {
    return resourceIsReadOnly_.get();
  }

  boolean isValid() {
    return valid_.get();
  }

  boolean isNotNativeEndian() {
    return nonNativeEndian_;
  }

  boolean isDirect() {
    return nativeBaseOffset_ > 0L;
  }

  boolean isPostional() {
    return positional_;
  }

  void putNativeBaseOffset(final long nativeBaseOffset) {
    this.nativeBaseOffset_ = nativeBaseOffset;
    compute();
  }

  void putUnsafeObject(final Object unsafeObj) {
    this.unsafeObj_ = unsafeObj;
    compute();
  }

  void putUnsafeObjectHeader(final long unsafeObjHeader) {
    this.unsafeObjHeader_ = unsafeObjHeader;
    compute();
  }

  void putByteBuffer(final ByteBuffer byteBuf) {
    this.byteBuf_ = byteBuf;
  }

  void putFile(final File file) {
    this.file_ = file;
  }

  void putFileOffset(final long fileOffset) {
    this.fileOffset_ = fileOffset;
  }

  void putRegionOffset(final long regionOffset) {
    this.regionOffset_ = regionOffset;
    compute();
  }

  void putCapacity(final long capacity) {
    this.capacity_ = capacity;
  }

  void putMemoryRequest(final MemoryRequest memReq) {
    this.memReq_ = memReq;
  }

  void setResourceReadOnly() {
    this.resourceIsReadOnly_.change();
  }

  void setInvalid() { //NOT SUBJECT TO LOCK
    this.valid_.change();
  }

  void setNonNativeEndian(final boolean nonNativeEndian) {
    this.nonNativeEndian_ = nonNativeEndian;
  }

  void setPositional(final boolean positional) {
    this.positional_ = positional;
  }

}
