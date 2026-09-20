package com.khalid.filescanner.scanner;

import com.khalid.filescanner.model.FileRecord;
import com.khalid.filescanner.model.ScanStats;
import javafx.concurrent.Task;

import java.io.IOException;
import java.nio.file.DirectoryIteratorException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Week 3/4: a JavaFX Task that scans a folder tree using a pool of worker threads.
 *
 * Design:
 *  - the coordinator thread (the Task itself) starts the pool and waits until all folders are done
 *  - every folder is one job in the pool; sub-folders are submitted as new jobs
 *  - workers put results in a thread-safe queue; the UI drains the queue on a timer
 *    (so the JavaFX thread is never blocked and never flooded)
 *  - progress = folders finished / folders discovered so far
 */
public class FileScanTask extends Task<Void> implements Pausable {

    private final Path root;
    private final int threads;

    private final ScanStats stats = new ScanStats();
    private final Queue<FileRecord> queue = new ConcurrentLinkedQueue<>();

    private final AtomicInteger pendingDirs = new AtomicInteger();
    private final AtomicInteger dirsDiscovered = new AtomicInteger();
    private final AtomicInteger dirsDone = new AtomicInteger();
    private final CountDownLatch finished = new CountDownLatch(1);

    private final Object pauseLock = new Object();
    private volatile boolean paused;
    private volatile ExecutorService pool;

    public FileScanTask(Path root, int threads) {
        this.root = root;
        this.threads = Math.max(1, threads);
    }

    @Override
    protected Void call() {
        stats.start();
        pool = Executors.newFixedThreadPool(threads, r -> {
            Thread t = new Thread(r, "scan-worker");
            t.setDaemon(true);
            return t;
        });
        try {
            submitDir(root);
            finished.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // cancelled
        } finally {
            pool.shutdownNow();
            stats.stop();
        }
        if (!isCancelled()) {
            updateProgress(1, 1);
            updateMessage(String.format("Scan complete: %,d files in %,d folders", stats.getFiles(), stats.getFolders()));
        }
        return null;
    }

    private void submitDir(Path dir) {
        pendingDirs.incrementAndGet();
        dirsDiscovered.incrementAndGet();
        try {
            pool.submit(() -> {
                try {
                    scanDirectory(dir);
                } finally {
                    int done = dirsDone.incrementAndGet();
                    updateProgress(done, dirsDiscovered.get());
                    if (pendingDirs.decrementAndGet() == 0) {
                        finished.countDown();
                    }
                }
            });
        } catch (RejectedExecutionException e) { // pool already shut down (cancelled)
            if (pendingDirs.decrementAndGet() == 0) {
                finished.countDown();
            }
        }
    }

    private void scanDirectory(Path dir) {
        if (isCancelled()) {
            return;
        }
        stats.addFolder();
        try (DirectoryStream<Path> entries = Files.newDirectoryStream(dir)) {
            for (Path entry : entries) {
                if (isCancelled()) {
                    return;
                }
                waitIfPaused();
                handleEntry(entry);
            }
        } catch (IOException | SecurityException | DirectoryIteratorException e) {
            stats.addError(); // e.g. access denied
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void handleEntry(Path entry) {
        try {
            BasicFileAttributes attrs = Files.readAttributes(entry, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
            if (attrs.isSymbolicLink()) {
                return; // avoid link loops
            }
            if (attrs.isDirectory()) {
                submitDir(entry);
            } else if (attrs.isRegularFile()) {
                String name = entry.getFileName().toString();
                String ext = extensionOf(name);
                queue.add(new FileRecord(name, entry.toString(), ext, attrs.size(), attrs.lastModifiedTime().toMillis()));
                stats.addFile(ext, attrs.size());
                if (stats.getFiles() % 100 == 0) {
                    updateMessage(String.format("Scanning... %,d files found", stats.getFiles()));
                }
            }
        } catch (IOException | SecurityException e) {
            stats.addError();
        }
    }

    private static String extensionOf(String name) {
        int dot = name.lastIndexOf('.');
        if (dot <= 0 || dot == name.length() - 1) {
            return "";
        }
        return name.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    // ---- pause / resume (Week 4: wait / notifyAll) ----

    private void waitIfPaused() throws InterruptedException {
        synchronized (pauseLock) {
            while (paused && !isCancelled()) {
                pauseLock.wait();
            }
        }
    }

    @Override
    public void pause() {
        paused = true;
        updateMessage("Paused");
    }

    @Override
    public void resume() {
        synchronized (pauseLock) {
            paused = false;
            pauseLock.notifyAll();
        }
    }

    @Override
    public boolean isPaused() {
        return paused;
    }

    @Override
    protected void cancelled() {
        resume(); // wake up any paused workers so they can exit
    }

    // ---- results for the UI ----

    public ScanStats getStats() {
        return stats;
    }

    public List<FileRecord> drainBatch(int max) {
        List<FileRecord> out = new ArrayList<>();
        FileRecord r;
        while (out.size() < max && (r = queue.poll()) != null) {
            out.add(r);
        }
        return out;
    }

    public List<FileRecord> drainAll() {
        return drainBatch(Integer.MAX_VALUE);
    }
}
