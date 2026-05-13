package com.jpomodoro.db.migrations;

import com.jpomodoro.db.Migration;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public final class V3__summaries_and_feedback implements Migration {

    @Override
    public int version() { return 3; }

    @Override
    public String description() { return "tables summaries + feedback_entries"; }

    @Override
    public void up(Connection conn) throws SQLException {
        try (Statement st = conn.createStatement()) {
            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS summaries (
                    id          INTEGER PRIMARY KEY AUTOINCREMENT,
                    session_id  INTEGER NOT NULL REFERENCES sessions(id),
                    content     TEXT NOT NULL,
                    model       TEXT,
                    created_at  TEXT NOT NULL
                )
            """);
            st.executeUpdate("CREATE INDEX IF NOT EXISTS idx_summaries_session ON summaries(session_id)");
            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS feedback_entries (
                    id          INTEGER PRIMARY KEY AUTOINCREMENT,
                    summary_id  INTEGER NOT NULL REFERENCES summaries(id),
                    is_positive INTEGER NOT NULL,
                    created_at  TEXT NOT NULL
                )
            """);
            st.executeUpdate("CREATE INDEX IF NOT EXISTS idx_feedback_summary ON feedback_entries(summary_id)");
        }
    }
}
