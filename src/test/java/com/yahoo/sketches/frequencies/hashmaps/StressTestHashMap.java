package com.yahoo.sketches.frequencies.hashmaps;

import org.junit.Assert;
//import org.testng.Assert;
import org.testng.annotations.Test;

import com.yahoo.sketches.frequencies.hashmap.HashMap;
import com.yahoo.sketches.frequencies.hashmap.HashMapLinearProbingWithRebuilds;
import com.yahoo.sketches.frequencies.hashmap.HashMapDoubleHashingWithRebuilds;
import com.yahoo.sketches.frequencies.hashmap.HashMapWithEfficientDeletes;
import com.yahoo.sketches.frequencies.hashmap.HashMapWithImplicitDeletes;
import com.yahoo.sketches.frequencies.hashmap.HashMapRobinHood;

public class StressTestHashMap {
  
  @Test
  public void mainTest(){
    stressTestDeletesStats();
  }
  
  static private HashMap newHashMap(int capacity, int i){
    switch (i){
      case 0: return new HashMapLinearProbingWithRebuilds(capacity);
      case 1: return new HashMapDoubleHashingWithRebuilds(capacity);
      case 2: return new HashMapWithEfficientDeletes(capacity);
      case 3: return new HashMapWithImplicitDeletes(capacity);
      case 4: return new HashMapRobinHood(capacity);
      
    }
    return null;
  }
  
  public void stressTestDeletesStats(){
    int capacity = 10000;
    int remain = capacity/2;
    int deleted = capacity - remain;
    int rounds = 200;
                                     
    for (int hashMapClassIndex =0;  hashMapClassIndex<5; hashMapClassIndex++){
         
      HashMap hashmap = newHashMap(capacity,hashMapClassIndex);
      // Enter some keys such that they remain
      int key = 0;
      for (int i=0; i<remain;i++) {
        hashmap.adjust(key++ ,2*rounds);
      }
  
      final long startTime = System.currentTimeMillis();
      for (int round=0; round<rounds; round++){ 
        // fill the hash map to capacity
        for (int i=0; i<deleted;i++) hashmap.adjust(key++ ,1);
        Assert.assertEquals(hashmap.getSize(), capacity);
        // and delete all added items
        hashmap.shift(1);
        Assert.assertEquals(remain, hashmap.getSize());
      }
      final long endTime = System.currentTimeMillis();
      double timePerUpdate = (double)(endTime-startTime)/(double)key;
      System.out.format("%s Performes %.2f million updates per second.\n",
          hashmap.getClass().getSimpleName(),
          0.001/timePerUpdate );
    }
  }
}
