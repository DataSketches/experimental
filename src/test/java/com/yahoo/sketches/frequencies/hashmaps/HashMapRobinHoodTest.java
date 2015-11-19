package com.yahoo.sketches.frequencies.hashmaps;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.yahoo.sketches.frequencies.hashmap.HashMapRobinHood;

public class HashMapRobinHoodTest {
  
  //@Test
  public void number(){
    int logLength = 5;
    int length = (1<<logLength);
    int arrayMask = length-1;
    Assert.assertEquals((0)&arrayMask, 0);
    Assert.assertEquals((-2)&arrayMask, length-2);
  }
  
  @Test
  public void delete(){
    HashMapRobinHood hashmap  = new HashMapRobinHood(4);
    hashmap.adjustHash(1001,1,5);
    //hashmap.print();
    hashmap.adjustHash(1002,2,5);
    //hashmap.print();
    hashmap.adjustHash(1003,1,5);
    //hashmap.print();
    hashmap.adjustHash(1004,0,7);
    //hashmap.print();
    hashmap.adjustHash(1005,2,7);
    hashmap.adjustHash(1006,1,6);
    hashmap.adjustHash(1007,2,6);
    hashmap.print();
    
    hashmap.shift(1);
    hashmap.print();
    
  } 
}
