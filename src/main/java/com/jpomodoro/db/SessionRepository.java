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
import java.util.ArrayList;
import java.util.List;

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

    public List<SessionView> listLast(int limit) {
        String sql = """
            SELECT s.id, s.type, s.started_at, s.ended_at, s.completed,
                   s.task_id, t.title AS task_title,
                   sm.content AS summary,
                   (SELECT is_positive FROM feedback_entries f WHERE f.summary_id = sm.id ORDER BY f.id DESC LIMIT 1) AS feedback
            FROM sessions s
            LEFT JOIN tasks t ON t.id = s.task_id
            LEFT JOIN summaries sm ON sm.session_id = s.id
            ORDER BY s.id DESC
            LIMIT ?
            """;
        List<SessionView> out = new ArrayList<>();
        Connection c = Database.get();
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String endedRaw = rs.getString("ended_at");
                    Long taskId = (Long) rs.getObject("task_id");
                    Integer fb = (Integer) rs.getObject("feedback");
                    out.add(new SessionView(
                            rs.getLong("id"),
                            SessionType.valueOf(rs.getString("type")),
                            Instant.parse(rs.getString("started_at")),
                            endedRaw == null ? null : Instant.parse(endedRaw),
                            rs.getInt("completed") == 1,
                            taskId,
                            rs.getString("task_title"),
                            rs.getString("summary"),
                            fb == null ? null : (fb == 1)
                    ));
                }
            }
            return out;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public int[] countFocusPerDay(LocalDate from, LocalDate to) {
        int days = (int) (to.toEpochDay() - from.toEpochDay() + 1);
        int[] counts = new int[days];
        String sql = """
            SELECT started_at FROM sessions
            WHERE type = 'FOCUS' AND completed = 1 AND started_at >= ? AND started_at < ?
            """;
        Connection c = Database.get();
        ZoneId zone = ZoneId.systemDefault();
        Instant rangeStart = from.atStartOfDay(zone).toInstant();
        Instant rangeEnd = to.plusDays(1).atStartOfDay(zone).toInstant();
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, rangeStart.toString());
            ps.setString(2, rangeEnd.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Instant t = Instant.parse(rs.getString(1));
                    LocalDate day = t.atZone(zone).toLocalDate();
                    int idx = (int) (day.toEpochDay() - from.toEpochDay());
                    if (idx >= 0 && idx < days) counts[idx]++;
                }
            }
            return counts;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public int streakDays() {
        LocalDate today = LocalDate.now();
        ZoneId zone = ZoneId.systemDefault();
        int streak = 0;
        Connection c = Database.get();
        String sql = """
            SELECT 1 FROM sessions
            WHERE type='FOCUS' AND completed=1 AND started_at >= ? AND started_at < ?
            LIMIT 1
            """;
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            for (LocalDate day = today; ; day = day.minusDays(1)) {
                Instant dayStart = day.atStartOfDay(zone).toInstant();
                Instant dayEnd = day.plusDays(1).atStartOfDay(zone).toInstant();
                ps.setString(1, dayStart.toString());
                ps.setString(2, dayEnd.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        streak++;
                    } else {
                        if (day.equals(today)) continue;
                        break;
                    }
                }
                if (streak > 365) break;
            }
            return streak;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public int countFocusForTask(long taskId) {
        String sql = """
            SELECT COUNT(*) FROM sessions
            WHERE type = 'FOCUS' AND completed = 1 AND task_id = ?
            """;
        Connection c = Database.get();
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, taskId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public record DailyStats(int focusCount, long focusSeconds) {}
}
