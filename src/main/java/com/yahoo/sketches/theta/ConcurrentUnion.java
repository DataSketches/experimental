/*
 * Copyright 2016, Yahoo! Inc. Licensed under the terms of the Apache License 2.0. See LICENSE file
 * at the project root for terms.
 */

package com.yahoo.sketches.theta;

import com.yahoo.memory.Memory;

import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * ConcurrentUnion is a thread safe implementation of the union interface.
 * It supports concurrency by using a coarse-grain read-write lock to protect read and update
 * methods.
 * The Assumption is that read methods (like @link{getResult()} and @link{toByteArray()}) are
 * read-only, that is they do not change the internal state of the union.
 */
public class ConcurrentUnion implements Union {

  private final Union delegatee;
  private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

  public ConcurrentUnion(Union delegatee) {
    this.delegatee = delegatee;
  }

  @Override public void update(Sketch sketch) {
    try {
      lock.writeLock().lock();
      delegatee.update(sketch);
    } finally {
      lock.writeLock().unlock();
    }
  }

  @Override public void update(Memory memory) {
    try {
      lock.writeLock().lock();
      delegatee.update(memory);
    } finally {
      lock.writeLock().unlock();
    }
  }

  @Override public void update(long l) {
    try {
      lock.writeLock().lock();
      delegatee.update(l);
    } finally {
      lock.writeLock().unlock();
    }
  }

  @Override public void update(double v) {
    try {
      lock.writeLock().lock();
      delegatee.update(v);
    } finally {
      lock.writeLock().unlock();
    }
  }

  @Override public void update(String s) {
    try {
      lock.writeLock().lock();
      delegatee.update(s);
    } finally {
      lock.writeLock().unlock();
    }
  }

  @Override public void update(byte[] bytes) {
    try {
      lock.writeLock().lock();
      delegatee.update(bytes);
    } finally {
      lock.writeLock().unlock();
    }
  }

  @Override public void update(int[] ints) {
    try {
      lock.writeLock().lock();
      delegatee.update(ints);
    } finally {
      lock.writeLock().unlock();
    }
  }

  @Override public void update(char[] chars) {
    try {
      lock.writeLock().lock();
      delegatee.update(chars);
    } finally {
      lock.writeLock().unlock();
    }
  }

  @Override public void update(long[] longs) {
    try {
      lock.writeLock().lock();
      delegatee.update(longs);
    } finally {
      lock.writeLock().unlock();
    }
  }

  @Override public CompactSketch getResult(boolean b, Memory memory) {
    try {
      lock.readLock().lock();
      return delegatee.getResult(b, memory);
    } finally {
      lock.readLock().unlock();
    }
  }

  @Override public CompactSketch getResult() {
    try {
      lock.readLock().lock();
      return delegatee.getResult();
    } finally {
      lock.readLock().unlock();
    }
  }

  @Override public byte[] toByteArray() {
    try {
      lock.readLock().lock();
      return delegatee.toByteArray();
    } finally {
      lock.readLock().unlock();
    }
  }

  @Override public void reset() {
    try {
      lock.writeLock().lock();
      delegatee.reset();
    } finally {
      lock.writeLock().unlock();
    }
  }
}
