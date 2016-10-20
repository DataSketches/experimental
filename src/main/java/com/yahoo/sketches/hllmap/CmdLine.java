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

  CmdLine() {}


  /**
   *
   * @param args not used
   */
  CmdLine(String[] args) {

  }

  /**
   * Args not used.
   * @param args not used
   * @throws Exception exception thrown
   */
  public static void main(String[] args) throws Exception {
//    CmdLine cl = new CmdLine();
    processToUCMap();
//    checkIpAddr();
  }

  private static void processToUCMap() {
    String itemStr = "";
    UniqueCountMap map = new UniqueCountMap(4, 1024);
    UpdateSketch sketch = UpdateSketch.builder().setNominalEntries(65536).build();
    long count = 0;
    long updateTimenS = 0;
    try (BufferedReader br = new BufferedReader(new InputStreamReader(System.in))){
      while ((itemStr = br.readLine()) != null) {
        String[] tokens = itemStr.split("\t");
        int len = tokens.length;
        if (len != 2) throw new IllegalArgumentException("Too many args: "+len);
        InetAddress iAddr = InetAddress.getByName(tokens[0]);
        byte[] iAddBytes = iAddr.getAddress();
        byte[] valBytes = tokens[1].getBytes();
        long startnS = System.nanoTime();
        map.update(iAddBytes, valBytes);
        long endnS = System.nanoTime();
        updateTimenS += endnS - startnS;
        sketch.update(tokens[0]);
        count++;
      }
      println(map.toString());
      println("Lines Read: "+count);
      println("Theta Sketch Estimated Unique IPs: " + (int) sketch.getEstimate());
      println("nS Per update: " + ((double)updateTimenS/count));
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @SuppressWarnings("unused")
  private static void checkIpAddr() throws Exception {
    String ipStr = "10.126.1.1";
    byte[] ipBytes = InetAddress.getByName(ipStr).getAddress();
    for (int i = 0; i<4; i++) println(""+ipBytes[i]);
  }

  static void println(String s) { System.out.println(s); }

}
