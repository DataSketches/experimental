package com.yahoo.sketches.frequencies;

import gnu.trove.iterator.TLongLongIterator;
import gnu.trove.map.hash.TLongLongHashMap;
import gnu.trove.procedure.TLongLongProcedure;

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
public class FrequentItemsNaiveTrove {

  private TLongLongHashMap counters;
  private int maxSize;
  private long offset;
  private boolean inplaceModeOn = false; 
  private TLongLongProcedure valueGreaterThanOffset;
  
  /**
   * @param maxSize (must be positive)
   * Gives the maximal number of positive counters the sketch is allowed to keep.
   * This should be thought of as the limit on its space usage. The size is dynamic.
   * If fewer than maxSize different keys are inserted the size will be smaller 
   * that maxSize and the counts will be exact.  
   */
  public FrequentItemsNaiveTrove(int maxSize) {
    if (maxSize <= 0) throw new IllegalArgumentException("Received negative or zero value for maxSize.");
    this.maxSize = maxSize;
    offset = 0;
    counters = new TLongLongHashMap();
    if (inplaceModeOn) {
      valueGreaterThanOffset = new ValueGreaterThanOffset();
    }
  }

  /**
   * @param maxSize (must be positive)
   * Gives the maximal number of positive counters the sketch is allowed to keep.
   * This should be thought of as the limit on its space usage. The size is dynamic.
   * If fewer than maxSize different keys are inserted the size will be smaller 
   * that maxSize and the counts will be exact.
   * @param inplaceModeOn boolean false by default. Turning on inplaceModeOn
   * makes the data structure more space efficient but roughly 50% times slower.  
   */
  public FrequentItemsNaiveTrove(int maxSize, boolean inplaceModeOn) {
    if (maxSize <= 0) throw new IllegalArgumentException("Received negative or zero value for maxSize.");
    this.maxSize = maxSize;
    offset = 0;
    counters = new TLongLongHashMap();
    this.inplaceModeOn = inplaceModeOn;
    if (inplaceModeOn) {
      valueGreaterThanOffset = new ValueGreaterThanOffset();
    }
  }
  /**
   * @return the number of positive counters in the sketch.
   */
  public long nnz() {
    return counters.size();
  }
  
  /**
   * @param key whose count estimate is returned.
   * @return the approximate count for the key.
   * It is guaranteed that
   * 1) get(key) <= real count
   * 2) get(key) >= real count - getMaxError() 
   */
  public long get(long key) {
    return counters.containsKey(key) ? counters.get(key)-offset : 0L; 
  }

  /**
   * @return the maximal error of the estimate one gets from get(key).
   * Note that the error is one sided. if the real count is realCount(key) then
   * get(key) <= realCount(key) <= get(key) + getMaxError() 
   */
  public long getMaxError() {
    return offset;
  }
  
  /**
   * @author edo
   * This class is needed for in-place filtering of the Trove hashmap.
   * Removing in place is slower but more space efficient.
   */
  private final class ValueGreaterThanOffset implements TLongLongProcedure {
    @Override
    public final boolean execute(long key, long value) {
        return value > offset;
    }
  }
  
  private void flush(){
    if (inplaceModeOn) {
      counters.retainEntries(valueGreaterThanOffset);
    } else {
      TLongLongHashMap tempCounters = new TLongLongHashMap(maxSize);
      TLongLongIterator countersIterator = counters.iterator();
      for ( int i = counters.size(); i-- > 0; ) {
        countersIterator.advance();
        long value = countersIterator.value();
        if (value > offset){
          tempCounters.put(countersIterator.key(), countersIterator.value() );
        }
      }
      counters.clear();
      counters = tempCounters;
    }
  }
  
  /**
   * @param key a key (as long) to be added to the sketch. 
   * The key cannot be null.
   */
  public void update(long key) {
    counters.adjustOrPutValue(key,1,1+offset); 
    if (counters.size() > maxSize){
      offset+=1;
      flush();
    }
  }

  /**
   * @param other a different FrequentItemsNaiveTrove to union with.
   * The result of this union updates the sketch whose union function is applied.
   * @return this the reference to the updated sketch. 
   */
  public FrequentItemsNaiveTrove union(FrequentItemsNaiveTrove other) {
    other.flush();
    for(long key : other.counters.keys()){
      long delta = other.counters.get(key);
      counters.adjustOrPutValue(key, delta, delta+offset);
    }
    offset += other.offset;
    flush();
    return this;
  }
  
}
