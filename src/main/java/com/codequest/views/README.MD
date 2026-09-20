 # CodeQuest

A desktop application that helps Competitive Programming students learn, track, and improve — instead of just submitting solutions. Built with JavaFX and backed by a local SQLite database, CodeQuest brings learning roadmaps, problem tracking, algorithm visualizations, and performance analytics into one place.

> Built for **CSE 4402 – Visual Programming Lab**, in support of **SDG 4: Quality Education**.

## Features

| # | Feature | What it does |
|---|---------|---------------|
| 1 | **Learning Roadmap** | Topics grouped by category (Basics → Intermediate → Advanced → Expert) shown as a connected path. Clicking a node lets you jump straight to updating its status. |
| 2 | **Topic Progress Tracker** | A table of every topic with inline status/percent editing, rolled-up progress bars per category, and an overall completion summary. |
| 3 | **Problem Management System** | A personal problem bank with full CRUD — add, edit, delete, filter by platform/difficulty/status, and free-text search. Paste a problem URL (Codeforces, LeetCode, AtCoder, HackerRank) and the platform is auto-detected and the title is auto-fetched, so everything else is optional. |
| 4 | **Performance Analytics** | A dashboard with solved/total stat cards, a difficulty breakdown pie chart, a per-category progress bar chart, and a 14-day solving trend line chart. |
| 5 | **Algorithm Visualization** | Step-by-step animated walkthroughs of Bubble Sort, Binary Search, Linked List Traversal, Merge Sort, BFS, DFS, Dijkstra's Algorithm, and DP (Fibonacci memoization) — play, pause, and step through each one. |
| 6 | **Weak Topic Detection** | Cross-references topic completion with problem-solving activity to flag areas that need more focus, with an adjustable sensitivity threshold. |
| 7 | **Goal Setting System** | Set targets like "solve 20 problems" or "complete 5 topics," with an optional deadline. Progress is computed live from your actual activity. |
| 8 | **Achievement System** | Badges computed live from your current stats — always accurate, no separate "unlocked" state to maintain. |
| 9 | **Contest Simulation** | Run a timed, Codeforces-style mock contest: pick a duration and problem count, start a live countdown, pause/resume it, log solves as you go, and save a final score to your contest history. |
| 10 | **Online Synchronization** | Since CodeQuest is offline-first, this module *simulates* cloud sync by exporting/importing a JSON snapshot (`cloud_snapshot.json`) — a stand-in for a real backend that keeps the UI, progress feedback, and sync log fully functional. |

## Tech Stack

- **Language:** Java 17
- **UI:** JavaFX 21 (hand-built views, no FXML)
- **Storage:** SQLite (via `sqlite-jdbc`), stored locally as `codequest.db`
- **Build:** Maven

## Project Structure

```
Codequest/
├── pom.xml
├── codequest.db                  # created automatically on first run
└── src/main/
    ├── resources/
    │   └── style.css              # app-wide styling
    └── java/com/codequest/
        ├── Launcher.java          # entry point (works around JavaFX's module requirements)
        ├── Main.java              # app shell: sidebar nav + view switching
        ├── db/
        │   └── DatabaseManager.java   # schema creation + default roadmap seed
        ├── model/
        │   ├── Problem.java
        │   ├── Topic.java
        │   ├── Goal.java
        │   ├── ContestSession.java
        │   └── SyncLog.java
        ├── util/
        │   └── DateUtil.java
        └── views/
            ├── RoadmapView.java
            ├── ProgressTrackerView.java
            ├── ProblemManagementView.java
            ├── AnalyticsView.java
            ├── VisualizationView.java
            ├── WeakTopicView.java
            ├── GoalSettingView.java
            ├── AchievementView.java
            ├── ContestSimulationView.java
            └── SyncView.java
```

## Getting Started

### Prerequisites
- JDK 17 or newer
- Maven (or an IDE with Maven support, e.g. IntelliJ IDEA / VS Code)

### Run it

**From the command line:**
```bash
mvn clean javafx:run
```

**From an IDE:**
Open `src/main/java/com/codequest/Launcher.java` and run its `main` method (`Launcher`, not `Main`, is the entry point — this sidesteps JavaFX's restriction on the main class directly extending `Application` when launched from a plain classpath).

On first run, CodeQuest creates `codequest.db` in the project's working directory, builds the schema, and seeds a default roadmap so the app isn't empty on first launch.

### Database

The database is a single local SQLite file — no server or setup required. Core tables: `topics`, `problems`, `contest_sessions`, `goals`, and `sync_log`. All schema creation lives in `DatabaseManager.initialize()`.

## Notes

- **Problem URLs:** the auto-fill feature in the Problem Bank makes a live HTTP request to the pasted URL, so it needs an internet connection to fetch a title. If it can't (offline, or the site blocks the request), it falls back to a readable title derived from the URL itself, and tells you why in the dialog.
- **Online Sync is simulated:** it's a real, working import/export flow against a local JSON file, standing in for what would be a REST/Firebase backend in a networked version.


URL of video presentation : 

 https://www.youtube.com/watch?v=jntl_U6WhHI 


 