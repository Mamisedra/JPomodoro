package com.jpomodoro.db.migrations;

import com.jpomodoro.db.Migration;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public final class V2__task_extensions implements Migration {

    @Override
    public int version() { return 2; }

    @Override
    public String description() { return "tasks : priorité + estimation + soft-delete"; }

    @Override
    public void up(Connection conn) throws SQLException {
        try (Statement st = conn.createStatement()) {
            st.executeUpdate("ALTER TABLE tasks ADD COLUMN priority TEXT NOT NULL DEFAULT 'NORMAL'");
            st.executeUpdate("ALTER TABLE tasks ADD COLUMN estimated_pomodoros INTEGER");
            st.executeUpdate("ALTER TABLE tasks ADD COLUMN deleted_at TEXT");
            st.executeUpdate("CREATE INDEX IF NOT EXISTS idx_tasks_deleted ON tasks(deleted_at)");
        }
    }
}
