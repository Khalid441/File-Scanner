package com.khalid.filescanner.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

public final class HashUtil {
    private HashUtil() {
    }

    /** Streams the file and returns its SHA-256 as lowercase hex. */
    public static String sha256(Path file) throws IOException {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            try (InputStream in = Files.newInputStream(file);
                 DigestInputStream dis = new DigestInputStream(in, md)) {
                byte[] buffer = new byte[65536];
                while (dis.read(buffer) != -1) {
                    // reading updates the digest
                }
            }
            return HexFormat.of().formatHex(md.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
