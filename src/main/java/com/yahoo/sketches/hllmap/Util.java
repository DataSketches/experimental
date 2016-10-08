/*
 * Copyright 2016, Yahoo! Inc. Licensed under the terms of the
 * Apache License 2.0. See LICENSE file at the project root for terms.
 */

package com.yahoo.sketches.hllmap;

import java.math.BigInteger;

public class Util {

  static final int nextPrime(int target) {
    return BigInteger.valueOf(target).nextProbablePrime().intValueExact();
  }

}
