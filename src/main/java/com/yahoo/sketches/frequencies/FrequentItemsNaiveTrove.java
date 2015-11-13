package com.yahoo.sketches.frequencies;

import gnu.trove.iterator.TLongLongIterator;
import gnu.trove.map.hash.TLongLongHashMap;

import com.yahoo.sketches.QuickSelect;

/**
 * The frequent-items sketch is useful for keeping approximate counters for keys (map from key (long) to value (long)).  
 * The sketch is initialized with a maximal size parameter. The sketch will keep at most that number of positive counters at any given time.
 * When the sketch is updated with a key, the corresponding counter is incremented by 1 or, if there is no counter for that key, a new counter is created. 
 * If the sketch reaches its maximal allowed size, it removes some counter and decrements other.
 * The logic of the frequent-items sketch is such that the stored counts and real counts are never too different.
 * More explicitly 
 * 1) The estimate from the sketch is never larger than the real count
 * 2) The estimate from the sketch is never smaller than the real count minus the guaranteed error
 * 3) The guaranteed error is at most the number of updates to sketch divided by its maximal size.
 * 
 * Background:
 * The algorithm is most commonly known as the "Misra-Gries algorithm", "frequent items" or "space-saving". 
 * It was discovered and rediscovered and redesigned several times over the years.
 * "Finding repeated elements", Misra, Gries, 1982 
 * "Frequency estimation of internet packet streams with limited space" Demaine, Lopez-Ortiz, Munro, 2002
 * "A simple algorithm for finding frequent elements in streams and bags" Karp, Shenker, Papadimitriou, 2003
 * "Efficient Computation of Frequent and Top-k Elements in Data Streams" Metwally, Agrawal, Abbadi, 2006
 * 
 * @author edo
 */
public class FrequentItemsNaiveTrove extends FrequencyEstimator {

  private double EXPANSION_FACTOR = 1.9;
  private int maxSize;
  private int minSize;
  private long offset; 
  private TLongLongHashMap counters;
  
  /**
   * @param errorTolerance the acceptable relative error in the estimates of 
   * the sketch. The maximal error in the frequency estimate should not 
   * by more than the error tolerance times the number of updates.
   * Warning: the memory footprint of this class is inversely proportional
   * to the error tolerance!
   */
  public FrequentItemsNaiveTrove(double errorTolerance) {
    super(errorTolerance);
    minSize = (int)(1.0/errorTolerance)+1;
    maxSize = (int) (minSize*EXPANSION_FACTOR)+1;
    offset = 0;
    counters = new TLongLongHashMap();
  }
  
  public FrequentItemsNaiveTrove() {
    this(0.01);
  }
    
  /**
   * @param key a key (as long) to be added to the sketch. 
   * The key cannot be null.
   */
  @Override
  public void update(long key) {
    update(key,1);
  }

  @Override
  public void update(long key, long value) {
    counters.adjustOrPutValue(key,value,value+offset); 
    if (counters.size() >= maxSize){
      flush();
    }  
  }

  /**
   * @param key whose count estimate is returned.
   * @return the approximate count for the key.
   * It is guaranteed that
   * 1) getEstimate <= real count
   * 2) getEstimate >= real count - error tolerance 
   */
  @Override
  public long getEstimate(long key) {
    if (counters.containsKey(key)){
      long value = counters.get(key);
      return (value>offset)? value-offset : 0;
    }
    return 0;
  }

  /**
   * @param key whose count lower bound estimate is returned.
   * @return a value that is no greater than the real count. 
   */
  @Override
  public long getEstimateLowerBound(long key) {
    return getEstimate(key);
  }

  /**
   * @param key whose count upper bound estimate is returned.
   * @return a value that is no smaller than the real count. 
   */
  @Override
  public long getEstimateUpperBound(long key) {
    return getEstimate(key) + offset;
  }

  /**
   * @return an array of keys containing all keys whose frequencies are
   * are least the error tolerance.   
   */
  @Override
  public long[] getFrequentKeys() {
    return counters.keys();
  }

  /**
   * @param other FrequentItemsNaiveTrove to merge with  
   * @return a pointer to a FrequentItemsNaiveTrove whose estimates 
   * are within the guarantees of the larger error tolerance of
   * the two merged sketches.  
   */
  @Override
  public FrequencyEstimator merge(FrequencyEstimator other) {
    if (!(other instanceof FrequentItemsNaiveTrove)) throw new IllegalArgumentException("FrequentItemsNaiveTrove can only merge with other FrequentItemsNaiveTrove");
    FrequentItemsNaiveTrove otherCasted = (FrequentItemsNaiveTrove)other;
    if (getErrorTolerance() < otherCasted.getErrorTolerance()) throw new IllegalArgumentException("Can only merge with sketch of equal or lower error tolerance.");
    otherCasted.flush();
    for(long key : otherCasted.counters.keys()){
      long delta = otherCasted.counters.get(key);
      counters.adjustOrPutValue(key, delta, delta);
    }
    offset += otherCasted.offset;
    flush();
    return this;
  }

  public int size(){
    return counters.size();
  }  
  
  private void flush(){
    // Reseting the offset
    long[] values = counters.values();
    int currSize = values.length;
    if (currSize > minSize) {
      long median = QuickSelect.select(values, 0, currSize-1, currSize-minSize);
      offset = (median > offset) ? median : offset;
    }
    
    // Creating a new counter hash map 
    TLongLongHashMap tempCounters = new TLongLongHashMap(minSize);
    TLongLongIterator countersIterator = counters.iterator();
    for ( int i = counters.size(); i-- > 0; ) {
      countersIterator.advance();
      long value = countersIterator.value();
      if (value > offset){
        tempCounters.put(countersIterator.key(), countersIterator.value());
      }
    }
    counters.clear();
    counters = tempCounters;
  }
 
}
