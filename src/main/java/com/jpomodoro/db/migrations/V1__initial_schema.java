package com.jpomodoro.db.migrations;

import com.jpomodoro.db.Migration;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public final class V1__initial_schema implements Migration {

    @Override
    public int version() { return 1; }

    @Override
    public String description() { return "tables initiales tasks + sessions"; }

    @Override
    public void up(Connection conn) throws SQLException {
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
}
