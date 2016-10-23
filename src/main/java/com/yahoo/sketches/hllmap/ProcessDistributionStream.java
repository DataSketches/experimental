/*
 * Copyright 2016, Yahoo! Inc. Licensed under the terms of the
 * Apache License 2.0. See LICENSE file at the project root for terms.
 */

package com.yahoo.sketches.hllmap;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import com.yahoo.sketches.theta.UpdateSketch;

/**
 * Processes an input stream of pairs of integers from Standard-In into the UniqueCountMap.
 * The input stream defines a distribution whereby each pair defines the number of keys with the
 * corresponding number of unique IDs. Each pair is of the form:
 *
 * <p><code>&lt;NumIDs&gt;&lt;TAB&gt;&lt;NumKeys&lt;&lt;line-separator&gt;.</code></p>
 *
 * <p>For each input pair, this model generates <i>NumIDs</i> unique identifiers for each of
 * <i>NumKeys</i> (also unique) and inputs them into the UniqueCountMap.</p>
 *
 * <p>The end of the stream is a null input line.</p>
 *
 * <p>At the end of the stream, UniqueCountMap.toString() is called and sent to Standard-Out.</p>
 *
 * <p>A typical command line might be as follows:</p>
 *
 * <p><code>cat NumIDsTABnumKeys.txt | java -cp hllmap.jar:sketches-core-0.8.2-SNAPSHOT-with-shaded-memory.jar com.yahoo.sketches.hllmap.DistributionModel</code></p>
 */
public class ProcessDistributionStream {
  private int lineCount = 0;
  private int ip = 0;
  private long val = 0;
  private byte[] ipBytes = new byte[4];
  private byte[] valBytes = new byte[8];

  private ProcessDistributionStream() {}

  /**
   * Main entry point.
   * @param args Not used.
   * @throws RuntimeException Generally an IOException.
   */
  public static void main(String[] args) throws RuntimeException {
    ProcessDistributionStream dm = new ProcessDistributionStream();
    dm.processDistributionModel();
  }

  private void processDistributionModel() {
    String line = "";
    UniqueCountMap map = new UniqueCountMap(4, 1024);
    UpdateSketch sketch = UpdateSketch.builder().setNominalEntries(65536).build();
    long updateTime_nS = 0;
    try (BufferedReader br = new BufferedReader(new InputStreamReader(System.in))){
      while ((line = br.readLine()) != null) {
        String[] tokens = line.split("\t");
        checkLen(tokens);
        lineCount++;
        long numIps = Long.parseLong(tokens[1]);
        long numValues = Long.parseLong(tokens[0]);

        for (long nips = 0; nips < numIps; nips++) {
          intToBytes(++ip, ipBytes);
          for (long vals = 0; vals < numValues; vals++) {
            long start_nS = System.nanoTime();
            map.update(ipBytes, longToBytes(++val, valBytes));
            long end_nS = System.nanoTime();
            updateTime_nS += end_nS - start_nS;
          }
        }
        sketch.update(ip);
      }
      long updateCount = val;
      int ipCount = ip;
      println(map.toString());
      println("\n");
      println("Lines Read   : " + lineCount);
      println("IP Count     : " + ipCount);
      println("Update Count : " + updateCount);
      println("nS Per update: " + ((double)updateTime_nS/updateCount));
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private static final byte[] intToBytes(int v, byte[] arr) {
    for (int i=0; i<4; i++) {
      arr[i] = (byte) (v & 0XFF);
      v >>>= 8;
    }
    return arr;
  }

  private static final byte[] longToBytes(long v, byte[] arr) {
    for (int i=0; i<8; i++) {
      arr[i] = (byte) (v & 0XFFL);
      v >>>= 8;
    }
    return arr;
  }

  private static final void checkLen(String[] tokens) {
    int len = tokens.length;
    if (len != 2) throw new IllegalArgumentException("Args.length must be 2: "+len);
  }

  private static void println(String s) { System.out.println(s); }

}
