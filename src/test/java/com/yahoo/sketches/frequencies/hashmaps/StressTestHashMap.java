package com.yahoo.sketches.frequencies.hashmaps;

import com.yahoo.sketches.frequencies.hashmap.*;

public class StressTestHashMap {
  
  public static void main(String[] args) {
    int capacity = 1000;
    int remain = capacity/2;
    int deleted = capacity - remain;
    int rounds = 10000;
                                     
    for (int h=0; h<10 ;h++){
      HashMap hashmap = newHashMap(capacity, h);
      if (hashmap == null) continue;
         
      // Enter some keys such that they remain
      int key = 0;
      for (int i=0; i<remain;i++) {
        hashmap.adjust(key++ ,2*rounds);
      }
  
      final long startTime = System.currentTimeMillis();
      for (int round=0; round<rounds; round++){ 
        // fill the hash map to capacity
        for (int i=0; i<deleted;i++) hashmap.adjust(key++ ,1);
        hashmap.shift(1);
      }
      final long endTime = System.currentTimeMillis();
      double timePerUpdate = (double)(endTime-startTime)/(double)key;
      System.out.format("%s Performes %.2f million updates per second.\n",
          hashmap.getClass().getSimpleName(),
          0.001/timePerUpdate );
    }
  }

  static private HashMap newHashMap(int capacity, int i){
    switch (i){
      case 0: return new HashMapTrove(capacity);
      case 1: return new HashMapTroveRebuilds(capacity);
      case 2: return new HashMapLinearProbingWithRebuilds(capacity);
      case 3: return new HashMapDoubleHashingWithRebuilds(capacity);
      case 4: return new HashMapWithImplicitDeletes(capacity);
      case 5: return new HashMapWithEfficientDeletes(capacity);
      case 6: return new HashMapRobinHood(capacity);
       
    }
    return null;
  }

}
