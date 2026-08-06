package com.codequest.db;

import java.sql.*;

/**
 * Central place for all SQLite access.
 * Creates codequest.db (in the project's working directory) on first run,
 * builds the schema, and seeds a default Competitive Programming roadmap.
 */
public class DatabaseManager {

    private static final String DB_URL = "jdbc:sqlite:codequest.db";
    private static Connection connection;

    public static Connection getConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                connection = DriverManager.getConnection(DB_URL);
                connection.createStatement().execute("PRAGMA foreign_keys = ON;");
            }
        } catch (SQLException e) {
            throw new RuntimeException("Could not connect to database: " + e.getMessage(), e);
        }
        return connection;
    }

    public static void initialize() {
        try (Statement st = getConnection().createStatement()) {

            st.execute("""
                CREATE TABLE IF NOT EXISTS topics (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    name TEXT NOT NULL,
                    category TEXT NOT NULL,
                    order_index INTEGER NOT NULL,
                    status TEXT NOT NULL DEFAULT 'Not Started',
                    completion_percent INTEGER NOT NULL DEFAULT 0,
                    last_updated TEXT
                );
            """);

            st.execute("""
                CREATE TABLE IF NOT EXISTS problems (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    title TEXT NOT NULL,
                    platform TEXT,
                    topic TEXT,
                    difficulty TEXT,
                    status TEXT NOT NULL DEFAULT 'To Do',
                    url TEXT,
                    notes TEXT,
                    date_added TEXT,
                    date_solved TEXT
                );
            """);

            st.execute("""
                CREATE TABLE IF NOT EXISTS contest_sessions (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    name TEXT NOT NULL,
                    duration_minutes INTEGER NOT NULL,
                    problems_planned INTEGER NOT NULL,
                    problems_solved INTEGER NOT NULL DEFAULT 0,
                    score INTEGER NOT NULL DEFAULT 0,
                    started_at TEXT,
                    ended_at TEXT,
                    status TEXT NOT NULL DEFAULT 'Completed'
                );
            """);

            st.execute("""
                CREATE TABLE IF NOT EXISTS sync_log (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    timestamp TEXT NOT NULL,
                    direction TEXT NOT NULL,
                    status TEXT NOT NULL,
                    message TEXT
                );
            """);

            st.execute("""
                CREATE TABLE IF NOT EXISTS goals (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    title TEXT NOT NULL,
                    goal_type TEXT NOT NULL,
                    target_value INTEGER NOT NULL,
                    start_date TEXT NOT NULL,
                    deadline TEXT,
                    status TEXT NOT NULL DEFAULT 'Active'
                );
            """);

            seedRoadmapIfEmpty(st);

        } catch (SQLException e) {
            throw new RuntimeException("Database initialization failed: " + e.getMessage(), e);
        }
    }

    private static void seedRoadmapIfEmpty(Statement st) throws SQLException {
        ResultSet rs = st.executeQuery("SELECT COUNT(*) AS c FROM topics;");
        rs.next();
        if (rs.getInt("c") > 0) return;

        String[][] seed = {
            {"Basics", "Arrays & Time Complexity"},
            {"Basics", "Strings"},
            {"Basics", "Sorting Algorithms"},
            {"Basics", "Binary Search"},
            {"Basics", "Bitwise Operations"},
            {"Intermediate", "Recursion & Backtracking"},
            {"Intermediate", "Two Pointers"},
            {"Intermediate", "Sliding Window"},
            {"Intermediate", "Stacks & Queues"},
            {"Intermediate", "Hashing"},
            {"Advanced", "Graph Traversal (BFS/DFS)"},
            {"Advanced", "Shortest Paths (Dijkstra/Bellman-Ford)"},
            {"Advanced", "Dynamic Programming"},
            {"Advanced", "Trees & Binary Search Trees"},
            {"Advanced", "Trie"},
            {"Expert", "Segment Tree & Fenwick Tree"},
            {"Expert", "Advanced DP (Bitmask, Digit DP)"},
            {"Expert", "Network Flow"},
            {"Expert", "Game Theory"},
            {"Expert", "Number Theory"}
        };

        String insert = "INSERT INTO topics (name, category, order_index, status, completion_percent) VALUES (?, ?, ?, 'Not Started', 0);";
        try (PreparedStatement ps = getConnection().prepareStatement(insert)) {
            int order = 0;
            for (String[] row : seed) {
                ps.setString(1, row[1]);
                ps.setString(2, row[0]);
                ps.setInt(3, order++);
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }
}
