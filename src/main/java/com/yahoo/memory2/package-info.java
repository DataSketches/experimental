/*
BaseMemory

  public Memory //has "absolute" Read-Only methods
      DirectR // has free
          MemoryDR   //Requires AutoClosable and Cleaner
          MemoryBBDR
          MapDR      //Requires AutoClosable and Cleaner
      HeapR
          MemoryHR
          MemoryBBHR

  public MemoryWritable //has the "absolute" W methods
      DirectW // has free
          MemoryDW   //Requires AutoClosable and Cleaner
          MemoryBBDW
          MapDW      //Requires AutoClosable and Cleaner
      HeapW
          MemoryHW
          MemoryBBHW

  public PositionalMemory //has positional "Buffer" logic & variables, positional RO methods,
      DirectPR
          MemoryPDR.    //Requires AutoClosable and Cleaner
          MemoryBBPDR
          MapPDR.       //Requires AutoClosable and Cleaner
      HeapPPR
          MemoryPHR
          MemoryBBPHR

  public PositionalMemoryWritable //positional W methods
      DirectPW
          MemoryPDW.   //Requires AutoClosable and Cleaner
          MemroyBBPDW
          MapPDW.      //Requires AutoClosable and Cleaner
      HeapPW
          MemoryPHW
          MemoryBBPHW
*/
package com.yahoo.memory2;
