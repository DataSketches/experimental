/*
 * Copyright 2016, Yahoo! Inc. Licensed under the terms of the 
 * Apache License 2.0. See LICENSE file at the project root for terms.
 */

package com.yahoo.sketches.quantiles;

/** 
 *  <p>Note: this pool is designed to increase performance in good cases
 *  but not decrease performance too much in bad cases. These increases
 *  and decreases are measured relative to not having the pool at all.</p>
 *
 *  <p>The bad cases are when multiple k values are simultaneously in use
 *  that share the same bucket. When this happens, the different Ks will
 *  repeatedly steal the bucket from each other, causing the pool to
 *  become ineffective. However, except for the fairly light overhead of
 *  the pools own logic, that will be no worse than if there was no pool
 *  at all. In other words, the burden of allocating, clearing, and
 *  re-claiming buffers will fall back onto the JVM and its garbage collector.</p>
 *
 *  <p>The choice of three buckets was somewhat arbitrary (although not
 *  completely so; it DOES have a rationale) but cannot be easily converted
 *  into a user-specified parameter because the "hashing" logic in
 *  bucket_number_from_k() is specifically designed for a three-bucket
 *  cache. This logic has the nice property of assigning different
 *  buckets to adjacent powers of two: For example:</p>
 * <pre>
 *  K  bkt
 * (32, 0);
 * (64, 1);
 * (128, 2);
 * (256, 0);
 * (512, 1);
 * (1024, 2);
 * etc.
 * </pre>
 * <p>So for example, the system could be simultanously processing sketches
 * with k = 64, k = 128, and k = 256 with no bucket contention. On the
 * other hand, if it was simultaneously dealing with powers of two that
 * differ by a factor of 8 (such as 128 and 1024) then there would
 * be some bucket contention. However, we believe that the impact on speed
 * would at worst be roughly 10 percent, which is by no means a disaster.</p>
 *
 * <p>Despite our special attention to powers of two, this pool works for 
 * ARBITRARY values of K, so it would continue to function correctly 
 * even if the current "powers of two only" policy for K were relaxed 
 * either in the library itself or in a fork of the library.</p>
 *
 * <p>Finally, the fact that a value of K can  "steal" a bucket from another 
 * value of K is an important part of the design. In fact, it is the main
 * cleanup mechanism for a long-running system that deals with many 
 * different values of K.</p>
 *
 * <p>No matter long it runs, and no matter many values of K that it sees,
 * there will never be more than 45 = 3 * 15 extra buffers sitting around
 * in memory (assuming that each bucket is allowed to retain 15 buffers).</p>
 *
 * <p> [TODO: we might want to modify sp_replace so that it simply discards 
 *  any buffer of length > 2048 (or some other value). That way there would 
 *  be an absolute limit on the memory consumption of the pool. On the
 *  other hand, forcing the Garbage Collector to deal with those buffers 
 *  would induce a different kind of memory cost that might be even worse.]</p>
 */

public class TriPool {
  private static final int three = 3;
  private static final int[] threeFromFour = {0, 1, 2, 1};
  
  private final int sp_size; 
  private int[] sp_k;  //who owns this bucket
  private int[] sp_pop; //# of valid buffers in this bucket of size k
  double[][][] sp_pool; //actual buckets
  
  public TriPool(int poolSize) {
    sp_size = poolSize;
    sp_k = new int[three];
    sp_pop = new int[three];
    sp_pool = new double[three][poolSize][];
  }
  
  public double[] getBuffer(int k) {
    int bkt = bucketNumberFromK(k);
    if ((k != sp_k[bkt]) || (sp_pop[bkt] == 0)) {
      return new double[k];
    } else {
      double[][] sub_pool = sp_pool[bkt];
      sp_pop[bkt]--;
      double[] result = sub_pool[sp_pop[bkt]];
      sub_pool[sp_pop[bkt]] = null;
      return result;
    }
  }
  
  public void freeBuffer(double[] buf) {
    int k = buf.length;
    int bkt = bucketNumberFromK(k);
    if (k != sp_k[bkt]) { //the k is different so steal the bkt
      sp_k[bkt] = k;
      sp_pop[bkt] = 0;  //optional to null out the bucket
    }
    if (sp_pop[bkt] < sp_size) {
      double[][] sub_pool = sp_pool[bkt];
      sub_pool[sp_pop[bkt]] = buf;
      sp_pop[bkt]++;
    }
    
  }
  
  private static final int bucketNumberFromK(int k) {
    return threeFromFour[(k % 7) & 3];
  }

}
