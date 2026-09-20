package com.khalid.filescanner.util;

import javafx.scene.Scene;

import java.util.prefs.Preferences;

/** Switches between light and dark by adding or removing dark.css on the Scene. */
public final class ThemeManager {
    private static final String KEY = "darkMode";
    private static final Preferences PREFS = Preferences.userNodeForPackage(ThemeManager.class);
    private static final String DARK_CSS =
            ThemeManager.class.getResource("/com/khalid/filescanner/dark.css").toExternalForm();

    private ThemeManager() {
    }

    public static boolean isDark() {
        return PREFS.getBoolean(KEY, false);
    }

    public static void apply(Scene scene, boolean dark) {
        scene.getStylesheets().remove(DARK_CSS);
        if (dark) {
            scene.getStylesheets().add(DARK_CSS); // added last, so it overrides styles.css
        }
    }

    public static void remember(boolean dark) {
        PREFS.putBoolean(KEY, dark);
    }
}