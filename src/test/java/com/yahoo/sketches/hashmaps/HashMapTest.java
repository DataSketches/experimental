/*
 * Copyright 2016, Yahoo! Inc. Licensed under the terms of the Apache License 2.0. See LICENSE file
 * at the project root for terms.
 */

package com.yahoo.sketches.hashmaps;

import java.util.Random;

import org.testng.Assert;
import org.testng.annotations.Test;

import gnu.trove.function.TLongFunction;
import gnu.trove.map.hash.TLongLongHashMap;
import gnu.trove.procedure.TLongLongProcedure;

public class HashMapTest {
  
  static private HashMap newHashMap(int capacity, int i){
    switch (i){
      case 0: return new HashMapTrove(capacity);
      case 1: return new HashMapTroveRebuilds(capacity);
      case 2: return new HashMapLinearProbingWithRebuilds(capacity);
      case 3: return new HashMapDoubleHashingWithRebuilds(capacity);
      case 4: return new HashMapWithImplicitDeletes(capacity);
      case 5: return new HashMapWithEfficientDeletes(capacity);
      case 6: return new HashMapRobinHood(capacity);   
      case 7: return new HashMapReverseEfficient(capacity);
      case 8: return new HashMapReverseEfficientOneArray(capacity);
    } 
    return null;
  }

  @Test
  public void testAllHashMapsCorrect(){
    int capacity = 127;
    Random random = new Random(); 
    random.setSeed(422);
    int valueRange = 35219;
    int keyRange = 11173;
     
    // Looping over all hashMap types
    for (int h=0; h<10 ;h++){
      HashMap hashmap = newHashMap(capacity, h);
      if (hashmap == null) continue;
      String s = String.format("Test: %s\n", hashmap.getClass().getSimpleName());
      println(s);
      
      // correct is a the gold standard
      TLongLongHashMap correct = new TLongLongHashMap(capacity);
      // Insert random keys and values
      for (int i=0; i<capacity;i++) {
        long key = random.nextInt(keyRange);
        long value = random.nextInt(valueRange);
        hashmap.adjustOrPutValue(key ,value, value);
        correct.adjustOrPutValue(key, value ,value);
      }      
      
      // remove a bunch of values
      long threshold = valueRange/2;
      
      hashmap.adjustAllValuesBy(-threshold);
      hashmap.keepOnlyLargerThan(0);
      
      correct.retainEntries(new GreaterThenThreshold(threshold));
      correct.transformValues(new decreaseByThreshold(threshold));
      
      
      
      long[] keys = hashmap.getKeys();
      long[] values = hashmap.getValues();
      int size = hashmap.getSize();
      
      // map is of the correct size
      Assert.assertEquals(correct.size(), size);
      // keys and values of the same correct length
      Assert.assertEquals(size, keys.length);
      Assert.assertEquals(size, values.length);
      
      // All the keys and values are correct
      for (int i=0;i<size;i++){
        Assert.assertTrue(correct.containsKey(keys[i]));
        Assert.assertEquals(correct.get(keys[i]), values[i]);
        // Testing the get function
        Assert.assertEquals(values[i], hashmap.get(keys[i]));
      }
      
    }
  }
   
  private class GreaterThenThreshold implements TLongLongProcedure {
    long threshold;
    public GreaterThenThreshold(long threshold){
      this.threshold = threshold;
    }
    
    @Override
    public boolean execute(long key, long value) {
      return (value > threshold);
    }
  }
  
  private class decreaseByThreshold implements TLongFunction {
    long threshold;
    public decreaseByThreshold(long threshold){
      this.threshold = threshold;
    }
    
    @Override
    public long execute(long value) {
      return value - threshold;
    }    
  }
  
  @Test
  public void printlnTest() {
    println("PRINTING: " + this.getClass().getName());
  }

  /**
   * @param s value to print
   */
  static void println(String s) {
    // System.out.println(s); //disable here
  }
  
}