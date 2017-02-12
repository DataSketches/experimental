/*
BaseMemory // Thought abstraction

  public Memory //has "absolute" Read-Only methods and launches the rest using factory methods
      DirectR // Thought abstraction.
          MemoryDR   //Requires AutoClosable and Cleaner
          MemoryBBDR
          MapDR      //Requires AutoClosable and Cleaner
      HeapR // Thought abstraction
          MemoryHR
          MemoryBBHR

      MemoryW //has the "absolute" W methods
          DirectW // Thought abstraction.
              MemoryDW   //Requires AutoClosable and Cleaner
              MemoryBBDW
              MapDW      //Requires AutoClosable and Cleaner
          HeapW // Thought abstraction
              MemoryHW
              MemoryBBHW

  public PositionalMemory //has positional "Buffer" logic & variables, positional RO methods,
        //and launches the rest
      DirectPR // Thought abstraction.
          MemoryPDR.    //Requires AutoClosable and Cleaner
          MemoryBBPDR
          MapPDR.       //Requires AutoClosable and Cleaner
      HeapPPR // Thought abstraction
          MemoryPHR
          MemoryBBPHR

      MemoryPW //positional W methods
          DirectPW // Thought abstraction.
              MemoryPDW.   //Requires AutoClosable and Cleaner
              MemroyBBPDW
              MapPDW.      //Requires AutoClosable and Cleaner
          HeapPW // Thought abstraction
              MemoryPHW
              MemoryBBPHW
*/
package com.yahoo.memory2;
