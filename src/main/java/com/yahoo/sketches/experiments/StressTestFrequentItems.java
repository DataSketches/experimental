package com.yahoo.sketches.experiments;

import com.yahoo.sketches.frequencies.FrequencyEstimator;
import com.yahoo.sketches.frequencies.FrequentItemsAbstractHash;
//import com.yahoo.sketches.hashmaps.HashMap;

public class StressTestFrequentItems {
    
  static final String DATA_DIRECTORY = "/Users/edo/Workspace/datasketches/experimental/experiment/data";
  
  static final String[] hashMapTypes = new String[]{"RobinHood",
  																									"ReverseEfficient",
  																									"EfficientDeletes",
  																									"Trove",
  																									"TroveRebuilds", 
  																									"ProbingWithRebuilds", 
  																									"DoubleHashingWithRebuilds", 
  																									"ImplicitDeletes"};
  
  static final String[] dataTypes = new String[]{"uniform",
  																							 "emails",
  																							 "exponential",
  																							 "planted",
  																							 "zipfian"};
    
  public static void main(String[] args) {
    for (String dataType: dataTypes){
      long[] keys = StreamHandler.readLongsFromFile(DATA_DIRECTORY + "/" + dataType + ".csv");
      for (String hashMapType: hashMapTypes){
      	FrequencyEstimator fi = new FrequentItemsAbstractHash(10000, 10, hashMapType);
      	long timePerUpdate = timeOneFrequencyEstimator(fi, keys);
      	System.out.format("%s\t%s\t%d\n", hashMapType, dataType, timePerUpdate);
      }
    }
  }
  
	private static long timeOneFrequencyEstimator(FrequencyEstimator fi, long[] keys) {
		final long startTime = System.nanoTime();
			for (long key: keys) fi.update(key, 1);
      final long endTime = System.nanoTime();
      return (endTime - startTime) / keys.length;
    }
  
}

