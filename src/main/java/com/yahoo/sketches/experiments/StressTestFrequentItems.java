package com.yahoo.sketches.experiments;

import com.yahoo.sketches.frequencies.FrequencyEstimator;
import com.yahoo.sketches.frequencies.FrequentItemsAbstractHash;

public class StressTestFrequentItems {
    
  static final String DATA_DIRECTORY = "/Users/edo/Workspace/datasketches/experimental/experiment/data";
  
  
  static final String[] hashMapTypes = new String[]{"RobinHood",
  																									"ReverseEfficient",
  																									"EfficientDeletes",
  																									"Trove",
  																									"TroveRebuilds", 
  																									"ProbingWithRebuilds", 
  																									"DoubleHashingWithRebuilds", 
  																									"ImplicitDeletes"
  																									};
  
  static final String[] dataTypes = new String[]{"uniform",
  																							 "exponential",
  																							 "planted",
  																							 "zipfian",
  																							 "emails"};
  
  static int[] ks = new int[]{100,1000,10000};
  static int[] sampleSizeRatios = new int[]{1,2,5,10};
  
  public static void main(String[] args) {
  	int initialCapacity = 100;
    for (int round=1; round <=10; round++){
	  	for (String dataType: dataTypes){
	      long[] keys = StreamHandler.readLongsFromFile(DATA_DIRECTORY + "/" + dataType + ".csv");
	      if (keys == null) continue;
		    for (int k: ks){
		      for (String hashMapType: hashMapTypes){
		      	for (int sampleSizeRatio : sampleSizeRatios){
		      		int sampleSize = k/sampleSizeRatio;
			      	FrequencyEstimator fi = new FrequentItemsAbstractHash(k, initialCapacity, sampleSize, hashMapType);
			      	long timePerUpdate = timeOneFrequencyEstimator(fi, keys);
			      	System.out.format("{\"hashMapType\":\"%s\","
			      									 + "\"dataType\":\"%s\","
			      									 + "\"k\":%d,"
			      									 + "\"sampleSize\":%d" 
			      									 + "\"timePerUpdate\":%d}\n", hashMapType, dataType, k, sampleSize, timePerUpdate);
		      	}
		      }
	      }
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

