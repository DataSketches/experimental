package com.yahoo.sketches.frequencies;

import com.yahoo.sketches.hash.MurmurHash3;

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
public class CountMinFast{
  
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
  double eps;
  private int logLength;
  
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
  public CountMinFast(double eps, double delta) {
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
    for(int i = 0; i < this.length; i++)
    {
      counts[i] = 0;
    }
    this.update_sum = 0;
  }
  
  /**
   * @param key 
   * Process a key (specified as a long) update and treat the increment as 1
   */	
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
   * Process a key (specified as a long) and an increment (can be negative).
   */	
  public void update(long key, long increment) {
    this.update_sum += increment;
    
    long hash = hash(key);
    // make odd and independent of the probe:
    int stride = ((int) ((hash >> logLength) & STRIDE_MASK)<< 1) + 1;
    int probe = (int) (hash & arrayMask);
    
	for (int i=this.hashes; i-->0;) {
	  counts[probe] += increment;
	  probe = (probe + stride) & arrayMask;
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
  }
  
  
  /**
   * @param key whose count estimate is returned.
   * @return the approximate count for the key.
   * It is guaranteed that with probability at least 1-delta
   * 1) get(key) >= real count
   * 2) get(key) <= real count + getMaxError() 
   */
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
  
  
  public long getEstimateUpperBound(long key) { 
	return getEstimate(key);
  }
  
  
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
  public CountMinFast merge(CountMinFast other) {
  	if(this.hashes != other.hashes || this.length != other.length){
  	  throw new IllegalArgumentException("Trying to merge two CountMin data structures of different sizes.");
  	}
    for(int i = 0; i < this.length; i++){
      this.counts[i] +=other.counts[i];
    }
    this.update_sum += other.update_sum;
    return this;
  }
}