package com.jpomodoro.db;

import com.jpomodoro.db.migrations.V1__initial_schema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class MigrationRunner {

    private static final Logger log = LoggerFactory.getLogger(MigrationRunner.class);

    private static final List<Migration> ALL = List.of(
            new V1__initial_schema()
    );

    private MigrationRunner() {}

    public static void run(Connection conn) {
        try {
            ensureMigrationsTable(conn);
            adoptLegacyDbIfNeeded(conn);
            Set<Integer> applied = appliedVersions(conn);
            List<Migration> sorted = ALL.stream()
                    .sorted(Comparator.comparingInt(Migration::version))
                    .toList();
            for (Migration m : sorted) {
                if (applied.contains(m.version())) continue;
                log.info("Migration V{} : {}", m.version(), m.description());
                m.up(conn);
                record(conn, m);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Échec des migrations DB", e);
        }
    }

    private static void ensureMigrationsTable(Connection conn) throws SQLException {
        try (Statement st = conn.createStatement()) {
            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS schema_migrations (
                    version    INTEGER PRIMARY KEY,
                    applied_at TEXT NOT NULL
                )
            """);
        }
    }

    private static void adoptLegacyDbIfNeeded(Connection conn) throws SQLException {
        if (!tableExists(conn, "tasks")) return;
        if (!appliedVersions(conn).isEmpty()) return;
        log.info("Base existante détectée sans schema_migrations — adoption en V1");
        record(conn, new V1__initial_schema());
    }

    private static boolean tableExists(Connection conn, String name) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT name FROM sqlite_master WHERE type='table' AND name=?")) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private static Set<Integer> appliedVersions(Connection conn) throws SQLException {
        Set<Integer> versions = new HashSet<>();
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT version FROM schema_migrations")) {
            while (rs.next()) versions.add(rs.getInt(1));
        }
        return versions;
    }

    private static void record(Connection conn, Migration m) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT OR IGNORE INTO schema_migrations(version, applied_at) VALUES (?, ?)")) {
            ps.setInt(1, m.version());
            ps.setString(2, Instant.now().toString());
            ps.executeUpdate();
        }
    }
}
