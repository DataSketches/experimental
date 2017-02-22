/*
Version B
  public abstract class Memory (no class vars)
    - static RO methods: wrapping arrays, ByteBuffers, Maps
    - abstract RO methods: get<primitive>, get<primitive>Array. region

      class MemoryImpl extends Memory ()
        - Concrete impls of Memory

      public abstract class WritableMemory extends Memory (no class vars)
        - static W methods: wrapping arrays, ByteBuffers, Maps, Direct alloc, autoByteArrays
        - abstract W methods: put<primitive>, put<primitive>Array, getMemoryRequest(), freeMemory()

        class WritableMemoryImpl extends WritableMemory
          - Concrete RO impls of Memory (DUPLICATE CODE)
          - Concrete W impls of WritableMemory

          class AllocateDirect extends WritableMemoryImpl implements AutoClosable
            - Allocates direct memory, uses Cleaner

          class AllocateMap extends WritableMemory implements AutoClosable
            - Allocates direct memory, uses Cleaner (for Map)
*/
package com.yahoo.memory2;
