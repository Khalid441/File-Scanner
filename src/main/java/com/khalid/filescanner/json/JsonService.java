package com.khalid.filescanner.json;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.khalid.filescanner.model.FileRecord;
import com.khalid.filescanner.model.ScanExport;
import com.khalid.filescanner.model.ScanSession;
import com.khalid.filescanner.util.AppException;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

/** Week 7: JSON export and import with Gson. */
public final class JsonService {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private JsonService() {
    }

    public static void export(Path file, ScanSession session, List<FileRecord> files) throws AppException {
        ScanExport data = new ScanExport(Instant.now().toString(), session, files);
        try (Writer writer = Files.newBufferedWriter(file)) {
            GSON.toJson(data, writer);
        } catch (IOException e) {
            throw new AppException("Could not write the JSON file: " + e.getMessage(), e);
        }
    }

    public static ScanExport importFrom(Path file) throws AppException {
        try (Reader reader = Files.newBufferedReader(file)) {
            ScanExport data = GSON.fromJson(reader, ScanExport.class);
            if (data == null || data.getSession() == null || data.getFiles() == null) {
                throw new AppException("This file is not a valid scan export.");
            }
            return data;
        } catch (IOException | JsonParseException e) {
            throw new AppException("Could not read the JSON file: " + e.getMessage(), e);
        }
    }
}
