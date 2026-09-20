package com.khalid.filescanner.util;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/** Small helpers to display sizes, dates and durations. */
public final class FormatUtil {
    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.systemDefault());

    private FormatUtil() {
    }

    public static String formatBytes(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        }
        String[] units = {"KB", "MB", "GB", "TB"};
        double value = bytes;
        int i = -1;
        do {
            value /= 1024;
            i++;
        } while (value >= 1024 && i < units.length - 1);
        return String.format("%.1f %s", value, units[i]);
    }

    public static String formatDate(long epochMillis) {
        return DATE_FMT.format(Instant.ofEpochMilli(epochMillis));
    }

    public static String formatDuration(long millis) {
        if (millis < 1000) {
            return millis + " ms";
        }
        double seconds = millis / 1000.0;
        if (seconds < 60) {
            return String.format("%.1f s", seconds);
        }
        long total = (long) seconds;
        return (total / 60) + "m " + (total % 60) + "s";
    }
}
