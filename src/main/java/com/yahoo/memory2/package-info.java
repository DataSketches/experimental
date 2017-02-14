/*

  public Memory //has "absolute" Read-Only methods
    MemoryImpl
          ReadOnlyDirect    //Requires AutoClosable and Cleaner
          ReadOnlyMapDirect //Requires AutoClosable and special Cleaner
          ReadOnlyBBDirect  //no cleaner
          ReadOnlyBBHeap    //no cleaner

  public WritableMemory //has the "absolute" Writable methods
    WritableMemoryImpl
          WritableDirect   //Requires AutoClosable and Cleaner
          WritableMapDirect //Requires AutoClosable and special Cleaner
          WritableHeap
          MemoryBBDW
          MemoryBBHW

  public PositionalMemory //has positional "Buffer" logic & variables, Read-Only methods,
    PositionalMemoryImpl
          MemoryPDR.    //Requires AutoClosable and Cleaner
          MemoryBBPDR
          MapPDR.       //Requires AutoClosable and special Cleaner
          MemoryPHR
          MemoryBBPHR

  public PositionalMemoryWritable //positional Writable methods
    PositionalWritableMemoryImpl
          MemoryPDW.   //Requires AutoClosable and Cleaner
          MemroyBBPDW
          MapPDW.      //Requires AutoClosable and special Cleaner
          MemoryPHW
          MemoryBBPHW
*/
package com.yahoo.memory2;
