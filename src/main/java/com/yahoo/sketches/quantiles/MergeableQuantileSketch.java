package com.yahoo.sketches.quantiles;

import java.util.Arrays;


/*
MergeableQuantiles
getQuantile
getQuantiles
getPDF
getCDF
*/



/**
 * This is an implementation of the low-discrepancy mergeable quantile sketches described
 * in section 3.2 of the journal version of the paper "Mergeable Summaries"
 * by Agarwal, Cormode, Huang, Phillips, Wei, and Yi
 */

public class MergeableQuantileSketch { /* mergeable quantiles */

  /**
   * Parameter that controls space usage of sketch and accuracy of estimates.
   */
  private int mqK;

  /**
   * Total number of data items in the stream so far. (Uniqueness plays no role in these sketches).
   */
  private long mqN;

  /**
   * Each level is either null or a buffer of length K that is completely full and sorted.
   * Note: in the README file these length K buffers are called "mini-sketches".
   */
  private double[][] mqLevels; 

  /**
   * The base buffer has length 2*K but might not be full and isn't necessarily sorted.
   */
  private double[] mqBaseBuffer; 

  /**
   * Number of samples currently in base buffer
   */
  private int mqBaseBufferCount; 

  /********************************/
  // package private; should only be used in testing

  int mqKGetter() { return mqK; }
  long mqNGetter() { return mqN; }
  //  double[][] mqLevelsGetter() { return mqLevels; }
  //  double[] mqBaseBufferGetter() { return mqBaseBuffer; }
  //  int mqBaseBufferCountGetter() {return mqBaseBufferCount; }

  /********************************/

  public MergeableQuantileSketch (int k) {
    assert k >= 1;
    mqK = k;
    mqN = 0;
    mqLevels = new double[0][];
    mqBaseBuffer = new double[Math.min(4,2*k)];     // the 4 is somewhat arbitrary; the min is important
    mqBaseBufferCount = 0;
  }

  /********************************/

  /**
   * Returns the length of the input stream so far. 
   */

  public long getStreamLength () {
    return (mqN);
  }

  /********************************/
  // package private

  //  void show () {
  //    System.out.printf ("showing: K=%d N=%d levels=%d baseBufferLength=%d baseBufferCount=%d\n",
  //                       mqK, mqN, mqLevels.length, mqBaseBuffer.length, mqBaseBufferCount);
  //    for (int i = 0; i < mqBaseBufferCount; i++) {
  //      System.out.printf (" %.3f", mqBaseBuffer[i]);
  //    }
  //    System.out.printf ("\n");
  //    System.out.printf ("%d levels\n", mqLevels.length);
  //    for (int j = 0; j < mqLevels.length; j++) {
  //      System.out.printf (" level %d\n", j);
  //      if (mqLevels[j] == null) {
  //        System.out.printf ("   empty\n");
  //      }
  //      else {
  //        for (int i = 0; i < mqK; i++) {
  //          System.out.printf ("    %.3f", mqLevels[j][i]);
  //        }
  //        System.out.printf ("\n");
  //      }
  //    }
  //  }

  /********************************/
  // package private
  Auxiliary constructAuxiliary () {
    Auxiliary au = Auxiliary.constructAuxiliary (this.mqK, this.mqN, this.mqLevels, this.mqBaseBuffer, this.mqBaseBufferCount);
    return au;
  }

  /********************************/

  /* there exist faster implementations of this */
  private static int hiBitPos (long num) {
    assert (num > 0);
    return ((int) ((Math.log (0.5 + ((double) num))) / (Math.log (2.0))));
  }

  /********************************/

  private static int computeNumLevelsNeeded (int k, long n) {
    long long2k = ((long) 2 * k);
    long quo = n / long2k;
    if (quo == 0) return 0;
    else return (1 + (hiBitPos (quo)));
  }

  /********************************/

  private void growBaseBuffer () {
    int oldSize = mqBaseBuffer.length;
    int newSize = Math.max (Math.min (2*mqK, 2*oldSize), 1);
    double[] newBuf = new double[newSize];
    for (int i = 0; i < oldSize; i++) {
      newBuf[i] = mqBaseBuffer[i];
    }
    mqBaseBuffer = newBuf;
  }

  /********************************/

  private void growLevels (int newSize) {
    int oldSize = mqLevels.length;
    assert (newSize > oldSize);
    double [][] newLevels = new double[newSize][];
    for (int i = 0; i < oldSize; i++) {
      newLevels[i] = mqLevels[i];
    }
    for (int i = oldSize; i < newSize; i++) {
      newLevels[i] = null;
    } 
    mqLevels = newLevels;
  }

  /********************************/

  /* it is the caller's responsibility to ensure that inbuf is already sorted */
  private static double [] allocatingCarryOfOneSize2KBuffer (double [] inbuf, int k) {
    int randomOffset = (int) (2.0 * Math.random());
    assert randomOffset == 0 || randomOffset == 1;
    double [] outbuf = new double [k];
    for (int i = 0; i < k; i++) {
      outbuf[i] = inbuf[2*i + randomOffset];
    }
    return (outbuf);
  }

  /********************************/

  // this one is definitely slightly slower
  // will remove after gaining more confidence in the faster one.
  //  private static double [] slowerAllocatingMergeTwoSizeKBuffers (double [] bufA, double [] bufB, int k) {
  //    assert bufA.length == k;
  //    assert bufB.length == k;
  //    int tmpLen = 2 * k;
  //    double [] tmpBuf = new double [tmpLen];
  //    int a = 0;
  //    int b = 0;
  //    for (int j = 0; j < tmpLen; j++) {
  //      if      (b == k)            {tmpBuf[j] = bufA[a++];}
  //      else if (a == k)            {tmpBuf[j] = bufB[b++];}
  //      else if (bufA[a] < bufB[b]) {tmpBuf[j] = bufA[a++];}
  //      else                        {tmpBuf[j] = bufB[b++];}
  //    }
  //    assert a == k;
  //    assert b == k;
  //    return (allocatingCarryOfOneSize2KBuffer(tmpBuf,k));
  //  }

  /********************************/

  // this one is definitely slightly faster
  private static double [] allocatingMergeTwoSizeKBuffers (double [] bufA, double [] bufB, int k) {
    assert bufA.length == k;
    assert bufB.length == k;
    int tmpLen = 2 * k;
    double [] tmpBuf = new double [tmpLen];

    int a = 0;
    int b = 0;
    int j = 0;

    while (a < k && b < k) {
      if (bufB[b] < bufA[a]) {
        tmpBuf[j++] = bufB[b++];
      }
      else {
        tmpBuf[j++] = bufA[a++];
      }
    }
    if (a < k) {
      System.arraycopy (bufA, a, tmpBuf, j, k - a); 
    }
    else {
      assert b < k;
      System.arraycopy (bufB, b, tmpBuf, j, k - b);
    }

    return (allocatingCarryOfOneSize2KBuffer(tmpBuf,k));
  }

  /********************************/

  /* It is the caller's responsibility to ensure that the levels
     array is big enough so that this provably cannot fail.  Also,
     while this tail-recursive procedure might cause logarithmic
     stack growth in java, that in itself shouldn't be a problem,
     nor would recoding it provide a significant speed-up, since it
     is not the inner loop of the algorithm */

  private void propagateCarry (double [] carryIn, int curLvl) {
    if (mqLevels[curLvl] == null) {/* propagation stops here */
      mqLevels[curLvl] = carryIn;
    }
    else {
      double [] oldBuf = mqLevels[curLvl];
      mqLevels[curLvl] = null;
      double [] carryOut = allocatingMergeTwoSizeKBuffers (carryIn, oldBuf, mqK);
      propagateCarry(carryOut, curLvl+1);
    }
  }

  /********************************/

  private void processFullBaseBuffer() {
    assert mqBaseBufferCount == 2 * mqK;

    /* make sure there will be enough levels for the propagation */
    int numLevelsNeeded = computeNumLevelsNeeded (mqK, mqN);
    if (numLevelsNeeded > mqLevels.length) {
      this.growLevels(numLevelsNeeded);
    }

    /* now we construct a new length-k "carry" buffer, and propagate it */
    Arrays.sort (mqBaseBuffer, 0, mqBaseBufferCount);
    double [] carry = allocatingCarryOfOneSize2KBuffer (mqBaseBuffer, mqK);
    mqBaseBufferCount = 0;
    propagateCarry (carry, 0);
  }

  /********************************/

  /** 
   * Tells the MergeableQuantileSketch that the input stream contains dataItem
   */

  public void update (double dataItem) {

    if (Double.isNaN(dataItem)) return;

    if (mqBaseBufferCount+1 > mqBaseBuffer.length) {
      this.growBaseBuffer();
    } 

    mqBaseBuffer[mqBaseBufferCount++] = dataItem;
    mqN++;

    if (mqBaseBufferCount == 2*mqK) {
      this.processFullBaseBuffer();
    }
  }

  /****************************************************************/
  /****************************************************************/
  /****************************************************************/

  // It is easy to prove that the following simplified code which launches 
  // multiple waves of carry propagation does exactly the same amount of merging work
  // (including the work of allocating fresh buffers) as the more complicated and 
  // seemingly more efficient approach that tracks a single carry propagation wave
  // through both sketches.

  // This simplified code probably does do slightly more "outer loop" work,
  // but I am pretty sure that even that is within a constant factor
  // of the more complicated code, plus the total amount of "outer loop"
  // work is at least a factor of K smaller than the total amount of 
  // merging work, which is identical in the two approaches.

/**
 * Modified the target sketch by merging the source sketch into it.
 */

  public static void mergeInto (MergeableQuantileSketch mqTarget, MergeableQuantileSketch mqSource) {  

    assert mqTarget.mqK == mqSource.mqK; // this should actually raise an exception

    int k = mqTarget.mqK;

    long nFinal = mqTarget.mqN + mqSource.mqN; 

    double [] sourceBaseBuffer = mqSource.mqBaseBuffer;
    for (int i = 0; i < mqSource.mqBaseBufferCount; i++) {
      mqTarget.update (sourceBaseBuffer[i]);
    }
    /* note: the above updates have already changed mqTarget in many ways, 
       but it might still need an additional level */

    int numLevelsNeeded = computeNumLevelsNeeded (k, nFinal);
    if (numLevelsNeeded > mqTarget.mqLevels.length) {
      mqTarget.growLevels(numLevelsNeeded);
    }

    for (int lvl = 0; lvl < mqSource.mqLevels.length; lvl++) {
      double [] buf = mqSource.mqLevels[lvl]; 
      if (buf != null) {
        mqTarget.propagateCarry (buf, lvl);
      }
    }

    mqTarget.mqN = nFinal;
  }

  /*****************************************************************/
  /*****************************************************************/
  /*****************************************************************/

  /*

    Note: a two-way merge that doesn't modify either of its
    two inputs could be implemented by making a deep copy of
    the larger sketch and then merging the smaller one into it 
  */

  /*****************************************************************/
  /*****************************************************************/
  /*****************************************************************/

  /**
   * Returns an approximation to the value at the specified fractional position in the hypothetical sorted stream.
   * <p>
   * getQuantile() returns an approximation to the value of the data item
   * that would be preceded by the given fraction of a hypothetical sorted
   * version of the input stream so far.
   * <p>
   * We note that this method has a fairly large overhead (microseconds instead of nanoseconds)
   * so it should not be called multiple times to get different quantiles from the same
   * sketch. Instead use getQuantiles() which only pays the overhead one time.
   */

  public double getQuantile (double fraction) {
    Auxiliary au = this.constructAuxiliary ();
    return (au.getQuantile (fraction));
  }

  /**
   * getQuantiles() is a more efficent multiple-query version of getQuantile ().
   * <p>
   * getQuantiles() returns an array that could have been generated by
   * mapping getQuantile() over the given array of fractions.
   * However, the computational overhead of getQuantile() is shared
   * amongst the multiple queries. Therefore we strongly recommend the
   * use of getQuantiles() instead of multiple calls to getQuantile()
   */

  public double [] getQuantiles (double [] fractions) {
    Auxiliary au = this.constructAuxiliary ();
    double [] answers = new double [fractions.length];
    for (int i = 0; i < fractions.length; i++) {
      answers[i] = au.getQuantile (fractions[i]);
    }
    return (answers);
  }

  /*****************************************************************/
  /*****************************************************************/
  /*****************************************************************/
  // Note: there is a comment in the increment histogram counters
  // code that says that the splitpoints must be unique. However,
  // the end to end test could generate duplicate splitpoints.
  // Need to resolve this apparent conflict.

  private static void validateSplitPoints (double [] splitPoints) {
    for (int j = 0; j < splitPoints.length - 1; j++) {
      if (splitPoints[j] > splitPoints[j+1]) { // was >=
        assert (1 == 0);    // should actually raise an exception
      }
    }
  }

  /********************************/

  private long [] internalBuildHistogram (double [] splitPoints) {
    MergeableQuantileSketch mq = this;
    validateSplitPoints (splitPoints);
    int numSplitPoints = splitPoints.length;
    int numCounters = numSplitPoints + 1;
    long [] counters = new long [numCounters];
    for (int j = 0; j < numCounters; j++) { counters[j] = 0; } // already true, right?
    long weight = 1;
    if (numSplitPoints < 50) { // empirically determined crossover for K = 200
      // not worth it to sort when few split points
      Util.quadraticTimeIncrementHistogramCounters (mq.mqBaseBuffer, mq.mqBaseBufferCount, weight,
                                                     splitPoints, counters);
    }
    else {
      Arrays.sort (mq.mqBaseBuffer, 0, mq.mqBaseBufferCount); // sort is worth it when many split points
      Util.linearTimeIncrementHistogramCounters (mq.mqBaseBuffer, mq.mqBaseBufferCount, weight,
                                                  splitPoints, counters);
    }
    for (int lvl = 0; lvl < mq.mqLevels.length; lvl++) { 
      weight += weight; // *= 2
      if (mq.mqLevels[lvl] != null) { 
        // the levels are already sorted so we can use the fast version
        Util.linearTimeIncrementHistogramCounters (mq.mqLevels[lvl], mq.mqK, weight, splitPoints, counters);
      }
    }
    return counters;
  }

  /********************************/

  // actually it's more of a PMF

  /**
   * Given a set of splitPoints, returns an approximation to the PDF of the input stream.
   * <p>
   * splitPoints is an array of (say) m monotonically increasing doubles
   * that divide the real number line into m+1 consecutive disjoint intervals.
   * getPdf() returns an array of m+1 doubles each of whose entries is an approximation
   * to the fraction of the input stream values that fell into one of those intervals.
   * <p>
   * Actually the name PMF might be more accurate, but is less well known.
   */

  public double [] getPDF (double [] splitPoints) {
    long [] counters = internalBuildHistogram (splitPoints);
    int numCounters = counters.length;
    double [] result = new double [numCounters];
    double denom = (double) this.mqN;
    long subtotal = 0;
    for (int j = 0; j < numCounters; j++) { 
      long count = counters[j];
      subtotal += count;
      result[j] = ((double) count) / denom;
    }
    assert (subtotal == this.mqN);
    return result;
  }

  /********************************/

  /**
   * getCDF() is the cumulative analog of getPDF()
   * <p>
   * More specifically, the value at array position j of the CDF is the
   * sum of the values in positions 0 through j of the PDF.
   */

  @SuppressWarnings("cast")
  public double [] getCDF (double [] splitPoints) {
    long [] counters = internalBuildHistogram (splitPoints);
    int numCounters = counters.length;
    double [] result = new double [numCounters];
    double denom = (double) this.mqN;
    long subtotal = 0;
    for (int j = 0; j < numCounters; j++) { 
      long count = counters[j];
      subtotal += count;
      result[j] = ((double) subtotal) / denom;
    }
    assert (subtotal == this.mqN);
    return result;
  }


  /* end of MergeableQuantileSketch implementation */
  /*****************************************************************/
  /*****************************************************************/
  /*****************************************************************/
  /* start of testing code */

  // package private
  int numSamplesInSketch () { // mostly for testing
    int count = this.mqBaseBufferCount;
    for (int lvl = 0; lvl < this.mqLevels.length; lvl++) {
      if (this.mqLevels[lvl] != null) { count += this.mqK; }
    }
    return count;
  }

  // package private
  double sumOfSamplesInSketch () { // only useful for testing
    double total = Util.sumOfDoublesInArrayPrefix (this.mqBaseBuffer, this.mqBaseBufferCount);
    for (int lvl = 0; lvl < this.mqLevels.length; lvl++) {
      if (this.mqLevels[lvl] != null) { 
        total += Util.sumOfDoublesInArrayPrefix (this.mqLevels[lvl], this.mqK);
      }
    }
    return total;
  }

  /*****************************************************************/

  // package private

  // this should probably be in the regression testing file

  static void validateMergeableQuantileSketchStructure (MergeableQuantileSketch mq, int k, long n) {
    long long2k = ((long) 2 * k);
    long quotient = n / long2k;
    int remainder = (int) (n % long2k);
    assert mq.mqBaseBufferCount == remainder;
    int numLevels = 0;
    if (quotient > 0) { numLevels = (1 + (hiBitPos (quotient))); }
    assert mq.mqLevels.length == numLevels;
    int level;
    long bitPattern;
    for (level = 0, bitPattern = quotient; level < numLevels; level++, bitPattern >>>= 1) {
      if      ((bitPattern & 1) == 0) { 
        assert mq.mqLevels[level] == null; 
      }
      else if ((bitPattern & 1) == 1) { 
        assert mq.mqLevels[level] != null; 
        double [] thisBuf = mq.mqLevels[level];
        for (int i = 0; i < thisBuf.length - 1; i++) {
          assert thisBuf[i] <= thisBuf[i+1];
        }
      }
      else { 
        assert false;
      }
    }
 }

  /************************************************************************************/

} // end of class MergeableQuantileSketch




/*************************************************************************************/

//  Discussion of structure sharing and modification of buffer contents.

// We are currently using a scheme whose "zip" operation always creates a 
// fresh level buffer, whose contents are never modified afterwards.

// This means that nothing bad can happen even though the current mergeInto code can cause
// the source and target sketches to both contain pointers to the exact same level buffer.

// There is a different scheme, which we are current NOT using because it is slightly more
// complicated and dangerous, that re-uses and therefore modifies level buffers as each
// carry wave propagates. If we ever switched to that scheme, different sketches could 
// not be allowed to share level buffers. 

// The fix (which actually isn't all that complicated) would be the following:
// (1) Keep track of which sketch owns each level buffer during a multi-sketch carry propagation.
// (2) Never modify a level buffer that belongs to a different sketch; 
//     instead modify the "other" buffer that is involved in the buffer merge.
// (3) Never store a pointer to a level buffer that belongs to a different sketch;
//     instead copy the level buffer.
