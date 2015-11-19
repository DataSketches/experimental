package com.yahoo.sketches.tuple;

public class Entry<S extends Summary> implements Comparable<Entry<S>> {
  long key_;
  S summary_;

  Entry(long key) {
    this.key_ = key;
  }

  public Entry(long key, S summary) {
    this.key_ = key;
    this.summary_ = summary;
  }

  @Override
  public int compareTo(Entry<S> that) {
    if (this.key_ < that.key_) return -1;
    else if (this.key_ > that.key_) return 1;
    return 0;
  }

  public S getSummary() {
    return summary_;
  }
}
