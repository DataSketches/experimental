package com.yahoo.sketches.hllmap;

public class MapValuesIterator {

  private final int offset_;
  private final int numEntries_;
  private final short[] values_;
  private int index_;
  
  MapValuesIterator(final short[] values, final int offset, final int numEntries) {
    offset_ = offset;
    numEntries_ = numEntries;
    values_ = values;
    index_ = -1;
  }

  boolean next() {
    index_++;
    while (index_ < numEntries_) {
      if (values_[offset_ + index_] != 0) return true;
      index_++;
    }
    return false;
  }

  short getValue() {
    return values_[offset_ + index_];
  }

}
