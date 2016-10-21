/*
 * Copyright 2016, Yahoo! Inc. Licensed under the terms of the
 * Apache License 2.0. See LICENSE file at the project root for terms.
 */

package com.yahoo.sketches.hllmap;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.InetAddress;

import com.yahoo.sketches.theta.UpdateSketch;

public class CmdLine {

  /**
   * Example application, which reads tab-separated pairs of IPv4 and ID from the STDIN,
   * updates the map and prints a report at the end.
   * The report includes number of entries and memory usage on each level
   * as well as overall summary and nanoseconds per update.
   * @param args optional initial number of entries in the base table
   * @throws Exception exception thrown
   */
  public static void main(String[] args) throws Exception {
    int initNumEntries = 100_000_000;
    if (args.length > 0) {
      initNumEntries = Integer.parseInt(args[0]);
    }
    String itemStr = "";
    UniqueCountMap map = new UniqueCountMap(initNumEntries, 4, 1024);
    UpdateSketch sketch = UpdateSketch.builder().setNominalEntries(65536).build();
    long count = 0;
    long updateTimeNs = 0;
    try (BufferedReader br = new BufferedReader(new InputStreamReader(System.in))) {
      while ((itemStr = br.readLine()) != null) {
        String[] tokens = itemStr.split("\t");
        int len = tokens.length;
        if (len != 2) throw new IllegalArgumentException("Too many args: " + len);
        InetAddress iAddr = InetAddress.getByName(tokens[0]);
        final byte[] iAddBytes = iAddr.getAddress();
        final byte[] valBytes = tokens[1].getBytes();
        final long startNs = System.nanoTime();
        map.update(iAddBytes, valBytes);
        final long endNs = System.nanoTime();
        updateTimeNs += endNs - startNs;
        sketch.update(tokens[0]);
        count++;
      }
      println(map.toString());
      println("Lines Read: " + count);
      println("Theta Sketch Estimated Unique IPs: " + (int) sketch.getEstimate());
      println("nS Per update: " + ((double) updateTimeNs / count));
    }
  }

  static void println(String s) { System.out.println(s); }

}
