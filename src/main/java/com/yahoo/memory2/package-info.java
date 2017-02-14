/*

  public Memory //has "absolute" Read-Only methods
    MemoryImpl
          MemoryDR   //Requires AutoClosable and Cleaner
          MemoryBBDR
          MapDR      //Requires AutoClosable and Cleaner
          MemoryHR
          MemoryBBHR

  public WritableMemory //has the "absolute" W methods
    WritableMemoryImpl
          MemoryDW   //Requires AutoClosable and Cleaner
          MemoryBBDW
          MapDW      //Requires AutoClosable and Cleaner
          MemoryHW
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
