# Git plan: commit the project step by step

Each step below only depends on the steps before it, so the project compiles after every merge.
Use one feature branch per step, then merge into `main` (a Pull Request on GitHub is best,
because it shows up in your commit history for the assignment).

## Setup (once)
1. Create an empty repo on GitHub (no README), copy its URL.
2. In IntelliJ: File > New > Project from Version Control > paste the URL (or `git clone <url>`).
3. Copy this project's files into the cloned folder, but commit them in the steps below.

## Steps

| # | Branch | Files to add | Commit message |
|---|--------|--------------|----------------|
| 1 | `main` | `pom.xml`, `.gitignore`, `README.md`, `docs/` | `Set up Maven project with JavaFX, Gson and SQLite` |
| 2 | `feature/utils-and-model` | `util/*`, `model/*` | `Add utility classes and data model` |
| 3 | `feature/scanner` | `scanner/*` | `Add multi-threaded FileScanTask with pause and cancel` |
| 4 | `feature/database` | `db/*` | `Add SQLite database manager for scan history` |
| 5 | `feature/json` | `json/*` | `Add JSON export and import with Gson` |
| 6 | `feature/api` | `api/*` | `Add VirusTotal hash lookup client` |
| 7 | `feature/history-ui` | `controller/HistoryController.java`, `resources/.../history.fxml` | `Add scan history tab` |
| 8 | `feature/main-ui` | `App.java`, `Launcher.java`, `controller/MainController.java`, `resources/.../main.fxml`, `resources/.../styles.css` | `Add main scanner screen and app entry point` |
| 9 | `feature/polish` | any fixes you make | `Polish UI and fix bugs` |

## Commands for one step (example: step 3)
```bash
git checkout main
git pull
git checkout -b feature/scanner
git add src/main/java/com/khalid/filescanner/scanner
git commit -m "Add multi-threaded FileScanTask with pause and cancel"
git push -u origin feature/scanner
# on GitHub: open a Pull Request, merge it, then:
git checkout main
git pull
```
Step 1 goes straight to `main`:
```bash
git add pom.xml .gitignore README.md docs
git commit -m "Set up Maven project with JavaFX, Gson and SQLite"
git push -u origin main
```

## Tips
- Commit and push on different days if you can; a real history looks better than one big dump.
- After each step, run `mvn compile` (or Build > Build Project in IntelliJ) to check it builds.
- Before the demo video, read each class once so you can explain it. Small commits of your own
  (a comment, a tweak, a new filter) make the history genuinely yours.
- Submit on Google Classroom: GitHub repo link + demo video link.
