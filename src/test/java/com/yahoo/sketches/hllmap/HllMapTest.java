/*
 * Copyright 2016, Yahoo! Inc. Licensed under the terms of the
 * Apache License 2.0. See LICENSE file at the project root for terms.
 */

package com.yahoo.sketches.hllmap;

import static com.yahoo.sketches.hllmap.MapTestingUtil.TAB;
import static com.yahoo.sketches.hllmap.MapTestingUtil.bytesToInt;
import static com.yahoo.sketches.hllmap.MapTestingUtil.intToBytes;
import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;


public class HllMapTest {

  @Test
  public void singleKeyTest() {
    int k = 1024;
    int u = 1000;
    int initEntries = 16;
    int keySize = 4;
    float rf = (float)1.2;
    HllMap map = HllMap.getInstance(initEntries, keySize, k, rf);
    println("Entry bytes   : " + map.getEntrySizeBytes());
    println("Capacity      : " + map.getCapacityEntries());
    println("Table Entries : " + map.getTableEntries());
    println("Est Arr Size  : " + (map.getEntrySizeBytes() * map.getTableEntries()));
    println("Size of Arrays: "+ map.getSizeOfArrays());

    byte[] key = new byte[4];
    byte[] id = new byte[4];
    double est;
    key = intToBytes(1, key);
    for (int i=1; i<= u; i++) {
      id = intToBytes(i, id);
      int coupon = Util.coupon16(id, k);
      est = map.update(key, coupon);
      if (i % 100 == 0) {
        double err = (est/i -1.0) * 100;
        String eStr = String.format("%.3f%%", err);
        println("i: "+i + "\t Est: " + est + TAB + eStr);
      }
    }
    println("Table Entries : " + map.getTableEntries());
    println("Cur Count     : " + map.getCurrentCountEntries());
    println("RSE           : " + (1/Math.sqrt(k)));

    //map.printEntry(key);
  }

  @Test
  public void resizeTest() {
    int k = 1024;
    int u = 1000;
    int keys = 20;
    int initEntries = 16;
    int keySize = 4;
    float rf = (float)2.0;
    HllMap map = HllMap.getInstance(initEntries, keySize, k, rf);
    println("Entry bytes   : " + map.getEntrySizeBytes());
    println("Capacity      : " + map.getCapacityEntries());
    println("Table Entries : " + map.getTableEntries());
    println("Est Arr Size  : " + (map.getEntrySizeBytes() * map.getTableEntries()));
    println("Size of Arrays: "+ map.getSizeOfArrays());
    byte[] key = new byte[4];
    byte[] id = new byte[4];
    int i, j;
    for (j=1; j<=keys; j++) {
      key = intToBytes(j, key);
      for (i=1; i<= u; i++) {
        id = intToBytes(i, id);
        assertEquals(i, bytesToInt(id)); //TODO
        int coupon = Util.coupon16(id, k);
        map.update(key, coupon);
      }
      double est = map.getEstimate(key);
      double err = (est/u -1.0) * 100;
      String eStr = String.format("%.3f%%", err);
      println("key: " + j + "\tu: "+u + "\t Est: " + est + TAB + eStr);
    }

    println("Table Entries : " + map.getTableEntries());
    println("Cur Count     : " + map.getCurrentCountEntries());
    println("RSE           : " + (1/Math.sqrt(k)));
    for (j=1; j<=keys; j++) {
      key = intToBytes(j, key);
      double est = map.getEstimate(key);
      double err = (est/u -1.0) * 100;
      String eStr = String.format("%.3f%%", err);
      println("key: " + j + "\tu: "+u + "\t Est: " + est + TAB + eStr);
    }

  }

  public static void main(String[] args) {
    HllMapTest test = new HllMapTest();
    test.resizeTest();
    //test.singleKeyTest();
  }

  static void println(String s) { System.out.println(s); }
  static void print(String s) { System.out.print(s); }
}
