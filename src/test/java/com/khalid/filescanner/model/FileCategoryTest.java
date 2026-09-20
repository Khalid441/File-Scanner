package com.khalid.filescanner.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileCategoryTest {

    @Test
    void allMatchesEverything() {
        assertTrue(FileCategory.ALL.matches("pdf"));
        assertTrue(FileCategory.ALL.matches(""));
        assertTrue(FileCategory.ALL.matches(null));
    }

    @Test
    void knownExtensionsMatchTheirCategory() {
        assertTrue(FileCategory.DOCUMENTS.matches("pdf"));
        assertTrue(FileCategory.IMAGES.matches("png"));
        assertTrue(FileCategory.CODE.matches("java"));
        assertTrue(FileCategory.ARCHIVES.matches("zip"));
    }

    @Test
    void matchingIgnoresCase() {
        assertTrue(FileCategory.IMAGES.matches("JPG"));
    }

    @Test
    void knownExtensionDoesNotMatchOtherCategories() {
        assertFalse(FileCategory.IMAGES.matches("pdf"));
        assertFalse(FileCategory.DOCUMENTS.matches("png"));
    }

    @Test
    void otherCollectsUnknownAndMissingExtensions() {
        assertTrue(FileCategory.OTHER.matches("xyz"));
        assertTrue(FileCategory.OTHER.matches(""));
        assertFalse(FileCategory.OTHER.matches("pdf"));
    }
}
