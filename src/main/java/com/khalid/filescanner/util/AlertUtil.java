package com.khalid.filescanner.util;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;

/** Tiny wrapper around JavaFX dialogs. Call only on the JavaFX thread. */
public final class AlertUtil {
    private AlertUtil() {
    }

    public static void error(String title, String message) {
        show(Alert.AlertType.ERROR, title, message);
    }

    public static void info(String title, String message) {
        show(Alert.AlertType.INFORMATION, title, message);
    }

    public static boolean confirm(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, message, ButtonType.OK, ButtonType.CANCEL);
        alert.setTitle(title);
        alert.setHeaderText(null);
        return alert.showAndWait().filter(b -> b == ButtonType.OK).isPresent();
    }

    private static void show(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type, message, ButtonType.OK);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.showAndWait();
    }
}
