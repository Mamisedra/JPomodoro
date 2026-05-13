package com.jpomodoro.db;

import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThat;

class MigrationRunnerTest {

    @Test
    void appliesV1OnFreshDb() throws Exception {
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite::memory:")) {
            MigrationRunner.run(conn);

            assertThat(tableExists(conn, "tasks")).isTrue();
            assertThat(tableExists(conn, "sessions")).isTrue();
            assertThat(tableExists(conn, "schema_migrations")).isTrue();
            assertThat(versionsApplied(conn)).contains(1);
        }
    }

    @Test
    void isIdempotent() throws Exception {
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite::memory:")) {
            MigrationRunner.run(conn);
            MigrationRunner.run(conn);

            assertThat(rowCount(conn, "schema_migrations")).isEqualTo(1);
        }
    }

    @Test
    void adoptsLegacyDbWithExistingTablesButNoMigrationsTable() throws Exception {
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite::memory:")) {
            try (Statement st = conn.createStatement()) {
                st.executeUpdate("""
                    CREATE TABLE tasks (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        title TEXT NOT NULL,
                        done INTEGER NOT NULL DEFAULT 0,
                        created_at TEXT NOT NULL,
                        completed_at TEXT
                    )
                """);
                st.executeUpdate("INSERT INTO tasks(title, created_at) VALUES('legacy', '2025-01-01T00:00:00Z')");
            }

            MigrationRunner.run(conn);

            assertThat(versionsApplied(conn)).contains(1);
            assertThat(rowCount(conn, "tasks")).isEqualTo(1);
        }
    }

    private boolean tableExists(Connection conn, String name) throws Exception {
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(
                     "SELECT name FROM sqlite_master WHERE type='table' AND name='" + name + "'")) {
            return rs.next();
        }
    }

    private int rowCount(Connection conn, String table) throws Exception {
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM " + table)) {
            rs.next();
            return rs.getInt(1);
        }
    }

    private java.util.List<Integer> versionsApplied(Connection conn) throws Exception {
        java.util.List<Integer> out = new java.util.ArrayList<>();
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT version FROM schema_migrations ORDER BY version")) {
            while (rs.next()) out.add(rs.getInt(1));
        }
        return out;
    }
}
