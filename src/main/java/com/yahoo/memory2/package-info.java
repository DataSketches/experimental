/*

  public Memory
       - static RO methods: wrapping arrays, ByteBuffer, Map
       - abstract RO methods: get<primitive>, get<primitive>Array

    MemoryImpl extends Memory
      - Concrete impls of Memory

  public WritableMemory extends Memory
      - static W methods: wrapping arrays, ByteBuffer, Map, Direct alloc, autoByteArray
      - abstract W methods: put<primitive>, put<primitive>Array,
      - plus abstract get MemoryRequest(), freeMemory()

    WritableMemoryImpl extends WritableMemory
      - Concrete impls of Memory
      - Concrete impls of WritableMemory

      AllocateDirect extends WritableMemoryImpl implements AutoClosable
        - Allocates direct memory, uses Cleaner

*/
package com.yahoo.memory2;
