package com.khalid.filescanner.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/** Summary of one scan (one row in the "scans" table). */
public class ScanSession {
    private long id;
    private final String rootPath;
    private final String startedAt;
    private final long durationMs;
    private final int totalFiles;
    private final int totalFolders;
    private final long totalBytes;
    private final int errors;
    private String note;

    @JsonCreator
    public ScanSession(@JsonProperty("id") long id,
                       @JsonProperty("rootPath") String rootPath,
                       @JsonProperty("startedAt") String startedAt,
                       @JsonProperty("durationMs") long durationMs,
                       @JsonProperty("totalFiles") int totalFiles,
                       @JsonProperty("totalFolders") int totalFolders,
                       @JsonProperty("totalBytes") long totalBytes,
                       @JsonProperty("errors") int errors,
                       @JsonProperty("note") String note) {
        this.id = id;
        this.rootPath = rootPath;
        this.startedAt = startedAt;
        this.durationMs = durationMs;
        this.totalFiles = totalFiles;
        this.totalFolders = totalFolders;
        this.totalBytes = totalBytes;
        this.errors = errors;
        this.note = note == null ? "" : note;
    }

    public static ScanSession from(String rootPath, String startedAt, ScanStats stats) {
        return new ScanSession(0, rootPath, startedAt, stats.getElapsedMillis(), stats.getFiles(),
                stats.getFolders(), stats.getBytes(), stats.getErrors(), "");
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getRootPath() {
        return rootPath;
    }

    public String getStartedAt() {
        return startedAt;
    }

    public long getDurationMs() {
        return durationMs;
    }

    public int getTotalFiles() {
        return totalFiles;
    }

    public int getTotalFolders() {
        return totalFolders;
    }

    public long getTotalBytes() {
        return totalBytes;
    }

    public int getErrors() {
        return errors;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note == null ? "" : note;
    }
}