/*
 * Copyright 2016, Yahoo! Inc. Licensed under the terms of the Apache License 2.0. See LICENSE file
 * at the project root for terms.
 */

package com.yahoo.sketches.frequencies;

public class Pair implements Comparable<Pair> {
  private long name;
  private long value;

  public Pair(final long name, final long value) {
    this.name = name;
    this.value = value;
  }

  public long getname() {
    return name;
  }

  public long getvalue() {
    return value;
  }

  /**
   * blah
   * @param o1 blah
   * @param o2 blah
   * @return blah
   */
  public int compare(final Pair o1, final Pair o2) {
    if (o1.value > o2.value) {
      return 1;
    } else if (o1.value < o2.value) {
      return -1;
    }
    return 0;
  }

  @Override
  public int compareTo(final Pair o2) {
    if (this.value > o2.value) {
      return 1;
    } else if (this.value < o2.value) {
      return -1;
    }
    return 0;
  }

  @Override
  public int hashCode() {
    final int hash = 3;
    return hash;
  }

  @Override
  public boolean equals(final Object o) {
    final Pair a2 = (Pair) o;
    return ((a2 != null) && (this.name == a2.name));
  }
}
