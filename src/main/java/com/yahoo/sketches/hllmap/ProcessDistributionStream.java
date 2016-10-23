/*
 * Copyright 2016, Yahoo! Inc. Licensed under the terms of the
 * Apache License 2.0. See LICENSE file at the project root for terms.
 */

package com.yahoo.sketches.hllmap;

import static com.yahoo.sketches.Util.zeroPad;

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
  private static final String LS = System.getProperty("line.separator");
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
    long start_mS = System.currentTimeMillis();
    String line = "";
    long updateCount = 0;
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
      updateCount = val;
      int ipCount = ip;
      StringBuilder sb = new StringBuilder();
      String thisSimpleName = this.getClass().getSimpleName();
      sb.append("# ").append(thisSimpleName).append(" SUMMARY: ").append(LS);
      sb.append(map.toString());

      println("  Lines Read                : " + String.format("%,d", lineCount));
      println("  IP Count                  : " + String.format("%,d",ipCount));
      println("  Update Count              : " + String.format("%,d",updateCount));
      println("  nS Per update             : " + String.format("%,.3f", ((updateTime_nS * 1.0)/updateCount)));
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    long total_mS = System.currentTimeMillis() - start_mS;
    println("  Total Task Time           : " + milliSecToString(total_mS));
    println("  Task nS Per Update        : " + String.format("%,.3f", ((total_mS * 1E6)/updateCount)));
    println("# END PROCESS");
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

  /**
   * Returns the given time in milliseconds formatted as Hours:Min:Sec.mSec
   * @param mS the given nanoseconds
   * @return the given time in milliseconds formatted as Hours:Min:Sec.mSec
   */
  //temporarily copied from SNAPSHOT com.yahoo.sketches.TestingUtil (test branch)
  public static String milliSecToString(long mS) {
    long rem_mS = (long)(mS % 1000.0);
    long rem_sec = (long)((mS / 1000.0) % 60.0);
    long rem_min = (long)((mS / 60000.0) % 60.0);
    long hr  =     (long)(mS / 3600000.0);
    String mSstr = zeroPad(Long.toString(rem_mS), 3);
    String secStr = zeroPad(Long.toString(rem_sec), 2);
    String minStr = zeroPad(Long.toString(rem_min), 2);
    return String.format("%d:%2s:%2s.%3s", hr, minStr, secStr, mSstr);
  }


  private static void println(String s) { System.out.println(s); }

}
