package com.khalid.filescanner.util;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Shared background pool for short database / file / network jobs (Week 4). */
public final class Async {
    private static final ExecutorService POOL = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "io-worker");
        t.setDaemon(true);
        return t;
    });

    private Async() {
    }

    public static void run(Runnable task) {
        POOL.execute(task);
    }
}
