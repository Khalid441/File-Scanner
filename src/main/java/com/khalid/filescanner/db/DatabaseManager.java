package com.khalid.filescanner.db;

import com.khalid.filescanner.model.FileRecord;
import com.khalid.filescanner.model.ScanSession;
import com.khalid.filescanner.util.AppException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Week 6: SQLite with JDBC. Covers create table, insert, update, delete and query.
 * Always call from a background thread (see Async / Task), never from the JavaFX thread.
 */
public final class DatabaseManager {
    private static String url;

    private DatabaseManager() {
    }

    /** CREATE TABLE. Called once at startup. */
    public static void init() throws AppException {
        try {
            Path dir = Path.of(System.getProperty("user.home"), ".filescanner");
            Files.createDirectories(dir);
            url = "jdbc:sqlite:" + dir.resolve("history.db");
            try (Connection c = connect(); Statement st = c.createStatement()) {
                st.execute("""
                        CREATE TABLE IF NOT EXISTS scans (
                            id INTEGER PRIMARY KEY AUTOINCREMENT,
                            root_path TEXT NOT NULL,
                            started_at TEXT NOT NULL,
                            duration_ms INTEGER,
                            total_files INTEGER,
                            total_folders INTEGER,
                            total_bytes INTEGER,
                            errors INTEGER,
                            note TEXT DEFAULT ''
                        )""");
                st.execute("""
                        CREATE TABLE IF NOT EXISTS files (
                            id INTEGER PRIMARY KEY AUTOINCREMENT,
                            scan_id INTEGER NOT NULL REFERENCES scans(id) ON DELETE CASCADE,
                            name TEXT,
                            path TEXT,
                            extension TEXT,
                            size_bytes INTEGER,
                            last_modified INTEGER
                        )""");
                st.execute("CREATE INDEX IF NOT EXISTS idx_files_scan ON files(scan_id)");
            }
        } catch (IOException | SQLException e) {
            throw new AppException("Could not initialise the database: " + e.getMessage(), e);
        }
    }

    private static Connection connect() throws SQLException {
        Connection c = DriverManager.getConnection(url);
        try (Statement st = c.createStatement()) {
            st.execute("PRAGMA foreign_keys = ON");
        }
        return c;
    }

    /** INSERT a scan and all its files in one transaction. Returns the new scan id. */
    public static long insertScan(ScanSession s, List<FileRecord> files) throws AppException {
        String sqlScan = "INSERT INTO scans(root_path, started_at, duration_ms, total_files, total_folders, "
                + "total_bytes, errors, note) VALUES (?,?,?,?,?,?,?,?)";
        String sqlFile = "INSERT INTO files(scan_id, name, path, extension, size_bytes, last_modified) "
                + "VALUES (?,?,?,?,?,?)";
        try (Connection c = connect()) {
            c.setAutoCommit(false);
            try {
                long scanId;
                try (PreparedStatement ps = c.prepareStatement(sqlScan, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setString(1, s.getRootPath());
                    ps.setString(2, s.getStartedAt());
                    ps.setLong(3, s.getDurationMs());
                    ps.setInt(4, s.getTotalFiles());
                    ps.setInt(5, s.getTotalFolders());
                    ps.setLong(6, s.getTotalBytes());
                    ps.setInt(7, s.getErrors());
                    ps.setString(8, s.getNote());
                    ps.executeUpdate();
                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        keys.next();
                        scanId = keys.getLong(1);
                    }
                }
                try (PreparedStatement pf = c.prepareStatement(sqlFile)) {
                    int count = 0;
                    for (FileRecord r : files) {
                        pf.setLong(1, scanId);
                        pf.setString(2, r.getName());
                        pf.setString(3, r.getPath());
                        pf.setString(4, r.getExtension());
                        pf.setLong(5, r.getSizeBytes());
                        pf.setLong(6, r.getLastModified());
                        pf.addBatch();
                        if (++count % 1000 == 0) {
                            pf.executeBatch();
                        }
                    }
                    pf.executeBatch();
                }
                c.commit();
                return scanId;
            } catch (SQLException e) {
                c.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new AppException("Could not save the scan: " + e.getMessage(), e);
        }
    }

    /** SELECT all scans, newest first. */
    public static List<ScanSession> findAllScans() throws AppException {
        String sql = "SELECT id, root_path, started_at, duration_ms, total_files, total_folders, total_bytes, "
                + "errors, note FROM scans ORDER BY id DESC";
        List<ScanSession> result = new ArrayList<>();
        try (Connection c = connect(); Statement st = c.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                result.add(new ScanSession(rs.getLong("id"), rs.getString("root_path"), rs.getString("started_at"),
                        rs.getLong("duration_ms"), rs.getInt("total_files"), rs.getInt("total_folders"),
                        rs.getLong("total_bytes"), rs.getInt("errors"), rs.getString("note")));
            }
        } catch (SQLException e) {
            throw new AppException("Could not read scan history: " + e.getMessage(), e);
        }
        return result;
    }

    /** SELECT the files of one scan. */
    public static List<FileRecord> findFilesByScan(long scanId) throws AppException {
        String sql = "SELECT name, path, extension, size_bytes, last_modified FROM files WHERE scan_id = ?";
        List<FileRecord> result = new ArrayList<>();
        try (Connection c = connect(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, scanId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(new FileRecord(rs.getString("name"), rs.getString("path"), rs.getString("extension"),
                            rs.getLong("size_bytes"), rs.getLong("last_modified")));
                }
            }
        } catch (SQLException e) {
            throw new AppException("Could not load the scan: " + e.getMessage(), e);
        }
        return result;
    }

    /** UPDATE the note of a scan. */
    public static void updateNote(long scanId, String note) throws AppException {
        try (Connection c = connect();
             PreparedStatement ps = c.prepareStatement("UPDATE scans SET note = ? WHERE id = ?")) {
            ps.setString(1, note);
            ps.setLong(2, scanId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new AppException("Could not update the note: " + e.getMessage(), e);
        }
    }

    /** DELETE a scan (its files are removed by ON DELETE CASCADE). */
    public static void deleteScan(long scanId) throws AppException {
        try (Connection c = connect();
             PreparedStatement ps = c.prepareStatement("DELETE FROM scans WHERE id = ?")) {
            ps.setLong(1, scanId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new AppException("Could not delete the scan: " + e.getMessage(), e);
        }
    }
}
