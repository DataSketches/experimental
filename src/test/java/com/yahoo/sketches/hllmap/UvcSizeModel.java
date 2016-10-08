/*
 * Copyright 2016, Yahoo! Inc. Licensed under the terms of the
 * Apache License 2.0. See LICENSE file at the project root for terms.
 */

package com.yahoo.sketches.hllmap;


public class UvcSizeModel {
  private final UvcTableModel[] tables;
  private final int[] maxValues;

  public UvcSizeModel(int keyBytes) {
    tables = new UvcTableModel[6];
    maxValues = new int[6];

    tables[0] = new UvcTableModel(1 << 30, keyBytes, 4);
    maxValues[0] = 7;

    tables[1] = new UvcTableModel(1 << 20, keyBytes, 8);
    maxValues[1] = 68;

    tables[2] = new UvcTableModel(1 << 20, keyBytes, 64);
    maxValues[2] = 1434;

    tables[3] = new UvcTableModel(1 << 20, keyBytes, 512);
    maxValues[3] = 29569;

    tables[4] = new UvcTableModel(1 << 20, keyBytes, 2048);
    maxValues[4] = 147634;

    tables[5] = new UvcTableModel(1 << 20, keyBytes, 8192);
    maxValues[5] = Integer.MAX_VALUE;
  }

}
