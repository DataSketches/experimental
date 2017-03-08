/*
Version 4
public abstract class Memory {
  public static MemoryHandler wrap(final ByteBuffer byteBuf)
  public static MemoryHandler map(final File file)
  public static MemoryHandler map(final File file, final long fileOffset, final long capacity)
  public abstract Memory region(long offsetBytes, long capacityBytes)
  public static Memory wrap(final prim-type[] arr)
  public abstract void copy(long srcOffsetBytes, WritableMemory destination, long dstOffsetBytes,
      long lengthBytes)
  public abstract getXXX(offset) methods
  ... plus other read misc
  public abstract asNonNativeEndian() //not implemented
}

public abstract class WritableMemory {
  public static WritableMemoryHandler wrap(final ByteBuffer byteBuf)
  public static WritableMemoryHandler map(final File file)
  public static WritableMemoryHandler map(final File file, final long fileOffset, final long capacity)
  public static WritableMemoryHandler allocateDirect(final long capacityBytes)
  public static WritableMemoryHandler allocateDirect(final long capacityBytes, final MemoryRequest memReq)
  public abstract WritableMemory region(long offsetBytes, long capacityBytes)
  public abstract Memory asReadOnly();
  public static WritableMemory allocate(final int capacityBytes)
  public static WritableMemory wrap(final prim-type[] arr)
  public abstract void copy(long srcOffsetBytes, WritableMemory destination, long dstOffsetBytes,
      long lengthBytes);
  public abstract getXXX(offset) methods
  ... plus other read misc
  public abstract void putXXX(long offsetBytes, prim-type value)
  ... plus other write misc
  public abstract MemoryRequest getMemoryRequest()
  public abstract asNonNativeEndian() //not implemented
}

public interface MemoryHandler extends AutoCloseable {
  Memory get()
  void close()
  void load()        //only for memory-mapped-files
  boolean isLoaded() //only for memory-mapped-files
}

public interface WritableMemoryHandler extends AutoCloseable {
  WritableMemory get()
  void close()
  void load()        //only for memory-mapped-files
  boolean isLoaded() //only for memory-mapped-files
  void force()       //only for memory-mapped-files
}

public abstract Buffer { //Not implemented
}

public abstract WritableBuffer { //Not implemented
}

*/
package com.yahoo.memory4;
