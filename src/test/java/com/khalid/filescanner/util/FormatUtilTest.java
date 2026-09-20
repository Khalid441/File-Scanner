package com.khalid.filescanner.util;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FormatUtilTest {

    @BeforeAll
    static void useEnglishNumbers() {
        Locale.setDefault(Locale.US); // so "1.5" is not printed as "1,5"
    }

    @Test
    void smallSizesAreShownInBytes() {
        assertEquals("0 B", FormatUtil.formatBytes(0));
        assertEquals("1023 B", FormatUtil.formatBytes(1023));
    }

    @Test
    void largerSizesUseUnits() {
        assertEquals("1.0 KB", FormatUtil.formatBytes(1024));
        assertEquals("1.5 KB", FormatUtil.formatBytes(1536));
        assertEquals("1.0 MB", FormatUtil.formatBytes(1024L * 1024));
        assertEquals("5.0 GB", FormatUtil.formatBytes(5L * 1024 * 1024 * 1024));
    }

    @Test
    void durationsAreReadable() {
        assertEquals("500 ms", FormatUtil.formatDuration(500));
        assertEquals("1.5 s", FormatUtil.formatDuration(1500));
        assertEquals("2m 5s", FormatUtil.formatDuration(125_000));
    }
}
