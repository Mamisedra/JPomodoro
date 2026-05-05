package com.jpomodoro.db;

import com.jpomodoro.timer.SessionType;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

public class SessionRepository {

    public long start(SessionType type, Long taskId) {
        String sql = "INSERT INTO sessions(type, task_id, started_at, completed) VALUES (?, ?, ?, 0)";
        Connection c = Database.get();
        try (PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, type.name());
            if (taskId != null) ps.setLong(2, taskId); else ps.setNull(2, java.sql.Types.INTEGER);
            ps.setString(3, Instant.now().toString());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return keys.getLong(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void complete(long sessionId) {
        String sql = "UPDATE sessions SET ended_at = ?, completed = 1 WHERE id = ?";
        Connection c = Database.get();
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, Instant.now().toString());
            ps.setLong(2, sessionId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void cancel(long sessionId) {
        String sql = "UPDATE sessions SET ended_at = ?, completed = 0 WHERE id = ?";
        Connection c = Database.get();
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, Instant.now().toString());
            ps.setLong(2, sessionId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public DailyStats todayStats() {
        Instant startOfDay = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant();
        String sql = """
            SELECT COUNT(*) AS n,
                   COALESCE(SUM(strftime('%s', ended_at) - strftime('%s', started_at)), 0) AS secs
            FROM sessions
            WHERE type = 'FOCUS'
              AND completed = 1
              AND started_at >= ?
        """;
        Connection c = Database.get();
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, startOfDay.toString());
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return new DailyStats(rs.getInt("n"), rs.getLong("secs"));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public record DailyStats(int focusCount, long focusSeconds) {}
}
