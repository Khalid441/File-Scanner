package com.khalid.filescanner.model;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScanStatsTest {

    @Test
    void countsFilesBytesFoldersAndErrors() {
        ScanStats stats = new ScanStats();
        stats.addFile("txt", 100);
        stats.addFile("pdf", 200);
        stats.addFolder();
        stats.addError();

        assertEquals(2, stats.getFiles());
        assertEquals(300, stats.getBytes());
        assertEquals(1, stats.getFolders());
        assertEquals(1, stats.getErrors());
    }

    @Test
    void topExtensionsAreSortedAndFilesWithoutExtensionAreGrouped() {
        ScanStats stats = new ScanStats();
        for (int i = 0; i < 3; i++) {
            stats.addFile("txt", 1);
        }
        for (int i = 0; i < 2; i++) {
            stats.addFile("pdf", 1);
        }
        stats.addFile("", 1);

        List<Map.Entry<String, Integer>> top = stats.topExtensions(3);
        assertEquals("txt", top.get(0).getKey());
        assertEquals(3, top.get(0).getValue());
        assertEquals("pdf", top.get(1).getKey());
        assertEquals("(none)", top.get(2).getKey());
    }

    @Test
    void topExtensionsRespectsLimit() {
        ScanStats stats = new ScanStats();
        stats.addFile("a", 1);
        stats.addFile("b", 1);
        stats.addFile("c", 1);
        assertEquals(2, stats.topExtensions(2).size());
    }

    /** Week 4: many threads updating the same stats must never lose an update. */
    @Test
    void isThreadSafeUnderConcurrentUpdates() throws InterruptedException {
        int threads = 8;
        int perThread = 10_000;
        ScanStats stats = new ScanStats();
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch startSignal = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);

        for (int t = 0; t < threads; t++) {
            pool.execute(() -> {
                try {
                    startSignal.await();
                    for (int i = 0; i < perThread; i++) {
                        stats.addFile("txt", 1);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            });
        }
        startSignal.countDown(); // release all threads at the same moment
        assertTrue(done.await(30, TimeUnit.SECONDS));
        pool.shutdown();

        assertEquals(threads * perThread, stats.getFiles());
        assertEquals((long) threads * perThread, stats.getBytes());
        assertEquals(threads * perThread, stats.topExtensions(1).get(0).getValue());
    }

    @Test
    void loadFromRestoresSavedScan() {
        ScanSession session = new ScanSession(1, "C:\\test", "2026-01-01 10:00:00", 1234, 2, 5, 300, 2, "");
        List<FileRecord> records = List.of(
                new FileRecord("a.txt", "C:\\test\\a.txt", "txt", 100, 0),
                new FileRecord("b.pdf", "C:\\test\\b.pdf", "pdf", 200, 0));

        ScanStats stats = new ScanStats();
        stats.loadFrom(session, records);

        assertEquals(2, stats.getFiles());
        assertEquals(300, stats.getBytes());
        assertEquals(5, stats.getFolders());
        assertEquals(2, stats.getErrors());
        assertEquals(1234, stats.getElapsedMillis());
    }
}
