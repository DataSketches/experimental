/*
 * Copyright 2015-16, Yahoo! Inc.
 * Licensed under the terms of the Apache License 2.0. See LICENSE file at the project root for terms.
 */

package com.yahoo.memory4;

/**
 * The exception thrown when attempting to write into a read-only Memory or construct a
 * writable class from a read-only instance of a class to be wrapped.
 *
 * @author Praveenkumar Venkatesan
 */
public class ReadOnlyMemoryException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public ReadOnlyMemoryException() { }

    public ReadOnlyMemoryException(final String message) {
      super(message);
    }
}

