package com.yahoo.sketches.frequencies;

import java.util.Arrays;

import com.yahoo.sketches.hashmaps.HashMapReverseEfficient;

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
 * @author Justin8712
 */
public class FrequentItems extends FrequencyEstimator{

  private HashMapReverseEfficient counters;
  private double threshold;
  private int offset;
  private int mergeError;
  private int streamLength=0;
  private int maxSize;
  
  /**
   * @param maxSize (must be positive)
   * Gives the maximal number of positive counters the sketch is allowed to keep.
   * This should be thought of as the limit on its space usage. The size is dynamic.
   * If fewer than maxSize different keys are inserted the size will be smaller 
   * that maxSize and the counts will be exact.  
   */
  public FrequentItems(double errorParameter) {
    if (errorParameter <= 0) throw new IllegalArgumentException("Received negative or zero value for maxSize.");
    this.maxSize = (int) (1/errorParameter)+1;
    counters = new HashMapReverseEfficient(this.maxSize);
    this.threshold =  .75 * this.maxSize;
    this.offset = 0;
  }
  
  /**
   * @return the number of positive counters in the sketch.
   */
  public long nnz() {
    return counters.getSize();
  }
  
  @Override
  public long getEstimate(long key) { 
    //the logic below returns the count of associated counter if key is tracked.
    //If the key is not tracked and fewer than maxSize counters are in use, 0 is returned.
    //Otherwise, the minimum counter value is returned.

    if(counters.get(key) > 0) 
      return counters.get(key) + offset;
    else
      return 0;
  }
  
   /**
    * @param key whose count estimate is returned.
    * @return an upper bound on the count for the key.
    */
    @Override
   public long getEstimateUpperBound(long key)
   {
     long estimate = getEstimate(key);
     if(estimate > 0)
       return estimate + mergeError;

     return mergeError + offset;

   }
   
   /**
    * @param key whose count estimate is returned.
    * @return a lower bound on the count for the key.
    */
    @Override
   public long getEstimateLowerBound(long key)
   {
     if(getEstimate(key) == 0)
       return 0;
       
     if((getEstimate(key)-offset-mergeError) < 0)
       return 0;
       
     return (getEstimate(key)-offset-mergeError);
   }

  /**
   * @return the maximal error of the estimate one gets from get(key).
   * Note that the error is one sided. if the real count is realCount(key) then
   * get(key) <= realCount(key) <= get(key) + getMaxError() 
   */
  public int getMaxError() {
      return offset + mergeError;
  }
  
  /**
   * @param key 
   * Process a key (specified as a long) update and treat the increment as 1
   */
   @Override  
  public void update(long key) {
    update(key, 1);
  }


  
  /**
   * @param key 
   * A key (as long) to be added to the sketch. The key cannot be null.
   */
   @Override
  public void update(long key, long increment) {
    this.streamLength += increment;
    counters.adjust(key, increment);  
    if(counters.getSize() > this.threshold) {
      //System.out.println("About to purge");
      purge();
      //System.out.println("Done purging");
    }
  }

  public void purge()
  {
    //System.out.format("hit capacity of: %f\n, purging.", this.threshold);
    int sample_size = 100;
    if (sample_size > counters.getSize())
      sample_size = counters.getSize();
    long samples[] = new long[sample_size]; 
    long[] values = counters.ProtectedGetValues();
    int num_samples = 0;
    int i = 0;
    //System.out.println("Starting while loop in purge");
    while(num_samples < sample_size){
      if(counters.isActive(i)){
        samples[num_samples] = values[i];
        num_samples++;
      }
      i++;
    }
    //System.out.println("Through while loop in purge");
    
    Arrays.sort(samples);
    long val = samples[sample_size/2];
    //long val = QuickSelect.select(samples, 0, sample_size-1, sample_size/2);
    //System.out.format("about to adjust all values by: %d \n", val);
    counters.adjustAllValuesBy(-1 * val);
    //System.out.format("about to keep only larger than 0 \n");
    counters.keepOnlyLargerThan(0);
    //System.out.format("Changing offset by %d \n", val);
    this.offset += val; 
    //System.out.format("offset now %d \n", this.offset);
  }
  /**
   * @param that
   * Another FrequentItems sketch. Potentially of different size. 
   * @return pointer to the sketch resulting in adding the approximate counts of another sketch. 
   * This method does not create a new sketch. The sketch whose function is executed is changed.
   */
  public FrequencyEstimator mergeSlow(FrequencyEstimator other) {
    if (!(other instanceof FrequentItems)) throw new IllegalArgumentException("SpaceSaving can only merge with other SpaceSaving");
      FrequentItems otherCasted = (FrequentItems) other;
      
    this.offset += otherCasted.offset;
      
    long[] other_values = otherCasted.counters.ProtectedGetValues();
    long[] other_keys = otherCasted.counters.ProtectedGetKey();
      
    for(int i = 0; i < otherCasted.counters.getLength(); i++) {
      this.update(other_keys[i], other_values[i]);
    }
    return this;
  }
  
   /**
    * @param other
    * Another FrequentItems sketch. Potentially of different size. 
    * @return pointer to the sketch resulting in adding the approximate counts of another sketch. 
    * This method does not create a new sketch. The sketch whose function is executed is changed.
    */
    @Override
   public FrequencyEstimator merge(FrequencyEstimator other) {
     if (!(other instanceof FrequentItems)) throw new IllegalArgumentException("SpaceSaving can only merge with other SpaceSaving");
       FrequentItems otherCasted = (FrequentItems) other;
       
     this.streamLength += otherCasted.streamLength;
     this.mergeError += otherCasted.getMaxError();
     
     long[] other_keys = otherCasted.counters.getKeys();
     long[] other_values = otherCasted.counters.getValues();
     
     //System.out.format("merging other sketch of length %d \n,", other_keys.length);  
     for (int i=other_keys.length; i-->0;) { 
       this.update(other_keys[i], other_values[i]);
     }
       
     return this;
   }
  
   @Override
   public long[] getFrequentKeys() {
     int count = 0;
     long[] keys = counters.ProtectedGetKey();
     
     for(int i = 0; i < counters.getLength(); i++) {
       if(counters.isActive(i) && (getEstimate(keys[i]) >= (this.streamLength / this.maxSize))) {
         count++;
       }
     }
     
     long[] freq_keys = new long[count];
     count = 0;
     for(int i = counters.getLength(); i-->0;) {
       if(counters.isActive(i) && (getEstimate(keys[i]) >= (this.streamLength / this.maxSize))) {
         if(keys[i] == 0){
          System.out.println("asdifunasdfuoasndiuafsdnliasdfnu\n");
          System.exit(1);
         }
         freq_keys[count] = keys[i];
         count++;
       }
     }

     return freq_keys;
   }

}
