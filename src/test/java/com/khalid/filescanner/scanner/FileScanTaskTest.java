package com.khalid.filescanner.scanner;

import com.khalid.filescanner.model.FileRecord;
import javafx.application.Platform;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Scans a temporary folder created for each test and checks the results.
 * The JavaFX toolkit is started once because Task publishes progress through Platform.runLater.
 */
class FileScanTaskTest {

    @BeforeAll
    static void startJavaFx() {
        try {
            Platform.startup(() -> { });
        } catch (IllegalStateException alreadyStarted) {
            // toolkit already running, fine
        }
    }

    private static void write(Path file, int bytes) throws IOException {
        Files.createDirectories(file.getParent());
        Files.write(file, new byte[bytes]);
    }

    @Test
    void scansNestedFolders(@TempDir Path root) throws IOException {
        write(root.resolve("a.txt"), 5);
        write(root.resolve("sub/b.pdf"), 10);
        write(root.resolve("sub/deep/c"), 3); // no extension

        FileScanTask task = new FileScanTask(root, 4);
        task.run(); // run synchronously on the test thread; workers still use the pool

        assertEquals(3, task.getStats().getFiles());
        assertEquals(3, task.getStats().getFolders()); // root, sub, deep
        assertEquals(18, task.getStats().getBytes());
        assertEquals(0, task.getStats().getErrors());

        List<FileRecord> records = task.drainAll();
        assertEquals(3, records.size());
        assertTrue(records.stream().anyMatch(r -> r.getName().equals("a.txt") && r.getExtension().equals("txt")));
        assertTrue(records.stream().anyMatch(r -> r.getName().equals("c") && r.getExtension().isEmpty()));
    }

    @Test
    void emptyFolderGivesZeroFiles(@TempDir Path root) {
        FileScanTask task = new FileScanTask(root, 2);
        task.run();

        assertEquals(0, task.getStats().getFiles());
        assertEquals(1, task.getStats().getFolders());
        assertTrue(task.drainAll().isEmpty());
    }

    @Test
    void resultsAreTheSameForAnyThreadCount(@TempDir Path root) throws IOException {
        for (int d = 0; d < 30; d++) {
            for (int f = 0; f < 20; f++) {
                write(root.resolve("dir" + d + "/file" + f + ".dat"), 1);
            }
        }
        int expectedFiles = 30 * 20;

        for (int threads : new int[]{1, 4, 8}) {
            FileScanTask task = new FileScanTask(root, threads);
            task.run();
            assertEquals(expectedFiles, task.getStats().getFiles(), "files with " + threads + " threads");
            assertEquals(31, task.getStats().getFolders(), "folders with " + threads + " threads"); // 30 + root
            assertEquals(expectedFiles, task.drainAll().size(), "records with " + threads + " threads");
        }
    }

    @Test
    void pauseAndResumeAreTracked(@TempDir Path root) {
        FileScanTask task = new FileScanTask(root, 1);
        assertTrue(!task.isPaused());
        task.pause();
        assertTrue(task.isPaused());
        task.resume();
        assertTrue(!task.isPaused());
    }
}
