/*
 * Copyright 2015, Yahoo! Inc.
 * Licensed under the terms of the Apache License 2.0. See LICENSE file at the project root for terms.
 */
package com.yahoo.sketches.quantiles;

import static com.yahoo.sketches.quantiles.PreambleUtil.*;
import static org.testng.Assert.*;

import com.yahoo.sketches.memory.Memory;
import com.yahoo.sketches.memory.NativeMemory;
import com.yahoo.sketches.quantiles.QuantilesSketch;

import org.testng.annotations.Test;

public class PreambleUtilTest {

  @Test
  public void checkExtracts() {
    long along = 0XFFL;
    assertEquals(extractPreLongs(along), (int) along);
    
    along = 3L << 8;
    assertEquals(extractSerVer(along), 3);
    
    along = 7L << 16;
    assertEquals(extractFamilyID(along), 7);
    
    along = 0XFFL << 24;
    assertEquals(extractFlags(along), 0XFF);
    
    along = -1L << 32;
    assertEquals(extractK(along), -1);
    
    along = 0XFFFFFFFFL;
    assertEquals(extractBufAlloc(along), -1);
  }
  
  @Test
  public void checkInserts() {
    long v; int shift;
    v = 0XFFL; shift = 0;
    assertEquals(insertPreLongs((int)v, ~(v<<shift)), -1L);
    assertEquals(insertPreLongs((int)v, 0), v<<shift);
    
    v = 0XFFL; shift = 8; 
    assertEquals(insertSerVer((int)v, ~(v<<shift)), -1L);
    assertEquals(insertSerVer((int)v, 0), v<<shift);
    
    v = 0XFFL; shift = 16;
    assertEquals(insertFamilyID((int)v, ~(v<<shift)), -1L);
    assertEquals(insertFamilyID((int)v, 0), v<<shift);
    
    v = 0XFFL; shift = 24;
    assertEquals(insertFlags((int)v, ~(v<<shift)), -1L);
    assertEquals(insertFlags((int)v, 0), v<<shift);
    
    v = -1L; shift = 32;
    assertEquals(insertK((int)v, ~(v<<shift)), -1L);
    assertEquals(insertK((int)v, 0), v<<shift);
    
    v = 0XFFFFFFFFL; shift = 0;
    assertEquals(insertBufAlloc((int)v, ~(v<<shift)), -1L);
    assertEquals(insertBufAlloc((int)v, 0), v<<shift);
  }
  
  @Test
  public void checkToString() {
    int k = 227;
    int n = 1000000;
    QuantilesSketch qs = QuantilesSketch.builder().build(k);
    for (int i=0; i<n; i++) qs.update(i);
    byte[] byteArr = qs.toByteArray();
    println(PreambleUtil.toString(byteArr));
  }
  
  @Test
  public void checkToStringEmpty() {
    int k = 227;
    QuantilesSketch qs = QuantilesSketch.builder().build(k);
    byte[] byteArr = qs.toByteArray();
    println(PreambleUtil.toString(byteArr));
  }
  
  @Test
  public void printlnTest() {
    println("PRINTING: "+this.getClass().getName());
  }
  
  /**
   * @param s value to print 
   */
  static void println(String s) {
    //System.out.println(s); //disable here
  }
  
  
  
}
