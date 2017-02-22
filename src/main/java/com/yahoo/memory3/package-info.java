/*
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
