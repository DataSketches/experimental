/*
  Version A
  public abstract class Memory (no class vars)
    - public static RO methods: wrapping arrays, ByteBuffers, Maps
    - public abstract RO methods: get<primitive>, get<primitive>Array. region

      class MemoryImpl extends Memory
        - Concrete impls of Memory

      public class WritableMemory extends MemoryImpl
        - public static W methods: wrapping arrays, ByteBuffer, Map, Direct alloc, autoByteArray
        - public W methods: put<primitive>, put<primitive>Array, getMemoryRequest(), freeMemory()

          class AllocateDirect extends WritableMemory implements AutoClosable
            - Allocates direct memory, uses Cleaner

          class AllocateMap extends WritableMemory implements AutoClosable
            - Allocates direct memory, uses Cleaner (for Map)
*/
package com.yahoo.memory1;
