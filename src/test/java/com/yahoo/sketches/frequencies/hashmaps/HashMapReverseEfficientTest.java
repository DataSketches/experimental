package com.yahoo.sketches.frequencies.hashmaps;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.yahoo.sketches.frequencies.hashmap.HashMapWithEfficientDeletes;

public class HashMapReverseEfficientTest {

  @Test
  public void testAdjust(){
    HashMapWithEfficientDeletes hashmap = new HashMapWithEfficientDeletes(16);
    long key = 919;
    hashmap.adjust(key, 12);
    hashmap.adjust(key, 14);
    Assert.assertEquals(hashmap.get(key), 26);
  }

  @Test
  public void testDel(){
    HashMapWithEfficientDeletes hashmap = new HashMapWithEfficientDeletes(5);
    long key = 919;
    
    hashmap.adjust(key, 12);
    //hashmap.print();
    hashmap.del(key);
    //hashmap.print();
    Assert.assertEquals(hashmap.get(key), 0);
  }
  
  @Test
  public void testDel2(){
    HashMapWithEfficientDeletes hashmap = new HashMapWithEfficientDeletes(8);
    for (long key = 0;key<8;key++)
      hashmap.adjust(key,key);
    
    long key = 3;
    //hashmap.print();
    hashmap.del(key);
    //hashmap.print();
    Assert.assertEquals(hashmap.get(key), 0);
  }
  
  @Test
  public void testDelete(){
    int n = 10;
    HashMapWithEfficientDeletes hashmap = new HashMapWithEfficientDeletes(n);
    for (long key=0; key<n/2; key++){
      hashmap.adjust(key, 2);
    }
    for (long key=n/2; key<n; key++){
      hashmap.adjust(key, 1);
    }
    
    hashmap.print(); 
    hashmap.shift(1);
    hashmap.print();
  }

  //@Test
  public void testSizeAssertInGetValuse(){
    int n = 10;
    HashMapWithEfficientDeletes hashmap = new HashMapWithEfficientDeletes(n);
    for (long key=0; key<n; key++){
      hashmap.adjust(key, key);
    }
    hashmap.shift(1);
    for (long v : hashmap.getValues()){
      Assert.assertTrue(v>0);
    }
  }
  
  //@Test
  public void stressTestDeletesStats(){
    int capacity = 1000000;
    int remain = capacity/2;
    int deleted = capacity - remain;
    int rounds = 10;
    
    HashMapWithEfficientDeletes hashmap = new HashMapWithEfficientDeletes(capacity);
     
    // fill half the array such that it remains
    for (int key=remain; key-->0; ) {
      hashmap.adjust(-key ,2*rounds);
    }
    
    int key = 0;
    final long startTime = System.currentTimeMillis();
    for (int round=0; round<rounds; round++){ 
      // fill the hash map to capacity
      for (int _=deleted; _-->0; ) {
        hashmap.adjust(key%deleted ,1);
        key++;
      }
      hashmap.shift(1);
      //hashmap.print();
    }
    final long endTime = System.currentTimeMillis();
    double timePerUpdate = (double)(endTime-startTime)/(double)key;
    System.out.format("Performes %.2f million updates per second.\n", .001/timePerUpdate );
  }
  
}
