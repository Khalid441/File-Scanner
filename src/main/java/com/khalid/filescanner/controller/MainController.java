package com.khalid.filescanner.controller;

import com.khalid.filescanner.api.VirusTotalClient;
import com.khalid.filescanner.db.DatabaseManager;
import com.khalid.filescanner.json.JsonService;
import com.khalid.filescanner.model.FileCategory;
import com.khalid.filescanner.model.FileRecord;
import com.khalid.filescanner.model.ScanExport;
import com.khalid.filescanner.model.ScanSession;
import com.khalid.filescanner.model.ScanStats;
import com.khalid.filescanner.scanner.FileScanTask;
import com.khalid.filescanner.util.AlertUtil;
import com.khalid.filescanner.util.Async;
import com.khalid.filescanner.util.FormatUtil;
import com.khalid.filescanner.util.HashUtil;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Spinner;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import javafx.util.Duration;

import java.io.File;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Main screen (main.fxml). The scan itself runs in FileScanTask on background threads;
 * this controller only starts it, polls its results on a Timeline and updates the UI.
 */
public class MainController {

    // ---- controls from main.fxml (names must match fx:id) ----
    @FXML private TabPane tabPane;
    @FXML private Button chooseBtn;
    @FXML private Button startBtn;
    @FXML private Button pauseBtn;
    @FXML private Button cancelBtn;
    @FXML private Button exportBtn;
    @FXML private Button importBtn;
    @FXML private Button vtBtn;
    @FXML private Label folderLabel;
    @FXML private Label statusLabel;
    @FXML private Label resultCountLabel;
    @FXML private Label filesLabel;
    @FXML private Label foldersLabel;
    @FXML private Label sizeLabel;
    @FXML private Label errorsLabel;
    @FXML private Label elapsedLabel;
    @FXML private Label vtResultLabel;
    @FXML private Spinner<Integer> threadSpinner;
    @FXML private TextField searchField;
    @FXML private ComboBox<FileCategory> categoryCombo;
    @FXML private CheckBox autoSaveCheck;
    @FXML private ProgressBar progressBar;
    @FXML private PasswordField apiKeyField;
    @FXML private TableView<FileRecord> tableView;
    @FXML private TableColumn<FileRecord, String> nameCol;
    @FXML private TableColumn<FileRecord, String> extCol;
    @FXML private TableColumn<FileRecord, Long> sizeCol;
    @FXML private TableColumn<FileRecord, Long> modifiedCol;
    @FXML private TableColumn<FileRecord, String> pathCol;
    @FXML private BarChart<String, Number> extChart;

    // Injected automatically because history.fxml is included with fx:id="history"
    @FXML private HistoryController historyController;

    // ---- state ----
    private final ObservableList<FileRecord> allRecords = FXCollections.observableArrayList();
    private FilteredList<FileRecord> filtered;
    private final XYChart.Series<String, Number> extSeries = new XYChart.Series<>();
    private final BooleanProperty vtBusy = new SimpleBooleanProperty(false);
    private Timeline uiTimer;
    private FileScanTask currentTask;
    private Path selectedFolder;
    private String scanStartedAt;
    private ScanSession lastSession;
    private int tick;

    @FXML
    private void initialize() {
        setupTable();
        setupFilters();

        extChart.getData().add(extSeries);
        vtBtn.disableProperty().bind(tableView.getSelectionModel().selectedItemProperty().isNull().or(vtBusy));

        // Poll the running task 4 times per second on the JavaFX thread
        uiTimer = new Timeline(new KeyFrame(Duration.millis(250), e -> refreshFromTask()));
        uiTimer.setCycleCount(Animation.INDEFINITE);

        historyController.setOnLoadScan(this::loadFromHistory);
        setScanningState(false);
        updateCount();
    }

    // ------------------------------------------------------------------ setup

    private void setupTable() {
        nameCol.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().getName()));
        extCol.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().getExtension()));
        pathCol.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().getPath()));
        sizeCol.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().getSizeBytes()));
        modifiedCol.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().getLastModified()));

        // Sorting stays numeric/date-based; only the displayed text is formatted
        sizeCol.setCellFactory(col -> new TableCell<FileRecord, Long>() {
            @Override
            protected void updateItem(Long value, boolean empty) {
                super.updateItem(value, empty);
                setText(empty || value == null ? null : FormatUtil.formatBytes(value));
            }
        });
        modifiedCol.setCellFactory(col -> new TableCell<FileRecord, Long>() {
            @Override
            protected void updateItem(Long value, boolean empty) {
                super.updateItem(value, empty);
                setText(empty || value == null ? null : FormatUtil.formatDate(value));
            }
        });

        filtered = new FilteredList<>(allRecords, r -> true);
        SortedList<FileRecord> sorted = new SortedList<>(filtered);
        sorted.comparatorProperty().bind(tableView.comparatorProperty());
        tableView.setItems(sorted);
        filtered.addListener((ListChangeListener<FileRecord>) change -> updateCount());
    }

    private void setupFilters() {
        categoryCombo.getItems().setAll(FileCategory.values());
        categoryCombo.setValue(FileCategory.ALL);
        searchField.textProperty().addListener((obs, oldV, newV) -> applyFilter());
        categoryCombo.valueProperty().addListener((obs, oldV, newV) -> applyFilter());
    }

    private void applyFilter() {
        String query = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase();
        FileCategory category = categoryCombo.getValue() == null ? FileCategory.ALL : categoryCombo.getValue();
        filtered.setPredicate(r -> category.matches(r.getExtension())
                && (query.isEmpty() || r.getName().toLowerCase().contains(query)));
        updateCount();
    }

    private void updateCount() {
        resultCountLabel.setText(String.format("Showing %,d of %,d files", filtered.size(), allRecords.size()));
    }

    // ------------------------------------------------------------------ scanning

    @FXML
    private void onChooseFolder() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Choose a folder to scan");
        File dir = chooser.showDialog(window());
        if (dir != null) {
            selectedFolder = dir.toPath();
            folderLabel.setText(dir.getAbsolutePath());
            setScanningState(false);
        }
    }

    @FXML
    private void onStart() {
        if (selectedFolder == null) {
            return;
        }
        allRecords.clear();
        tick = 0;
        lastSession = null;

        FileScanTask task = new FileScanTask(selectedFolder, threadSpinner.getValue());
        currentTask = task;
        scanStartedAt = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        // The Task publishes progress/message safely; we just bind to them
        progressBar.progressProperty().bind(task.progressProperty());
        statusLabel.textProperty().bind(task.messageProperty());

        task.setOnSucceeded(e -> onScanFinished(task, false));
        task.setOnCancelled(e -> onScanFinished(task, true));
        task.setOnFailed(e -> {
            onScanFinished(task, true);
            statusLabel.setText("Scan failed");
            AlertUtil.error("Scan failed", String.valueOf(task.getException()));
        });

        pauseBtn.setText("Pause");
        setScanningState(true);
        uiTimer.play();

        Thread coordinator = new Thread(task, "scan-coordinator");
        coordinator.setDaemon(true);
        coordinator.start();
    }

    @FXML
    private void onPause() {
        if (currentTask == null) {
            return;
        }
        if (currentTask.isPaused()) {
            currentTask.resume();
            pauseBtn.setText("Pause");
        } else {
            currentTask.pause();
            pauseBtn.setText("Resume");
        }
    }

    @FXML
    private void onCancel() {
        if (currentTask != null) {
            currentTask.cancel();
        }
    }

    private void refreshFromTask() {
        if (currentTask == null) {
            return;
        }
        List<FileRecord> batch = currentTask.drainBatch(5000);
        if (!batch.isEmpty()) {
            allRecords.addAll(batch);
        }
        updateStatsPanel(currentTask.getStats(), ++tick % 4 == 0);
    }

    private void onScanFinished(FileScanTask task, boolean cancelled) {
        uiTimer.stop();
        List<FileRecord> rest = task.drainAll();
        if (!rest.isEmpty()) {
            allRecords.addAll(rest);
        }
        ScanStats stats = task.getStats();
        updateStatsPanel(stats, true);

        progressBar.progressProperty().unbind();
        statusLabel.textProperty().unbind();
        progressBar.setProgress(cancelled ? 0 : 1);
        pauseBtn.setText("Pause");
        setScanningState(false);

        String duration = FormatUtil.formatDuration(stats.getElapsedMillis());
        statusLabel.setText(cancelled
                ? "Scan stopped after " + duration
                : String.format("Scan complete in %s - %,d files", duration, allRecords.size()));

        lastSession = ScanSession.from(selectedFolder.toString(), scanStartedAt, stats);
        if (!cancelled && autoSaveCheck.isSelected()) {
            saveToHistory(lastSession, new ArrayList<>(allRecords));
        }
    }

    private void setScanningState(boolean scanning) {
        chooseBtn.setDisable(scanning);
        startBtn.setDisable(scanning || selectedFolder == null);
        pauseBtn.setDisable(!scanning);
        cancelBtn.setDisable(!scanning);
        threadSpinner.setDisable(scanning);
        exportBtn.setDisable(scanning);
        importBtn.setDisable(scanning);
    }

    private boolean isScanning() {
        return currentTask != null && currentTask.isRunning();
    }

    // ------------------------------------------------------------------ statistics

    private void updateStatsPanel(ScanStats stats, boolean updateChart) {
        filesLabel.setText(String.format("%,d", stats.getFiles()));
        foldersLabel.setText(String.format("%,d", stats.getFolders()));
        sizeLabel.setText(FormatUtil.formatBytes(stats.getBytes()));
        errorsLabel.setText(String.format("%,d", stats.getErrors()));
        elapsedLabel.setText(FormatUtil.formatDuration(stats.getElapsedMillis()));
        if (updateChart) {
            List<XYChart.Data<String, Number>> data = new ArrayList<>();
            for (Map.Entry<String, Integer> e : stats.topExtensions(8)) {
                data.add(new XYChart.Data<String, Number>(e.getKey(), e.getValue()));
            }
            extSeries.getData().setAll(data);
        }
    }

    // ------------------------------------------------------------------ SQLite history (Week 6)

    private void saveToHistory(ScanSession session, List<FileRecord> records) {
        Task<Long> task = new Task<Long>() {
            @Override
            protected Long call() throws Exception {
                return DatabaseManager.insertScan(session, records);
            }
        };
        task.setOnSucceeded(e -> {
            session.setId(task.getValue());
            statusLabel.setText(statusLabel.getText() + " (saved to history)");
            historyController.refresh();
        });
        task.setOnFailed(e -> AlertUtil.error("Could not save scan", task.getException().getMessage()));
        Async.run(task);
    }

    private void loadFromHistory(ScanSession session) {
        if (isScanning()) {
            AlertUtil.info("Scan running", "Stop the current scan before loading another one.");
            return;
        }
        Task<List<FileRecord>> task = new Task<List<FileRecord>>() {
            @Override
            protected List<FileRecord> call() throws Exception {
                return DatabaseManager.findFilesByScan(session.getId());
            }
        };
        task.setOnSucceeded(e -> {
            showLoadedScan(session, task.getValue());
            tabPane.getSelectionModel().select(0);
        });
        task.setOnFailed(e -> AlertUtil.error("Could not load scan", task.getException().getMessage()));
        Async.run(task);
    }

    private void showLoadedScan(ScanSession session, List<FileRecord> records) {
        allRecords.setAll(records);
        lastSession = session;
        ScanStats stats = new ScanStats();
        stats.loadFrom(session, records);
        updateStatsPanel(stats, true);
        folderLabel.setText(session.getRootPath());
        progressBar.setProgress(1);
        statusLabel.setText(String.format("Loaded scan from %s (%,d files)", session.getStartedAt(), records.size()));
    }

    // ------------------------------------------------------------------ JSON (Week 7)

    @FXML
    private void onExport() {
        if (allRecords.isEmpty() || lastSession == null) {
            AlertUtil.info("Nothing to export", "Run a scan or load one first.");
            return;
        }
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Export scan results");
        chooser.setInitialFileName("scan-results.json");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON files", "*.json"));
        File file = chooser.showSaveDialog(window());
        if (file == null) {
            return;
        }
        ScanSession session = lastSession;
        List<FileRecord> copy = new ArrayList<>(allRecords);
        Task<Void> task = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                JsonService.export(file.toPath(), session, copy);
                return null;
            }
        };
        task.setOnSucceeded(e -> statusLabel.setText("Exported " + copy.size() + " records to " + file.getName()));
        task.setOnFailed(e -> AlertUtil.error("Export failed", task.getException().getMessage()));
        Async.run(task);
    }

    @FXML
    private void onImport() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Import scan results");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON files", "*.json"));
        File file = chooser.showOpenDialog(window());
        if (file == null) {
            return;
        }
        Task<ScanExport> task = new Task<ScanExport>() {
            @Override
            protected ScanExport call() throws Exception {
                return JsonService.importFrom(file.toPath());
            }
        };
        task.setOnSucceeded(e -> {
            ScanExport data = task.getValue();
            showLoadedScan(data.getSession(), data.getFiles());
            statusLabel.setText("Imported " + data.getFiles().size() + " records from " + file.getName());
        });
        task.setOnFailed(e -> AlertUtil.error("Import failed", task.getException().getMessage()));
        Async.run(task);
    }

    // ------------------------------------------------------------------ REST API (Week 7)

    @FXML
    private void onCheckVirusTotal() {
        FileRecord selected = tableView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            return;
        }
        String key = apiKeyField.getText();
        if (key == null || key.isBlank()) {
            key = System.getenv("VT_API_KEY");
        }
        final String apiKey = key;

        vtBusy.set(true);
        vtResultLabel.setText("Hashing and checking " + selected.getName() + "...");
        Task<String> task = new Task<String>() {
            @Override
            protected String call() throws Exception {
                String hash = HashUtil.sha256(Path.of(selected.getPath()));
                return VirusTotalClient.lookupHash(hash, apiKey) + "   [SHA-256 " + hash.substring(0, 12) + "...]";
            }
        };
        task.setOnSucceeded(e -> {
            vtResultLabel.setText(selected.getName() + ": " + task.getValue());
            vtBusy.set(false);
        });
        task.setOnFailed(e -> {
            vtResultLabel.setText("Error: " + task.getException().getMessage());
            vtBusy.set(false);
        });
        Async.run(task);
    }

    private Window window() {
        return chooseBtn.getScene().getWindow();
    }
}
