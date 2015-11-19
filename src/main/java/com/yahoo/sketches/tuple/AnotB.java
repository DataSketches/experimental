/*
 * Copyright 2015, Yahoo! Inc.
 * Licensed under the terms of the Apache License 2.0. See LICENSE file at the project root for terms.
 */
package com.yahoo.sketches.tuple;

/**
 * 
 */
public interface AnotB<S extends Summary> {

  /**
   * Perform A-and-not-B set operation on the two given sketches.
   * A null sketch is interpreted as an empty sketch.
   * 
   * @param a The incoming sketch for the first argument
   * @param b The incoming sketch for the second argument
   */  
  void update(Sketch<S> a, Sketch<S> b);
  
  /**
   * Gets the result of this operation as a CompactSketch of the chosen form
   * @param dstOrdered 
   * <a href="{@docRoot}/resources/dictionary.html#dstOrdered">See Destination Ordered</a>
   * 
   * @param dstMem 
   * <a href="{@docRoot}/resources/dictionary.html#dstMem">See Destination Memory</a>.
   * 
   * @return the result of this operation as a CompactSketch of the chosen form
   */
  //CompactTupleSketch getResult(boolean dstOrdered, Memory dstMem);
  
  /**
   * Gets the result of this operation as an ordered CompactSketch on the Java heap
   * @return the result of this operation as an ordered CompactSketch on the Java heap
   */
  CompactSketch<S> getResult();
}