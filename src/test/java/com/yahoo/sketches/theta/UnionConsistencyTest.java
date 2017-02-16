/*
 * Copyright 2016, Yahoo! Inc. Licensed under the terms of the Apache License 2.0. See LICENSE file
 * at the project root for terms.
 */

package com.yahoo.sketches.theta;

import com.yahoo.sketches.utils.MultithreadedTestUtil.RepeatingTestThread;
import com.yahoo.sketches.utils.MultithreadedTestUtil.TestContext;

import com.google.common.collect.Lists;
import com.yahoo.sketches.utils.RandomSketchesAndDatumGenerator;

import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

/**
 */
@RunWith(Parameterized.class)
public class UnionConsistencyTest {

  @Parameterized.Parameters
  public static Object[] data() {
    return new Object[] { "NOT_THREAD_SAFE", "CONCURRENT_RW_LOCK" };
  }

  enum UnionType {
    NOT_THREAD_SAFE,
    CONCURRENT_RW_LOCK
  }

  private Union union;
  private final UnionType type;
  private static final int K = 16;
  private static final Log LOG = LogFactory.getLog(UnionConsistencyTest.class);

  public UnionConsistencyTest(String type) {
    this.type = UnionType.valueOf(type);
  }

  @Before
  public void setUp() throws Exception {
    Union u = Sketches.setOperationBuilder().buildUnion(K);
    switch (type) {
    case CONCURRENT_RW_LOCK:
      union = new ConcurrentUnion(u);
      break;
    case NOT_THREAD_SAFE:
    default:
      union = u;
      break;
    }
  }

  @Test
  public void testSingleWriterMultipleReaders() throws Exception {
    runTestAtomicity(union, 20000, 1, 5, K);
  }

  private void runTestAtomicity (
      Union union,
      long millisToRun,
      int numWriters,
      int numGetters,
      int k) throws Exception{

    TestContext ctx = new TestContext();

    List<AtomicityWriter> writers = Lists.newArrayList();
    for (int i = 0; i < numWriters; i++) {
      AtomicityWriter writer = new AtomicityWriter(ctx, union, k);
      writers.add(writer);
      ctx.addThread(writer);
    }

    List<AtomicGetReader> getters = Lists.newArrayList();
    for (int i = 0; i < numGetters; i++) {
      AtomicGetReader getter = new AtomicGetReader(ctx, union, k);
      getters.add(getter);
      ctx.addThread(getter);
    }

    ctx.startThreads();
    ctx.waitFor(millisToRun);
    ctx.stop();

    LOG.info("Finished test. Writers:");
    for (AtomicityWriter writer : writers) {
      LOG.info("  wrote " + writer.numWritten.get());
    }
    LOG.info("Readers:");
    for (AtomicGetReader reader : getters) {
      LOG.info("  read " + reader.numRead.get());
    }

  }

  public static class AtomicityWriter extends RepeatingTestThread {
    Random rand = new Random();
    Union union;
    int k;
    AtomicLong numWritten = new AtomicLong();

    public AtomicityWriter(TestContext ctx, Union union, int k) {
      super(ctx);
      this.union = union;
      this.k = k;
    }

    @Override
    public void doAnAction() throws Exception {
      int next = rand.nextInt(13);
      switch (next) {
      case 0:
        union.update(RandomSketchesAndDatumGenerator.generateRandomSketch(k));
        break;
      case 1:
        union.update(RandomSketchesAndDatumGenerator.generateRandomMemory(k));
        break;
      case 2:
        union.update(RandomSketchesAndDatumGenerator.generateRandomLongDatum(k));
        break;
      case 3:
        union.update(RandomSketchesAndDatumGenerator.generateRandomLongsDatum(k));
        break;
      case 4:
        union.update(RandomSketchesAndDatumGenerator.generateRandomByteDatum(k));
        break;
      case 5:
        union.update(RandomSketchesAndDatumGenerator.generateRandomBytesDatum(k));
        break;
      case 6:
        union.update(RandomSketchesAndDatumGenerator.generateRandomCharDatum(k));
        break;
      case 7:
        union.update(RandomSketchesAndDatumGenerator.generateRandomCharsDatum(k));
        break;
      case 8:
        union.update(RandomSketchesAndDatumGenerator.generateRandomIntDatum(k));
        break;
      case 9:
        union.update(RandomSketchesAndDatumGenerator.generateRandomIntsDatum(k));
        break;
      case 10:
        union.update(RandomSketchesAndDatumGenerator.generateRandomShortDatum(k));
        break;
      case 11:
        union.update(RandomSketchesAndDatumGenerator.generateRandomDoubleDatum(k));
        break;
      case 12:
        union.update(RandomSketchesAndDatumGenerator.generateRandomFloatDatum(k));
        break;
      default:
        //place holder
        break;
      }
      numWritten.getAndIncrement();
    }
  }

  /**
   * Thread that reads the results of the union, looking for partially
   * completed state.
   */
  public static class AtomicGetReader extends RepeatingTestThread {

    Union union;
    int k;
    AtomicLong numRead = new AtomicLong();
    int numVerified = 0;

    public AtomicGetReader(TestContext ctx, Union union, int k) {
      super(ctx);
      this.union = union;
      this.k = k;
    }

    @Override
    public void doAnAction() throws Exception {
      CompactSketch result = union.getResult();
      validateThetaGreaterThanHashValues(result);
      validateCountEqualsToHashSize(result);
      numRead.getAndIncrement();
    }

    private void validateThetaGreaterThanHashValues(CompactSketch result) {
      long theta = result.getThetaLong();
      for(long val : result.getCache()) {
        if(val > theta) {
          gotThetaFailure(theta, val, result);
        } else {
          numVerified++;
        }
      }
    }

    private void validateCountEqualsToHashSize(CompactSketch result) {
      int count = result.getRetainedEntries(true);
      int cacheSize = result.getCache().length;
      if(count != cacheSize) {
        gotCountFailure(count, cacheSize, result);
      }
    }

    private void gotThetaFailure(long theta, long val, CompactSketch res) {
      StringBuilder msg = new StringBuilder();
      msg.append("Theta validation failed after ").append(numVerified).append("!");
      msg.append("Theta=").append(theta);
      msg.append("Cache value=").append(val);
      throwCacheException(res, msg);
    }

    private void gotCountFailure(int count, int cacheSize, CompactSketch res) {
      StringBuilder msg = new StringBuilder();
      msg.append("Count validation failed!");
      msg.append("Count=").append(count);
      msg.append("Cache size=").append(cacheSize);
      throwCacheException(res, msg);
    }

    private void throwCacheException(CompactSketch res, StringBuilder msg) {
      msg.append("in cache:\n");
      msg.append("[");
      for (long l : res.getCache()) {
        msg.append(l);
        msg.append(", ");
      }
      msg.append("]\n");
      throw new RuntimeException(msg.toString());
    }

  }
}