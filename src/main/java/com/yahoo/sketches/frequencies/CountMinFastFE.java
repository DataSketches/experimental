package com.yahoo.sketches.frequencies;


import com.yahoo.sketches.hash.MurmurHash3;

import gnu.trove.set.hash.TLongHashSet;
import gnu.trove.iterator.TLongIterator;

/**
 * The Count-Min sketch of Cormode and Muthukrishnan is useful for approximately answering point queries, i.e.,
 * queries of the form "what is the frequency of key i"? It can also answer other queries as well
 * (range queries, inner product queries, heavy hitters, quantiles, etc.), though it incurs significant
 * overheads for some of these other queries.
 * 
 * 
 * @author Justin8712
 */


//@SuppressWarnings("cast")
public class CountMinFastFE extends FrequencyEstimator{
  
  //queue will store counters and their associated keys 
  //for fast access to smallest counter. 
  //counts will also store counters and their associated 
  //keys to quickly check if a key is currently assigned a counter.
  
  private int hashes;
  private int length;
  private int arrayMask;
  private long update_sum;
  private long[] counts;
  private long[] keyArr = new long[1];
  private double eps;
  private int logLength;
  private TLongHashSet freq_keys;
  private int freq_limit;
  
  private static final int STRIDE_HASH_BITS = 30; 
  static final int STRIDE_MASK = (1 << STRIDE_HASH_BITS) - 1;
  
  
  /**
   * @param eps, delta
   * The guarantee of the sketch is that the answer returned to any individual
   * point query will, with probability at least 1-delta, 
   * be accurate to error plus or minus eps*F, where 
   * F is the sum of all the increments the sketch has processed.
   * 
   */    
  public CountMinFastFE(double eps, double delta) {
	if (eps <= 0 || delta <= 0){
	  throw new IllegalArgumentException("Received negative or zero value for eps or delta.");
	}
    this.eps = eps;
    this.hashes = (int) (Math.ceil(Math.log(1/delta)/Math.log(2.0)) ); 
    int columns  = (int) (2*Math.ceil(1/eps));
    this.length = columns*this.hashes;
    
    this.length = Integer.highestOneBit(2*(this.length-1));
    this.logLength = Integer.numberOfTrailingZeros(this.length);
    this.arrayMask = length-1; 
    
    counts = new long[this.length];
    for(int i = 0; i < this.length; i++){
      counts[i] = 0;
    }
    this.update_sum = 0;
    
    this.freq_limit = 2*(int)(1/eps);
    this.freq_keys = new TLongHashSet(this.freq_limit);
    
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
   * Process a key (specified as a long) update and treat the increment as 1
   */	
   
  public void conservative_update(long key) {
  	conservative_update(key, 1);
  }
  
  /**
   * @param key to be hashed
   * @return an index into the hash table 
   */
  protected long hash(long key){
    keyArr[0] = key;
    return MurmurHash3.hash(keyArr,0)[0];
  }

  /**
   * @param key 
   * Process a key (specified as a long) and an increment (CANNOT be negative, because of the way
   * we are tracking frequent items).
   */	
   @Override
  public void update(long key, long increment) {
	if (increment <= 0) throw new IllegalArgumentException("Received negative or zero value for increment.");
    this.update_sum += increment;
    int is_freq = 0;
    
    long hash = hash(key);
    // make odd and independent of the probe:
    int stride = ((int) ((hash >> logLength) & STRIDE_MASK)<< 1) + 1;
    int probe = (int) (hash & arrayMask);
    
	for (int i=this.hashes; i-->0;) {
	  counts[probe] += increment;
	  if(counts[probe] >= this.eps * this.update_sum){
	    is_freq = 1;
	  }
	  probe = (probe + stride) & arrayMask;
	}
	if(is_freq == 1){
	  this.freq_keys.add(key);
	}
	if(this.freq_keys.size() > this.freq_limit){
	  purge();
	}
  }
  
  /**
   * @param key 
   * Process a key (specified as a long) and an increment (can be negative).
   */	
  public void conservative_update(long key, long increment) {
	this.update_sum +=increment;
  	long min_count = Long.MAX_VALUE;
  	
  	long hash = hash(key);
    // make odd and independent of the probe:
    int stride = ((int) ((hash >> logLength) & STRIDE_MASK)<< 1) + 1;
    int probe = (int) (hash & arrayMask);
    
    for (int i=this.hashes; i-->0;) {
	  if(counts[probe] < min_count){
	    min_count = counts[probe];
	  }
	  probe = (probe + stride) & arrayMask;
	}
	
    probe = (int) (hash & arrayMask);
	for (int i=0; i < this.hashes; i++) {
	  if(counts[probe] < min_count + increment){
	    counts[probe] = min_count + increment;
	  }
	  probe = (probe + stride) & arrayMask;
	}
	if(min_count + increment >= this.eps * this.update_sum){
	  this.freq_keys.add(key);
	}
	if(this.freq_keys.size() > this.freq_limit){
	  purge();
	}
  }
  
  public void purge(){
    TLongHashSet newset = new TLongHashSet(this.freq_limit);
    TLongIterator it = this.freq_keys.iterator();
    long threshold = (long) (eps*this.update_sum);
    for ( int i = this.freq_keys.size(); i-- > 0; ) {
	  long key = it.next();
	  if(getEstimate(key) >= threshold){
	    newset.add(key);
      }
    }
    this.freq_keys = newset;
  }
  
  
  /**
   * @param key whose count estimate is returned.
   * @return the approximate count for the key.
   * It is guaranteed that with probability at least 1-delta
   * 1) get(key) >= real count
   * 2) get(key) <= real count + getMaxError() 
   */
   @Override
  public long getEstimate(long key) { 
	keyArr[0] = key;
	long min_count = Long.MAX_VALUE;
	
  	long hash = hash(key);
    // make odd and independent of the probe:
    long stride = ((int) ((hash >> logLength) & STRIDE_MASK)<< 1) + 1;
    int probe = (int) (hash & arrayMask);
	
	for (int i=0; i < this.hashes; i++) {
	  if(counts[probe] < min_count){
	    min_count = counts[probe];
	  }
	  probe = (int) ((probe + stride) & arrayMask);
	}
	return min_count;
  }
  
  @Override
  public long getEstimateUpperBound(long key) { 
	return getEstimate(key);
  }
  
  @Override
  public long getEstimateLowerBound(long key) { 
	return getEstimate(key) - getMaxError();
  }
  
  /**
   * @return a bound on the error of the estimate one gets from get(key).
   * Note that the error is one sided. if the real count is realCount(key) then
   * get(key) >= realCount(key). The guarantee of the sketch is that, for any fixed key, 
   * with probability at least 1-delta, realCount(key) is also at most get(key) + getMaxError() 
   */
  public long getMaxError() {
  	return (long) (Math.ceil(this.eps * this.update_sum)); 
  }
  
  /**
  
  /**
   * @param that
   * Another CountMin sketch. Must have been created using the same hash functions (i.e., same seed to MurmurHash)
   * and have the same parameter values eps, delta. 
   * @return pointer to the sketch resulting in adding the approximate counts of another sketch. 
   * This method does not create a new sketch. The sketch whose function is executed is changed.
   */
  @Override
  public FrequencyEstimator merge(FrequencyEstimator other) {
	if (!(other instanceof CountMinFastFE)) throw new IllegalArgumentException("SpaceSaving can only merge with other SpaceSaving");
	  CountMinFastFE otherCasted = (CountMinFastFE)other;
  	if(this.hashes != otherCasted.hashes || this.length != otherCasted.length){
  	  throw new IllegalArgumentException("Trying to merge two CountMin data structures of different sizes.");
  	}
    for(int i = 0; i < this.length; i++){
      this.counts[i] +=otherCasted.counts[i];
    }
    this.update_sum += otherCasted.update_sum;
    
    TLongHashSet newset = new TLongHashSet(this.freq_limit);
    TLongIterator it = this.freq_keys.iterator();
    long threshold = (long) (eps*this.update_sum);
    for ( int i = this.freq_keys.size(); i-- > 0; ) {
	  long key = it.next();
	  if(getEstimate(key) >= threshold){
	    newset.add(key);
      }
    }
    it = otherCasted.freq_keys.iterator();
    for ( int i = this.freq_keys.size(); i-- > 0; ) {
	  long key = it.next();
	  if(getEstimate(key) >= threshold){
	    newset.add(key);
      }
    }
    
    this.freq_keys = newset;
    
    return this;
  }
  
  @Override
  public long[] getFrequentKeys() {
	TLongIterator it = this.freq_keys.iterator();
	int count = 0;
	long threshold = (long) (eps*this.update_sum);
	for ( int i = this.freq_keys.size(); i-- > 0; ) {
	  long key = it.next();
      if(getEstimate(key) >= threshold){
		 count++;
      }
    }
    
    long[] keys = new long[count];
    int j=0;
    it = this.freq_keys.iterator();
	for ( int i = this.freq_keys.size(); i-- > 0; ) {
	  long key = it.next();
      if(getEstimate(key) >= threshold){
    	  keys[j] = key;
          j++;
      }
    }
    return keys;
  }
}