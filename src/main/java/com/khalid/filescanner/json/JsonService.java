package com.khalid.filescanner.json;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.khalid.filescanner.model.FileRecord;
import com.khalid.filescanner.model.ScanExport;
import com.khalid.filescanner.model.ScanSession;
import com.khalid.filescanner.util.AppException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

/** Week 7: JSON export and import with Jackson. */
public final class JsonService {
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    private JsonService() {
    }

    public static void export(Path file, ScanSession session, List<FileRecord> files) throws AppException {
        ScanExport data = new ScanExport(Instant.now().toString(), session, files);
        try {
            MAPPER.writeValue(file.toFile(), data);
        } catch (IOException e) {
            throw new AppException("Could not write the JSON file: " + e.getMessage(), e);
        }
    }

    public static ScanExport importFrom(Path file) throws AppException {
        try {
            ScanExport data = MAPPER.readValue(file.toFile(), ScanExport.class);
            if (data.getSession() == null || data.getFiles() == null) {
                throw new AppException("This file is not a valid scan export.");
            }
            return data;
        } catch (IOException e) {
            throw new AppException("Could not read the JSON file: " + e.getMessage(), e);
        }
    }
}