package com.khalid.filescanner.model;

/** One scanned file. Immutable, so it is safe to share between threads. */
public class FileRecord {
    private final String name;
    private final String path;
    private final String extension;
    private final long sizeBytes;
    private final long lastModified;

    public FileRecord(String name, String path, String extension, long sizeBytes, long lastModified) {
        this.name = name;
        this.path = path;
        this.extension = extension;
        this.sizeBytes = sizeBytes;
        this.lastModified = lastModified;
    }

    public String getName() {
        return name;
    }

    public String getPath() {
        return path;
    }

    public String getExtension() {
        return extension;
    }

    public long getSizeBytes() {
        return sizeBytes;
    }

    public long getLastModified() {
        return lastModified;
    }
}
