/*
 * Copyright 2017, Yahoo! Inc. Licensed under the terms of the
 * Apache License 2.0. See LICENSE file at the project root for terms.
 */

package com.yahoo.memory2;

import static com.yahoo.memory2.UnsafeUtil.unsafe;

import java.io.File;
import java.io.FileDescriptor;
import java.io.RandomAccessFile;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.concurrent.atomic.AtomicBoolean;

import sun.misc.Cleaner;
import sun.nio.ch.FileChannelImpl;

/**
 * AllocateDirectMap class extends WritableMemoryImpl and is used to memory map files
 * (including those &gt; 2GB) off heap.
 *
 * @author Praveenkumar Venkatesan
 */
final class AllocateDirectMap extends WritableMemoryImpl {
  private RandomAccessFile randomAccessFile = null;
  private MappedByteBuffer dummyMbbInstance = null;
  private final Cleaner cleaner;


  private AllocateDirectMap(
      final RandomAccessFile raf,
      final MappedByteBuffer mbb,
      final long nativeBaseOffset,
      final long capacity,
      final boolean readOnly) {
    super(nativeBaseOffset, null, 0L, null, 0L, capacity, null, readOnly);
    this.randomAccessFile = raf;
    this.dummyMbbInstance = mbb;
    this.cleaner = Cleaner.create(this,
        new Deallocator(raf, nativeBaseOffset, capacity, super.valid));
  }

  /**
   * Factory method for creating a memory mapping a file.
   *
   * <p>Memory maps a file directly in off heap leveraging native map0 method used in
   * FileChannelImpl.c. The owner will have read write access to that address space.</p>
   *
   * @param file File to be mapped
   * @param offset Memory map starting from this position in the file
   * @param capacity Memory map at most capacity bytes &gt; 0 starting from {@code position}
   * @param readOnlyRequest true if requesting method requests read-only interface.
   * @return A new MemoryMappedFile
   * @throws Exception file not found or RuntimeException, etc.
   */
  @SuppressWarnings("resource")
  static AllocateDirectMap getInstance(final File file, final long offset, final long capacity)
      throws Exception {
    checkOffsetAndCapacity(offset, capacity);
    final String mode = file.canWrite() ? "rw" : "r";
    final RandomAccessFile raf = new RandomAccessFile(file, mode);
    final FileChannel fc = raf.getChannel();
    final long nativeBaseAddress = map(fc, offset, capacity);
    final long capacityBytes = capacity;

    // len can be more than the file.length
    raf.setLength(capacity);
    final MappedByteBuffer mbb = createDummyMbbInstance(nativeBaseAddress);

    return new AllocateDirectMap(raf, mbb, nativeBaseAddress, capacityBytes, (mode.equals("r")));
  }

  @Override
  public void load() {
    madvise();

    // Read a byte from each page to bring it into memory.
    final int ps = unsafe.pageSize();
    final int count = pageCount(ps, capacity);
    long a = nativeBaseOffset;
    for (int i = 0; i < count; i++) {
      unsafe.getByte(a);
      a += ps;
    }
  }

  @Override
  public boolean isLoaded() {
    try {
      final int ps = unsafe.pageSize();
      final int pageCount = pageCount(ps, capacity);
      final Method method =
          MappedByteBuffer.class.getDeclaredMethod("isLoaded0", long.class, long.class, int.class);
      method.setAccessible(true);
      return (boolean) method.invoke(dummyMbbInstance, nativeBaseOffset, capacity,
          pageCount);
    } catch (final Exception e) {
      throw new RuntimeException(
          String.format("Encountered %s exception while loading", e.getClass()));
    }
  }

  @Override
  public void force() {
    try {
      final Method method = MappedByteBuffer.class.getDeclaredMethod("force0", FileDescriptor.class,
          long.class, long.class);
      method.setAccessible(true);
      method.invoke(dummyMbbInstance, randomAccessFile.getFD(), nativeBaseOffset,
          capacity);
    } catch (final Exception e) {
      throw new RuntimeException(String.format("Encountered %s exception in force", e.getClass()));
    }
  }

  @Override
  public void close() {
    try {
      cleaner.clean();
    } catch (final Exception e) {
      throw e;
    }
  }

  // Restricted methods

  static final int pageCount(final int ps, final long capacity) {
    return (int) ( (capacity == 0) ? 0 : (capacity - 1L) / ps + 1L);
  }

  private static final MappedByteBuffer createDummyMbbInstance(final long nativeBaseAddress)
      throws RuntimeException {
    try {
      final Class<?> cl = Class.forName("java.nio.DirectByteBuffer");
      final Constructor<?> ctor =
          cl.getDeclaredConstructor(int.class, long.class, FileDescriptor.class, Runnable.class);
      ctor.setAccessible(true);
      final MappedByteBuffer mbb = (MappedByteBuffer) ctor.newInstance(0, // some junk capacity
          nativeBaseAddress, null, null);
      return mbb;
    } catch (final Exception e) {
      throw new RuntimeException(
          "Could not create Dummy MappedByteBuffer instance: " + e.getClass());
    }
  }

  /**
   * madvise is a system call made by load0 native method
   */
  private void madvise() throws RuntimeException {
    try {
      final Method method = MappedByteBuffer.class.getDeclaredMethod("load0", long.class, long.class);
      method.setAccessible(true);
      method.invoke(dummyMbbInstance, nativeBaseOffset, capacity);
    } catch (final Exception e) {
      throw new RuntimeException(
          String.format("Encountered %s exception while loading", e.getClass()));
    }
  }

  /**
   * Creates a mapping of the file on disk starting at position and of size length to pages in OS.
   * May throw OutOfMemory error if you have exhausted memory. Force garbage collection and
   * re-attempt.
   */
  private static final long map(final FileChannel fileChannel, final long position, final long len)
      throws RuntimeException {
    final int pagePosition = (int) (position % unsafe.pageSize());
    final long mapPosition = position - pagePosition;
    final long mapSize = len + pagePosition;

    try {
      final Method method =
          FileChannelImpl.class.getDeclaredMethod("map0", int.class, long.class, long.class);
      method.setAccessible(true);
      final long addr = (long) method.invoke(fileChannel, 1, mapPosition, mapSize);
      return addr;
    } catch (final Exception e) {
      throw new RuntimeException(
          String.format("Encountered %s exception while mapping", e.getClass()));
    }
  }

  private static final class Deallocator implements Runnable {
    private RandomAccessFile raf;
    private FileChannel fc;
    //This is the only place the actual native offset is kept for use by unsafe.freeMemory();
    //It can never be modified until it is deallocated.
    private long actualNativeBaseOffset;
    private final long myCapacity;
    private final AtomicBoolean parentValidRef;

    private Deallocator(final RandomAccessFile randomAccessFile, final long nativeBaseOffset,
        final long capacity, final AtomicBoolean valid) {
      assert (randomAccessFile != null);
      assert (nativeBaseOffset != 0);
      assert (capacity != 0);
      this.raf = randomAccessFile;
      this.fc = randomAccessFile.getChannel();
      this.actualNativeBaseOffset = nativeBaseOffset;
      this.myCapacity = capacity;
      this.parentValidRef = valid;
    }

    /**
     * Removes existing mapping
     */
    private void unmap() throws RuntimeException {
      try {
        final Method method = FileChannelImpl.class.getDeclaredMethod("unmap0", long.class, long.class);
        method.setAccessible(true);
        method.invoke(this.fc, this.actualNativeBaseOffset, this.myCapacity);
        this.raf.close();
      } catch (final Exception e) {
        throw new RuntimeException(
            String.format("Encountered %s exception while freeing memory", e.getClass()));
      }
    }

    @Override
    public void run() {
      if (this.fc != null) {
        unmap();
      }
      this.actualNativeBaseOffset = 0L;
      this.parentValidRef.set(false); //The only place valid is set false.
    }
  } //End of class Deallocator

  private static final void checkOffsetAndCapacity(final long offset, final long capacity) {
    if (((offset) | (capacity - 1L) | (offset + capacity)) < 0) {
      throw new IllegalArgumentException(
          "offset: " + offset + ", capacity: " + capacity
          + ", offset + capacity: " + (offset + capacity));
    }
  }

}
