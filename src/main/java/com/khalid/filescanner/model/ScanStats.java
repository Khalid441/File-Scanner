package com.khalid.filescanner.model;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Live statistics updated by many worker threads at once.
 * Week 4: thread safety via atomics and ConcurrentHashMap (no locks needed).
 */
public class ScanStats {
    private final AtomicInteger files = new AtomicInteger();
    private final AtomicInteger folders = new AtomicInteger();
    private final AtomicInteger errors = new AtomicInteger();
    private final AtomicLong bytes = new AtomicLong();
    private final ConcurrentHashMap<String, AtomicInteger> byExtension = new ConcurrentHashMap<>();

    private volatile long startNanos;
    private volatile long endNanos;
    private volatile long durationOverrideMs = -1;

    public void start() {
        startNanos = System.nanoTime();
        endNanos = 0;
    }

    public void stop() {
        endNanos = System.nanoTime();
    }

    public void addFile(String extension, long size) {
        files.incrementAndGet();
        bytes.addAndGet(size);
        String key = (extension == null || extension.isEmpty()) ? "(none)" : extension;
        byExtension.computeIfAbsent(key, k -> new AtomicInteger()).incrementAndGet();
    }

    public void addFolder() {
        folders.incrementAndGet();
    }

    public void addError() {
        errors.incrementAndGet();
    }

    public int getFiles() {
        return files.get();
    }

    public int getFolders() {
        return folders.get();
    }

    public int getErrors() {
        return errors.get();
    }

    public long getBytes() {
        return bytes.get();
    }

    public long getElapsedMillis() {
        if (durationOverrideMs >= 0) {
            return durationOverrideMs;
        }
        if (startNanos == 0) {
            return 0;
        }
        long end = endNanos != 0 ? endNanos : System.nanoTime();
        return (end - startNanos) / 1_000_000;
    }

    public List<Map.Entry<String, Integer>> topExtensions(int limit) {
        return byExtension.entrySet().stream()
                .map(e -> Map.entry(e.getKey(), e.getValue().get()))
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(limit)
                .toList();
    }

    /** Rebuilds the stats from a saved / imported scan so the dashboard can show it. */
    public void loadFrom(ScanSession session, List<FileRecord> records) {
        for (FileRecord r : records) {
            addFile(r.getExtension(), r.getSizeBytes());
        }
        folders.set(session.getTotalFolders());
        errors.set(session.getErrors());
        durationOverrideMs = session.getDurationMs();
    }
}
