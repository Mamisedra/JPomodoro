package com.jpomodoro.db;

import com.jpomodoro.model.Priority;
import com.jpomodoro.model.Task;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class TaskRepository {

    public Task add(String title) {
        String sql = "INSERT INTO tasks(title, done, priority, created_at) VALUES (?, 0, 'NORMAL', ?)";
        Instant now = Instant.now();
        Connection c = Database.get();
        try (PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, title);
            ps.setString(2, now.toString());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return new Task(keys.getLong(1), title, false, Priority.NORMAL, null, now, null, null);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public List<Task> listOpen() {
        String sql = """
            SELECT id, title, done, priority, estimated_pomodoros, created_at, completed_at, deleted_at
            FROM tasks
            WHERE deleted_at IS NULL
            ORDER BY done ASC,
                     CASE priority WHEN 'HIGH' THEN 0 WHEN 'NORMAL' THEN 1 ELSE 2 END,
                     id ASC
            """;
        List<Task> out = new ArrayList<>();
        Connection c = Database.get();
        try (PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String completed = rs.getString("completed_at");
                String deleted = rs.getString("deleted_at");
                int est = rs.getInt("estimated_pomodoros");
                Integer estimated = rs.wasNull() ? null : est;
                out.add(new Task(
                        rs.getLong("id"),
                        rs.getString("title"),
                        rs.getInt("done") == 1,
                        Priority.parse(rs.getString("priority")),
                        estimated,
                        Instant.parse(rs.getString("created_at")),
                        completed != null ? Instant.parse(completed) : null,
                        deleted != null ? Instant.parse(deleted) : null
                ));
            }
            return out;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void toggleDone(long id, boolean done) {
        String sql = "UPDATE tasks SET done = ?, completed_at = ? WHERE id = ?";
        Connection c = Database.get();
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, done ? 1 : 0);
            ps.setString(2, done ? Instant.now().toString() : null);
            ps.setLong(3, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void delete(long id) {
        String sql = "UPDATE tasks SET deleted_at = ? WHERE id = ? AND deleted_at IS NULL";
        Connection c = Database.get();
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, Instant.now().toString());
            ps.setLong(2, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public Priority cyclePriority(long id) {
        Connection c = Database.get();
        try (PreparedStatement select = c.prepareStatement("SELECT priority FROM tasks WHERE id = ?")) {
            select.setLong(1, id);
            try (ResultSet rs = select.executeQuery()) {
                if (!rs.next()) throw new IllegalStateException("Tâche introuvable : " + id);
                Priority next = Priority.parse(rs.getString(1)).next();
                try (PreparedStatement update = c.prepareStatement("UPDATE tasks SET priority = ? WHERE id = ?")) {
                    update.setString(1, next.name());
                    update.setLong(2, id);
                    update.executeUpdate();
                }
                return next;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void setEstimate(long id, Integer estimate) {
        Connection c = Database.get();
        try (PreparedStatement ps = c.prepareStatement(
                "UPDATE tasks SET estimated_pomodoros = ? WHERE id = ?")) {
            if (estimate == null || estimate <= 0) ps.setNull(1, Types.INTEGER);
            else ps.setInt(1, estimate);
            ps.setLong(2, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
