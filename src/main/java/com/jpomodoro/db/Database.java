package com.jpomodoro.db;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public final class Database {
    private static Connection connection;

    public static synchronized Connection get() {
        if (connection != null) return connection;
        try {
            Path dir = Path.of(System.getProperty("user.home"), ".pomodoro");
            Files.createDirectories(dir);
            Path dbPath = dir.resolve("data.db");
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
            initSchema(connection);
            return connection;
        } catch (Exception e) {
            throw new RuntimeException("Impossible d'initialiser la base SQLite", e);
        }
    }

    private static void initSchema(Connection conn) throws SQLException {
        try (Statement st = conn.createStatement()) {
            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS tasks (
                    id           INTEGER PRIMARY KEY AUTOINCREMENT,
                    title        TEXT NOT NULL,
                    done         INTEGER NOT NULL DEFAULT 0,
                    created_at   TEXT NOT NULL,
                    completed_at TEXT
                )
            """);
            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS sessions (
                    id          INTEGER PRIMARY KEY AUTOINCREMENT,
                    type        TEXT NOT NULL,
                    task_id     INTEGER REFERENCES tasks(id),
                    started_at  TEXT NOT NULL,
                    ended_at    TEXT,
                    completed   INTEGER NOT NULL DEFAULT 0
                )
            """);
            st.executeUpdate("CREATE INDEX IF NOT EXISTS idx_sessions_started ON sessions(started_at)");
        }
    }

    private Database() {}
}
