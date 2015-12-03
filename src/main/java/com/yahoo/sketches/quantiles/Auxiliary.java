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
  int auK; // doesn't seem to be used anywhere, could maybe delete
  long auN;
  double[] auItems; // this could perhaps be called auSamples instead
  long[] auAccum;

  Auxiliary(int k, long n, double[] items, long[] accum) {
    auK = k;
    auN = n;
    auItems = items;
    auAccum = accum;
  }

  static void populateAuxiliaryArraysFromMQ6 (int mqK, long mqN, long mqBitPattern, 
                                             double [] mqCombinedBuffer, int mqBaseBufferCount,
                                             int numLevels, int numSamples,
                                             double [] items, long [] accum) {
    long weight = 1;
    int nxt = 0;
    long bits = mqBitPattern;
    for (int lvl = 0; lvl < numLevels; lvl++, bits >>>= 1) {
      weight *= 2;
      if ((bits & 1L) > 0L) {
        int offset = (2+lvl) * mqK;
        for (int i = 0; i < mqK; i++) {
          items[nxt] = mqCombinedBuffer[i+offset];
          accum[nxt] = weight;
          nxt++;
        }
      }
    }

    weight = 1; // NOT a mistake; we just copied the highest level; now we need to copy the base buffer

    int startOfBaseBufferBlock = nxt;

    /* Copy it over, along with appropriate weights */
    for (int i = 0; i < mqBaseBufferCount; i++) {
      items[nxt] = mqCombinedBuffer[i];
      accum[nxt] = weight;
      nxt++;
    }
    assert nxt == numSamples;

    // Must sort the items that came from the base buffer.
    // Don't need to sort the corresponding weights because they are all the same.

    Arrays.sort (items, startOfBaseBufferBlock, numSamples);

    accum[numSamples] = 0;

  }

  static Auxiliary constructAuxiliaryFromMQ6 (int mqK, long mqN, long mqBitPattern, 
                                              double [] mqCombinedBuffer, int mqBaseBufferCount,
                                              int numLevels, int numSamples) {
    double [] items = new double [numSamples];
    long   [] accum = new long   [numSamples+1]; /* the extra slot is very important */

    /* copy over the "levels" and then the base buffer, all with appropriate weights */
    populateAuxiliaryArraysFromMQ6 (mqK, mqN, mqBitPattern, mqCombinedBuffer, mqBaseBufferCount, numLevels, numSamples, items, accum);

    /* Sort the first "numSamples" slots of the two arrays in tandem, 
       taking advantage of the already sorted blocks of length k */

    Util.blockyTandemMergeSort (items, accum, numSamples, mqK);

    // convert the item weights into totals of the weights preceding each item
    long subtot = 0;
    for (int i = 0; i < numSamples+1; i++) {
      long newSubtot = subtot + accum[i];
      accum[i] = subtot;
      subtot = newSubtot;
    }

    assert subtot == mqN;

    Auxiliary au = new Auxiliary (mqK, mqN, items, accum);
    return au;
  }


  @SuppressWarnings("unused")
  private static int countSamplesInSketch(int mqK, long mqN, double[][] mqLevels,
      double[] mqBaseBuffer, int mqBaseBufferCount) {
    int count = mqBaseBufferCount;
    for (int lvl = 0; lvl < mqLevels.length; lvl++ ) {
      if (mqLevels[lvl] != null) {
        count += mqK;
      }
    }
    return count;
  }

  @SuppressWarnings("unused")
  private static void populateAuxiliaryArrays(int mqK, long mqN, double[][] mqLevels,
      double[] mqBaseBuffer, int mqBaseBufferCount, double[] items, long[] accum, int numSamples) {

    // The specific sorting / merging scheme that will applied to the auxiliary arrays requires 
    //   that the contents of the "levels" precede the contents of the "base buffer". 
    //   That way each successive block of length = k will already be sorted

    long weight = 1;

    // copy over all of the occupied "levels"
    int nxt = 0;
    for (int lvl = 0; lvl < mqLevels.length; lvl++ ) {
      weight *= 2;
      if (mqLevels[lvl] != null) {
        double[] buf = mqLevels[lvl];
        for (int i = 0; i < mqK; i++ ) {
          items[nxt] = buf[i];
          accum[nxt] = weight;
          nxt++ ;
        }
      }
    }

    weight = 1; // NOT a mistake; we just copied the highest level; now we need to copy the base buffer

    int startOfBaseBufferBlock = nxt;

    /* Copy it over, along with appropriate weights */
    for (int i = 0; i < mqBaseBufferCount; i++ ) {
      items[nxt] = mqBaseBuffer[i];
      accum[nxt] = weight;
      nxt++ ;
    }
    assert nxt == numSamples;

    // Must sort the items that came from the base buffer.
    // Don't need to sort the corresponding weights because they are all the same.

    Arrays.sort(items, startOfBaseBufferBlock, numSamples);

    accum[numSamples] = 0;

  }

  // This basically merges all of an MergeableQuantileSketch's weighted samples into a single auxiliary 
  // data structure that can be used to efficiently answer quantile queries.
  static Auxiliary constructAuxiliary(int mqK, long mqN, double[][] mqLevels,
      double[] mqBaseBuffer, int mqBaseBufferCount) {

    int numSamples = countSamplesInSketch(mqK, mqN, mqLevels, mqBaseBuffer, mqBaseBufferCount);

    double[] items = new double[numSamples];
    long[] accum = new long[numSamples + 1]; /* the extra slot is very important */

    /* copy over the "levels" and then the base buffer, all with appropriate weights */
    populateAuxiliaryArrays(
        mqK, mqN, mqLevels, mqBaseBuffer, mqBaseBufferCount, items, accum, numSamples);

    /* Sort the first "numSamples" slots of the two arrays in tandem, 
       taking advantage of the already sorted blocks of length k */

    Util.blockyTandemMergeSort(items, accum, numSamples, mqK);

    // convert the item weights into totals of the weights preceding each item
    long subtot = 0;
    for (int i = 0; i < numSamples + 1; i++ ) {
      long newSubtot = subtot + accum[i];
      accum[i] = subtot;
      subtot = newSubtot;
    }

    assert subtot == mqN;

    Auxiliary au = new Auxiliary(mqK, mqN, items, accum);
    return au;
  }

  /* let m_i denote the minimum position of the length=n 
     "full" sorted sequence that is represented in 
     slot i of the length = n "chunked" sorted sequence.
  
     Note that m_i is the same thing as auAccum[i]
  
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
    assert pos < this.auN;
    int index = chunkContainingPos(this.auAccum, pos);
    return (this.auItems[index]);
  }

  @SuppressWarnings("cast")
  static long posOfPhi(double phi, long n) { // don't tinker with this definition
    long pos = (long) Math.floor(phi * ((double) (n)));
    if (pos == n) {
      pos = n - 1; /* special rule */
    }
    return (pos);
  }

  double getQuantile(double phi) {
    assert 0.0 <= phi;
    assert phi <= 1.0;
    long pos = posOfPhi(phi, this.auN);
    return (approximatelyAnswerPositionalQuery(pos));
  }

} /* end of class Auxiliary */
