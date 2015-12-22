/*
 * Copyright 2015, Yahoo! Inc.
 * Licensed under the terms of the Apache License 2.0. See LICENSE file at the project root for terms.
 */
package com.yahoo.sketches.quantiles;

import java.util.Arrays;

/**
 * Auxiliary data structure for answering quantile queries
 */
class Auxiliary {
  int auxK_; // doesn't seem to be used anywhere, could maybe delete
  long auxN_;
  double[] auxSamplesArr_; //array of size samples
  long[] auxCumWtsArr_;

  /**
   * Constructs the Auxiliary structure from the QuantilesSketch
   * @param qs a QuantilesSketch
   */
  Auxiliary( QuantilesSketch qs ) {
    int k = qs.getK();
    long n = qs.getN();
    long bitPattern = qs.getBitPattern();
    double[] combinedBuffer = qs.getCombinedBuffer();
    int baseBufferCount = qs.getBaseBufferCount();
    int numSamples = qs.numValidSamples();
    
    double[] itemsArr = new double[numSamples];
    long[] cumWtsArr = new long[numSamples + 1]; /* the extra slot is very important */

    // Populate from QuantilesSketch:
    //  copy over the "levels" and then the base buffer, all with appropriate weights
    populateFromQuantilesSketch(k, n, bitPattern, combinedBuffer, baseBufferCount,
        numSamples, itemsArr, cumWtsArr);

    // Sort the first "numSamples" slots of the two arrays in tandem, 
    //  taking advantage of the already sorted blocks of length k

    Util.blockyTandemMergeSort(itemsArr, cumWtsArr, numSamples, k);

    // convert the item weights into totals of the weights preceding each item
    long subtot = 0;
    for (int i = 0; i < numSamples + 1; i++ ) {
      long newSubtot = subtot + cumWtsArr[i];
      cumWtsArr[i] = subtot;
      subtot = newSubtot;
    }

    assert subtot == n;
    
    auxK_ = k;
    auxN_ = n;
    auxSamplesArr_ = itemsArr;
    auxCumWtsArr_ = cumWtsArr;
  }
  
  /**
   * Populate the arrays and registers from a QuantilesSketch
   * @param k K value of sketch
   * @param n The current size of the stream
   * @param bitPattern 
   * @param combinedBuffer 
   * @param baseBufferCount the count of the base buffer
   * @param numSamples Total samples in the sketch
   * @param itemsArr 
   * @param cumWtsArr 
   */
  private final static void populateFromQuantilesSketch(
      int k, long n, long bitPattern, double[] combinedBuffer, int baseBufferCount,
      int numSamples, double[] itemsArr, long[] cumWtsArr) {
    long weight = 1;
    int nxt = 0;
    long bits = bitPattern;
    assert bits == n / (2L * k); // internal consistency check
    for (int lvl = 0; bits != 0L; lvl++, bits >>>= 1) {
      weight *= 2;
      if ((bits & 1L) > 0L) {
        int offset = (2+lvl) * k;
        for (int i = 0; i < k; i++) {
          itemsArr[nxt] = combinedBuffer[i+offset];
          cumWtsArr[nxt] = weight;
          nxt++;
        }
      }
    }

    weight = 1; // NOT a mistake! We just copied the highest level; now we need to copy the base buffer
    int startOfBaseBufferBlock = nxt;

    // Copy BaseBuffer over, along with weight = 1
    for (int i = 0; i < baseBufferCount; i++) {
      itemsArr[nxt] = combinedBuffer[i];
      cumWtsArr[nxt] = weight;
      nxt++;
    }
    assert nxt == numSamples;

    // Must sort the items that came from the base buffer.
    // Don't need to sort the corresponding weights because they are all the same.
    Arrays.sort (itemsArr, startOfBaseBufferBlock, numSamples);
    cumWtsArr[numSamples] = 0;
  }
  
  /**
   * This basically merges all of an MergeableQuantileSketch's weighted samples into a single 
   * auxiliary data structure that can be used to efficiently answer quantile queries.
   * ONLY used by MergeableQuantileSketch, could remove.
   * @param mq A MergeableQuantileSketch
   */
  Auxiliary( MergeableQuantileSketch mq ) {
    int k = mq.getK();
    long n = mq.getN();
    double[][] levelsArr = mq.getLevelsArr();
    double[] baseBuffer = mq.getBaseBuffer();
    int baseBufferCount = mq.getBaseBufferCount();
    
    int numSamples = countSamplesInSketch(k, n, levelsArr, baseBuffer, baseBufferCount);

    double[] itemsArr = new double[numSamples];
    long[] cumWtsArr = new long[numSamples + 1]; /* the extra slot is very important */

    /* copy over the "levels" and then the base buffer, all with appropriate weights */
    populateFromMQS(k, n, levelsArr, baseBuffer, baseBufferCount, itemsArr, cumWtsArr, numSamples);

    /* Sort the first "numSamples" slots of the two arrays in tandem, 
       taking advantage of the already sorted blocks of length k */

    Util.blockyTandemMergeSort(itemsArr, cumWtsArr, numSamples, k);

    // convert the item weights into totals of the weights preceding each item
    long subtot = 0;
    for (int i = 0; i < numSamples + 1; i++ ) {
      long newSubtot = subtot + cumWtsArr[i];
      cumWtsArr[i] = subtot;
      subtot = newSubtot;
    }

    assert subtot == n;
    
    auxK_ = k;
    auxN_ = n;
    auxSamplesArr_ = itemsArr;
    auxCumWtsArr_ = cumWtsArr;
  }
  
  /**
   * 
   * @param k K value of sketch
   * @param n The current size of the stream; Not used
   * @param levelsArr The levels arrays as double[][k]
   * @param baseBuffer The base buffer
   * @param baseBufferCount the count of items in base buffer
   * @param itemsArr empty array of numSamples 
   * @param cumWtsArr accumulator of weights
   * @param numSamples number of samples
   */
  private static void populateFromMQS(int k, long n, double[][] levelsArr,
      double[] baseBuffer, int baseBufferCount, double[] itemsArr, long[] cumWtsArr, int numSamples) {

    // The specific sorting / merging scheme that will applied to the auxiliary arrays requires 
    //   that the contents of the "levels" precede the contents of the "base buffer". 
    //   That way each successive block of length = k will already be sorted

    long weight = 1;

    // copy over all of the occupied "levels"
    int nxt = 0;
    for (int lvl = 0; lvl < levelsArr.length; lvl++ ) {
      weight *= 2;
      if (levelsArr[lvl] != null) {
        double[] buf = levelsArr[lvl];
        for (int i = 0; i < k; i++ ) {
          itemsArr[nxt] = buf[i];
          cumWtsArr[nxt] = weight;
          nxt++ ;
        }
      }
    }

    weight = 1; // NOT a mistake; we just copied the highest level; now we need to copy the base buffer

    int startOfBaseBufferBlock = nxt;

    /* Copy it over, along with appropriate weights */
    for (int i = 0; i < baseBufferCount; i++ ) {
      itemsArr[nxt] = baseBuffer[i];
      cumWtsArr[nxt] = weight;
      nxt++ ;
    }
    assert nxt == numSamples;

    // Must sort the items that came from the base buffer.
    // Don't need to sort the corresponding weights because they are all the same.

    Arrays.sort(itemsArr, startOfBaseBufferBlock, numSamples);

    cumWtsArr[numSamples] = 0;
  }

  /**
   * 
   * @param k K value of sketch
   * @param n The current size of the stream; Not used
   * @param levelsArr The levels arrays as double[][k]
   * @param baseBuffer The base buffer; Not used
   * @param baseBufferCount count of items in Base Buffer
   * @return the count of samples in the sketch
   */
  private static int countSamplesInSketch(int k, long n, double[][] levelsArr,
      double[] baseBuffer, int baseBufferCount) {
    int count = baseBufferCount;
    for (int lvl = 0; lvl < levelsArr.length; lvl++ ) {
      if (levelsArr[lvl] != null) {
        count += k;
      }
    }
    return count;
  }

  /* let m_i denote the minimum position of the length=n 
     "full" sorted sequence that is represented in 
     slot i of the length = n "chunked" sorted sequence.
  
     Note that m_i is the same thing as auxCumWtsArr_[i]
  
     Then the answer to a positional query 0 <= q < n
     is l, where 0 <= l < len, 
     A)  m_l <= q
     B)   q  < m_r
     C)   l+1 = r
  
     A) and B) provide the invariants for our binary search.
     Observe that they are satisfied by the initial conditions
     l = 0 and r = len.
  */
  private static int searchForChunkContainingPos(long[] arr, long q, int l, int r) {
    /* the following three asserts can probably go away eventually, since it is fairly clear
       that if these invariants hold at the beginning of the search, they will be maintained */
    assert l < r;
    assert arr[l] <= q;
    assert q < arr[r];
    if (l + 1 == r) {
      return l;
    }
    else {
      int m = l + (r - l) / 2;
      if (arr[m] <= q) {
        return (searchForChunkContainingPos(arr, q, m, r));
      }
      else {
        return (searchForChunkContainingPos(arr, q, l, m));
      }
    }
  }

  /* this is written in terms of a plain array to facilitate testing */
  private static int chunkContainingPos(long[] arr, long q) {
    int nominalLength = arr.length - 1; /* remember, arr contains an "extra" position */
    assert nominalLength > 0;
    long n = arr[nominalLength];
    assert 0 <= q;
    assert q < n;
    int l = 0;
    int r = nominalLength;
    /* the following three asserts should probably be retained since they ensure
       that the necessary invariants hold at the beginning of the search */
    assert l < r;
    assert arr[l] <= q;
    assert q < arr[r];
    return (searchForChunkContainingPos(arr, q, l, r));
  }

  /* Assuming that there are n items in the true stream, this asks what
     item would appear in position 0 <= pos < n of a hypothetical sorted
     version of that stream.  
  
     Note that since that since the true stream is unavailable,
     we don't actually answer the question for that stream, but rather for
     a _different_ stream of the same length, that could hypothetically
     be reconstructed from the weighted samples in our sketch */
  private double approximatelyAnswerPositionalQuery(long pos) {
    assert 0 <= pos;
    assert pos < this.auxN_;
    int index = chunkContainingPos(this.auxCumWtsArr_, pos);
    return (this.auxSamplesArr_[index]);
  }

  private static long posOfPhi(double phi, long n) { // don't tinker with this definition
    long pos = (long) Math.floor(phi * n);
    if (pos == n) {
      pos = n - 1; /* special rule */
    }
    return (pos);
  }
  
  //Used by QuantileSketch
  double getQuantile(double phi) {
    assert 0.0 <= phi;
    assert phi <= 1.0;
    long pos = posOfPhi(phi, this.auxN_);
    return (approximatelyAnswerPositionalQuery(pos));
  }

} /* end of class Auxiliary */
