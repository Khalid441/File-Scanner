package com.khalid.filescanner;

import com.khalid.filescanner.db.DatabaseManager;
import com.khalid.filescanner.util.AlertUtil;
import com.khalid.filescanner.util.AppException;
import com.khalid.filescanner.util.ThemeManager;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.stage.Screen;
import javafx.stage.Stage;

import java.io.IOException;

public class App extends Application {

    @Override
    public void start(Stage stage) throws IOException {
        try {
            DatabaseManager.init();
        } catch (AppException e) {
            AlertUtil.error("Database problem", e.getMessage());
        }

        FXMLLoader loader = new FXMLLoader(App.class.getResource("/com/khalid/filescanner/main.fxml"));

        // Fit the window to the visible screen area (excludes the taskbar)
        Rectangle2D screen = Screen.getPrimary().getVisualBounds();
        double width = Math.min(1200, screen.getWidth() * 0.9);
        double height = Math.min(780, screen.getHeight() * 0.9);

        Scene scene = new Scene(loader.load(), width, height);
        scene.getStylesheets().add(App.class.getResource("/com/khalid/filescanner/styles.css").toExternalForm());
        ThemeManager.apply(scene, ThemeManager.isDark()); // must come after styles.css

        stage.setTitle("File Scanner Dashboard");
        stage.setScene(scene);
        stage.setMinWidth(Math.min(650, width));
        stage.setMinHeight(Math.min(450, height));

        // Center it inside the visible area
        stage.setX(screen.getMinX() + (screen.getWidth() - width) / 2);
        stage.setY(screen.getMinY() + (screen.getHeight() - height) / 2);

        stage.setOnCloseRequest(e -> {
            Platform.exit();
            System.exit(0); // make sure background scan threads stop
        });
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}