package com.khalid.filescanner.scanner;

/** Week 1: a small interface implemented by the scan task. */
public interface Pausable {
    void pause();

    void resume();

    boolean isPaused();
}
