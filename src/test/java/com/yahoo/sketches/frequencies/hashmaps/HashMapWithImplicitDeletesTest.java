package com.yahoo.sketches.frequencies.hashmaps;


import org.testng.Assert;
import org.testng.annotations.Test;

import com.yahoo.sketches.frequencies.hashmap.HashMapWithImplicitDeletes;

public class HashMapWithImplicitDeletesTest {
  
  //@Test
  public void testAdjust(){
    HashMapWithImplicitDeletes hashmap = new HashMapWithImplicitDeletes(16);
    long key = 919;
    
    hashmap.adjust(key, 12);
    hashmap.adjust(key, 14);
    Assert.assertEquals(hashmap.get(key), 26);
  }

  //@Test
  public void testDelete(){
    int n = 10;
    HashMapWithImplicitDeletes hashmap = new HashMapWithImplicitDeletes(n);
    for (long key=0; key<n; key++){
      hashmap.adjust(key, key);
    }
    hashmap.shift(1);
    hashmap.print();
   
    for (long key=0; key<n; key++){
      hashmap.adjust(key, key);
    }
    hashmap.print();
  }

  //@Test
  public void testSizeAssertInGetValuse(){
    int n = 10;
    HashMapWithImplicitDeletes hashmap = new HashMapWithImplicitDeletes(n);
    for (long key=0; key<n; key++){
      hashmap.adjust(key, key);
    }
    hashmap.shift(1);
    for (long v : hashmap.getValues()){
      Assert.assertTrue(v>0);
    }
  }
  
  //@Test
  public void insertZeroKey(){
    int n = 4;
    HashMapWithImplicitDeletes hashmap = new HashMapWithImplicitDeletes(n);
    hashmap.adjust(0,12);  
  }
  
  //@Test
  public void stressTestDeletesStats(){
    int capacity = 10;
    int rounds = 7;
    HashMapWithImplicitDeletes hashmap = new HashMapWithImplicitDeletes(capacity);
  
    for (int key=0; key<capacity/2; key++) {
      hashmap.adjust(key ,2*rounds);
    }
    hashmap.print();
    System.out.println("^^^^^ First ^^^^^^");
    
    int uniqeKey = capacity/2;
    for (int round=0; round<rounds; round++){ 
      for (int key=capacity/2; key<capacity; key++) {
        hashmap.adjust(uniqeKey++ ,1);
      }
      hashmap.print();
      System.out.println("^^^^^ after adjust ^^^^^");
      hashmap.shift(1);
      hashmap.print();
      System.out.println("^^^^^ after delete ^^^^^");
    }
    //hashmap.print();
  }
  
  @Test
  public void stressTestDeletesStats2(){
    int capacity = 10000;
    int remain = capacity/2;
    int deleted = capacity - remain;
    int rounds = 300;
    
    HashMapWithImplicitDeletes hashmap = new HashMapWithImplicitDeletes(capacity);
     
    // fill half the array such that it remains
    for (int key=remain; key-->0; ) {
      hashmap.adjust(-key ,2*rounds);
    }
    
    int key = 0;
    final long startTime = System.currentTimeMillis();
    for (int round=0; round<rounds; round++){ 
      // fill the hashmap to capacity
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
