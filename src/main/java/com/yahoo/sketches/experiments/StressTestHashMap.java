/*
 * Copyright 2016, Yahoo! Inc. Licensed under the terms of the Apache License 2.0. See LICENSE file
 * at the project root for terms.
 */

package com.yahoo.sketches.experiments;

import com.yahoo.sketches.hashmaps.*;

public class StressTestHashMap {
  final static int NUM_HASHMAP_CLASSES = 9;
  final static int NUM_STREAM_TYPES = 5;
  
  public static void main(String[] args) {
      stress();
  }
  
  private static void stress() {
    for (int capacity = 2 << 5; capacity < 2 << 24; capacity *= 2) {
      for (int s = 0; s < NUM_STREAM_TYPES; s++) {
        for (int h = 0; h < NUM_HASHMAP_CLASSES; h++) {
          HashMap hashmap = hashMapFactory(capacity, h);
          long[] stream = streamFactory(s);
          long timePerAdjust = timeOneHashMap(hashmap, stream);
          System.out.format("%s\t%s\t%d\t%d\n", hashmap.getClass().getSimpleName(), streamNames()[s], capacity, timePerAdjust);
	}
      }
    }
  }

  private static long timeOneHashMap(HashMap hashmap, long[] keys) {
    final long startTime = System.nanoTime();
    int hashmapCapacity = hashmap.getCapacity();
    for (long key: keys) {
      hashmap.adjust(key, 1);
      if (hashmap.getSize() >= hashmapCapacity) {
        hashmap.adjustAllValuesBy(-1);
        hashmap.keepOnlyLargerThan(0);
      }
    }
    final long endTime = System.nanoTime();
    return (endTime - startTime) / keys.length;
  }

  static private HashMap hashMapFactory(int capacity, int i) {
    switch (i) {
      case 0:
        return new HashMapTrove(capacity);
      case 1:
        return new HashMapTroveRebuilds(capacity);
      case 2:
        return new HashMapLinearProbingWithRebuilds(capacity);
      case 3:
        return new HashMapDoubleHashingWithRebuilds(capacity);
      case 4:
        return new HashMapWithImplicitDeletes(capacity);
      case 5:
        return new HashMapWithEfficientDeletes(capacity);
      case 6:
        return new HashMapRobinHood(capacity);
      case 7:
        return new HashMapReverseEfficient(capacity);
      case 8:
        return new HashMapReverseEfficientOneArray(capacity);
    }
    return null;
  }
  
  static private String[] streamNames(){
      return new String[]{"uniform","emails","exponential","planted","zipfian"};
  }
  
  static private long[] streamFactory(int s) {
    try{
      return StreamHandler.readLongsFromFile("data/" + streamNames()[s] + ".csv");
    } catch (Exception e){
      e.printStackTrace();
    }
    return null;
  }
}
