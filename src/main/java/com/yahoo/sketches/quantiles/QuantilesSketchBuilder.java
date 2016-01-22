/*
 * Copyright 2015, Yahoo! Inc.
 * Licensed under the terms of the Apache License 2.0. See LICENSE file at the project root for terms.
 */
package com.yahoo.sketches.quantiles;

import static com.yahoo.sketches.Util.LS;
import static com.yahoo.sketches.Util.TAB;
import static com.yahoo.sketches.quantiles.Util.*;

import com.yahoo.sketches.memory.Memory;

/**
 * For building a new HeapQuantilesSketch.
 * 
 * @author Lee Rhodes 
 */
public class QuantilesSketchBuilder {
  private int bK;
  private Memory bDstMem;
  
  public QuantilesSketchBuilder() {
    bK = 227; //default for ~1% rank accuracy
    bDstMem = null;
  }
  
  public QuantilesSketchBuilder setK(int k) {
    checkK(k);
    bK = k;
    return this;
  }
  
  public QuantilesSketchBuilder initMemory(Memory dstMem) {
    bDstMem = dstMem;
    return this;
  }
  
  public QuantilesSketch build() {
    QuantilesSketch sketch = null;
    if (bDstMem == null) {
      sketch = HeapQuantilesSketch.getInstance(bK);
    } else {
      //sketch = HeapQuantilesSketch.getInstance(bDstMem);
    }
    return sketch;
  }
  
  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("QuantileSketchBuilder configuration:").append(LS);
    sb.append("K:").append(TAB).append(bK).append(LS);
    sb.append("DstMemory:").append(TAB).append(bDstMem != null).append(LS);
    return sb.toString();
  }
}
