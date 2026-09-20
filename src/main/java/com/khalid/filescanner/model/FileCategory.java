package com.khalid.filescanner.model;

import java.util.Set;

/** Filter categories for the results table. */
public enum FileCategory {
    ALL("All files", Set.of()),
    IMAGES("Images", Set.of("jpg", "jpeg", "png", "gif", "bmp", "webp", "svg", "tiff", "ico")),
    DOCUMENTS("Documents", Set.of("pdf", "doc", "docx", "txt", "rtf", "odt", "ppt", "pptx", "xls", "xlsx", "csv", "md")),
    VIDEOS("Videos", Set.of("mp4", "mkv", "avi", "mov", "wmv", "flv", "webm")),
    AUDIO("Audio", Set.of("mp3", "wav", "flac", "aac", "ogg", "m4a")),
    ARCHIVES("Archives", Set.of("zip", "rar", "7z", "tar", "gz", "bz2")),
    CODE("Code", Set.of("java", "c", "cpp", "h", "py", "js", "ts", "html", "css", "xml", "json", "fxml", "sql", "sh")),
    OTHER("Other", Set.of());

    private final String label;
    private final Set<String> extensions;

    FileCategory(String label, Set<String> extensions) {
        this.label = label;
        this.extensions = extensions;
    }

    public boolean matches(String extension) {
        if (this == ALL) {
            return true;
        }
        String ext = extension == null ? "" : extension.toLowerCase();
        if (this == OTHER) {
            return !isKnown(ext);
        }
        return extensions.contains(ext);
    }

    private static boolean isKnown(String ext) {
        for (FileCategory c : values()) {
            if (c != ALL && c != OTHER && c.extensions.contains(ext)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String toString() {
        return label;
    }
}
