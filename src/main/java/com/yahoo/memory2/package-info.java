/*
Version B
  public abstract class Memory implements AutoCloseable
    - static RO methods: wrapping arrays, ByteBuffers, Maps
    - abstract RO methods: get<primitive>, get<primitive>Array, region, etc

      public abstract class WritableMemory extends Memory
        - static W methods: wrapping arrays, ByteBuffers, Maps, Direct alloc, autoByteArrays
        - abstract W methods: put<primitive>, put<primitive>Array, getMemoryRequest(), freeMemory()

        class WritableMemoryImpl extends WritableMemory
          - Concrete RO impls of Memory; Return Memory
          - Concrete W impls of WritableMemory; Return WritableMemory

          class AllocateDirect extends WritableMemoryImpl
            - Allocates direct memory, uses Cleaner

          class AllocateDirectMap extends WritableMemoryImpl
            - Allocates direct memory, uses Cleaner (for Map)

      class AccessByteBuffer
*/
package com.yahoo.memory2;
