package com.khalid.filescanner.controller;

import com.khalid.filescanner.db.DatabaseManager;
import com.khalid.filescanner.model.ScanSession;
import com.khalid.filescanner.util.AlertUtil;
import com.khalid.filescanner.util.Async;
import com.khalid.filescanner.util.FormatUtil;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextInputDialog;

import java.util.List;
import java.util.function.Consumer;

/** Week 6: shows the SQLite scan history with load / edit note / delete. */
public class HistoryController {

    @FXML private TableView<ScanSession> historyTable;
    @FXML private TableColumn<ScanSession, Long> idCol;
    @FXML private TableColumn<ScanSession, String> rootCol;
    @FXML private TableColumn<ScanSession, String> dateCol;
    @FXML private TableColumn<ScanSession, Integer> filesCol;
    @FXML private TableColumn<ScanSession, Long> sizeCol;
    @FXML private TableColumn<ScanSession, Long> durationCol;
    @FXML private TableColumn<ScanSession, String> noteCol;
    @FXML private Button loadBtn;
    @FXML private Button noteBtn;
    @FXML private Button deleteBtn;
    @FXML private Label infoLabel;

    private final ObservableList<ScanSession> sessions = FXCollections.observableArrayList();
    private Consumer<ScanSession> onLoadScan;

    @FXML
    private void initialize() {
        idCol.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().getId()));
        rootCol.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().getRootPath()));
        dateCol.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().getStartedAt()));
        filesCol.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().getTotalFiles()));
        sizeCol.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().getTotalBytes()));
        durationCol.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().getDurationMs()));
        noteCol.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().getNote()));

        sizeCol.setCellFactory(col -> new TableCell<ScanSession, Long>() {
            @Override
            protected void updateItem(Long value, boolean empty) {
                super.updateItem(value, empty);
                setText(empty || value == null ? null : FormatUtil.formatBytes(value));
            }
        });
        durationCol.setCellFactory(col -> new TableCell<ScanSession, Long>() {
            @Override
            protected void updateItem(Long value, boolean empty) {
                super.updateItem(value, empty);
                setText(empty || value == null ? null : FormatUtil.formatDuration(value));
            }
        });

        historyTable.setItems(sessions);
        var noSelection = historyTable.getSelectionModel().selectedItemProperty().isNull();
        loadBtn.disableProperty().bind(noSelection);
        noteBtn.disableProperty().bind(noSelection);
        deleteBtn.disableProperty().bind(noSelection);

        refresh();
    }

    /** MainController registers what should happen when the user loads a saved scan. */
    public void setOnLoadScan(Consumer<ScanSession> onLoadScan) {
        this.onLoadScan = onLoadScan;
    }

    @FXML
    public void refresh() {
        Task<List<ScanSession>> task = new Task<List<ScanSession>>() {
            @Override
            protected List<ScanSession> call() throws Exception {
                return DatabaseManager.findAllScans();
            }
        };
        task.setOnSucceeded(e -> {
            sessions.setAll(task.getValue());
            infoLabel.setText(sessions.size() + " saved scan(s)");
        });
        task.setOnFailed(e -> AlertUtil.error("Database error", task.getException().getMessage()));
        Async.run(task);
    }

    @FXML
    private void onLoad() {
        ScanSession selected = historyTable.getSelectionModel().getSelectedItem();
        if (selected != null && onLoadScan != null) {
            onLoadScan.accept(selected);
        }
    }

    @FXML
    private void onEditNote() {
        ScanSession selected = historyTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            return;
        }
        TextInputDialog dialog = new TextInputDialog(selected.getNote());
        dialog.setTitle("Edit note");
        dialog.setHeaderText(null);
        dialog.setContentText("Note:");
        dialog.showAndWait().ifPresent(text -> {
            Task<Void> task = new Task<Void>() {
                @Override
                protected Void call() throws Exception {
                    DatabaseManager.updateNote(selected.getId(), text);
                    return null;
                }
            };
            task.setOnSucceeded(e -> refresh());
            task.setOnFailed(e -> AlertUtil.error("Database error", task.getException().getMessage()));
            Async.run(task);
        });
    }

    @FXML
    private void onDelete() {
        ScanSession selected = historyTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            return;
        }
        if (!AlertUtil.confirm("Delete scan", "Delete scan #" + selected.getId() + " and all its file records?")) {
            return;
        }
        Task<Void> task = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                DatabaseManager.deleteScan(selected.getId());
                return null;
            }
        };
        task.setOnSucceeded(e -> refresh());
        task.setOnFailed(e -> AlertUtil.error("Database error", task.getException().getMessage()));
        Async.run(task);
    }
}
