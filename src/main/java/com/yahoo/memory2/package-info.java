/*

  public Memory //has "absolute" Read-Only methods
    MemoryImpl
          ReadOnlyDirect    //Requires AutoClosable and Cleaner
          ReadOnlyMapDirect //Requires AutoClosable and special Cleaner
          ReadOnlyBBDirect  //no cleaner
          ReadOnlyBBHeap    //no cleaner

  public WritableMemory //has the "absolute" W methods
    WritableMemoryImpl
          WritableDirect   //Requires AutoClosable and Cleaner
          WritableMapDirect //Requires AutoClosable and special Cleaner
          WritableHeap
          MemoryBBDW
          MemoryBBHW

  public PositionalMemory //has positional "Buffer" logic & variables, positional RO methods,
    PositionalMemoryImpl
          MemoryPDR.    //Requires AutoClosable and Cleaner
          MemoryBBPDR
          MapPDR.       //Requires AutoClosable and Cleaner
          MemoryPHR
          MemoryBBPHR

  public PositionalMemoryWritable //positional W methods
    PositionalWritableMemoryImpl
          MemoryPDW.   //Requires AutoClosable and Cleaner
          MemroyBBPDW
          MapPDW.      //Requires AutoClosable and Cleaner
          MemoryPHW
          MemoryBBPHW
*/
package com.yahoo.memory2;
