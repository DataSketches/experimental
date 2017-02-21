/*
  Version A
  public abstract class Memory (no class vars) -- Default Read-only
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


Version B
  public abstract class Memory (no class vars) -- Default Read-only
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

          class AccessByteBuffer

 Version C
   public abstract class Memory (no class vars) -- Default Writable
    - static methods: wrapping arrays, ByteBuffers, Maps
    - abstract methods: get<primitive>, get<primitive>Array, region, asReadOnly, others

      class MemoryImpl extends Memory ()
        - Concrete impls of Memory

        class AllocateDirect extends WritableMemoryImpl implements AutoClosable
          - Allocates direct memory, uses Cleaner

        class AllocateMap extends WritableMemory implements AutoClosable
          - Allocates direct memory, uses Cleaner (for Map)

      class AccessByteBuffer
*/
package com.yahoo.memory3;
