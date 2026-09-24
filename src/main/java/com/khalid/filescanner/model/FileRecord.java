package com.khalid.filescanner.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/** One scanned file. Immutable, so it is safe to share between threads. */
public class FileRecord {
    private final String name;
    private final String path;
    private final String extension;
    private final long sizeBytes;
    private final long lastModified;

    @JsonCreator
    public FileRecord(@JsonProperty("name") String name,
                      @JsonProperty("path") String path,
                      @JsonProperty("extension") String extension,
                      @JsonProperty("sizeBytes") long sizeBytes,
                      @JsonProperty("lastModified") long lastModified) {
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