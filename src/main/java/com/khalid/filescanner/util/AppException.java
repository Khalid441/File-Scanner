package com.khalid.filescanner.util;

/** Checked exception used for expected failures (database, JSON, network). Week 1: exception handling. */
public class AppException extends Exception {
    public AppException(String message) {
        super(message);
    }

    public AppException(String message, Throwable cause) {
        super(message, cause);
    }
}
