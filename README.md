# Multi-threaded File/Folder Scanner with Progress Dashboard

A JavaFX desktop app that recursively scans a folder on background threads and shows live
progress, a searchable/filterable results table, and scan statistics, without freezing the UI.

## Features
- Choose a folder and scan it with a configurable number of worker threads
- Live progress bar, status text, pause/resume and cancel
- Searchable and filterable table (name search + file category), sortable columns
- Statistics panel: files, folders, total size, errors, elapsed time, top file types chart
- Scan history stored in SQLite (view, load, annotate, delete)
- Export / import scan results as JSON
- Optional VirusTotal hash lookup for a selected file (API call, JSON response parsing)

## Course topics covered (Weeks 1-7)
| Week | Topic | Where in the project |
|------|-------|----------------------|
| 1 | Java / OOP | `model`, `scanner.Pausable` interface, `util.AppException`, packages, exception handling |
| 2 | Git | Feature branches and commit history (see `docs/COMMIT_PLAN.md`) |
| 3 | JavaFX GUI | `main.fxml`, `history.fxml` (Scene Builder), controllers, event handlers |
| 4 | Multithreading | `scanner.FileScanTask` (Task + ExecutorService, atomics, pause/resume with wait/notify) |
| 6 | SQLite + JavaFX | `db.DatabaseManager` (create, insert, update, delete, query), History tab |
| 7 | JSON + API | `json.JsonService` (Gson), `api.VirusTotalClient` (HttpClient + JSON parsing) |

## Requirements
- JDK 17 or newer
- IntelliJ IDEA (Maven project) and Scene Builder for editing the FXML files

## Run
- IntelliJ: open the folder as a Maven project, then run `com.khalid.filescanner.Launcher`
- Terminal: `mvn javafx:run`

## VirusTotal (optional)
Get a free API key at virustotal.com, then paste it in the app or set the `VT_API_KEY`
environment variable. Only the file's SHA-256 hash is sent, never the file itself.

## Data location
The SQLite history file is created at `~/.filescanner/history.db`.
