/*
Version 4
public abstract Memory {
  static ByteBufferHandler wrap(BB, NNE)
  static MemoryMapHandler map(file, cap, NNE)
  static wrap(array)
  static allocate(cap) //hidden byte[]
  static regionAs()
  abstract getXXX(offset) methods
}

public abstract WritableMemory {
  static WritableByteBufferHandler wrap(BB, NNE)
  static WritableMemoryMapHandler map(file, cap, NNE)
  static wrap(array)
  static allocate(cap)  //hidden byte[]
  static allocateDirect(cap, NNE)
  static wrap(array)
  abstract getXXX(offset)
  abstract putXXX(offset, value)
}

public abstract Buffer {
  static wrap(BB, NNE)
  static map(file, cap, NNE)
  static wrap(array)
  static allocate(cap) //hidden byte[]
  abstract getXXX(offset) methods
  abstract //positional methods
}

public abstract WritableBuffer {
  static wrap(BB, NNE)
  static map(file, cap, NNE)
  static wrap(array)
  static allocate(cap)  //hidden byte[]
  static allocateDirect(cap, NNE)
  static wrap(array)
  abstract getXXX(offset)
  abstract putXXX(offset, value)
  abstract //positional methods
}

public ByteBufferHandler implements AutoCloseable {
  Memory get() returns the correct MemoryImpl: RO/NE, RO/NNE
  void close()
}

public WritableByteBufferHandler implements AutoCloseable {
  WritableMemory get() returns the correct WritableMemoryImpl: W/NE, W/NNE
  void close()
}


public MemoryMapHandler implements AutoCloseable {
}

public WritableMemoryMapHandler implements AutoCloseable {
}

public DirectMemoryHandler implements AutoCloseable { //always writable
}
*/
package com.yahoo.memory4;
