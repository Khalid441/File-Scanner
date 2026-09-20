package com.khalid.filescanner.model;

import java.util.List;

/** Shape of the exported / imported JSON file. */
public class ScanExport {
    private final String exportedAt;
    private final ScanSession session;
    private final List<FileRecord> files;

    public ScanExport(String exportedAt, ScanSession session, List<FileRecord> files) {
        this.exportedAt = exportedAt;
        this.session = session;
        this.files = files;
    }

    public String getExportedAt() {
        return exportedAt;
    }

    public ScanSession getSession() {
        return session;
    }

    public List<FileRecord> getFiles() {
        return files;
    }
}
