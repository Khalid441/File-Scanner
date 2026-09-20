package com.khalid.filescanner;

import com.khalid.filescanner.db.DatabaseManager;
import com.khalid.filescanner.util.AlertUtil;
import com.khalid.filescanner.util.AppException;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
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
        Scene scene = new Scene(loader.load(), 1200, 780);
        scene.getStylesheets().add(App.class.getResource("/com/khalid/filescanner/styles.css").toExternalForm());
        stage.setTitle("File Scanner Dashboard");
        stage.setScene(scene);
        stage.setMinWidth(950);
        stage.setMinHeight(600);
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
