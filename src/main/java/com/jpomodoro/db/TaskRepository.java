package com.jpomodoro.db;

import com.jpomodoro.model.Task;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class TaskRepository {

    public Task add(String title) {
        String sql = "INSERT INTO tasks(title, done, created_at) VALUES (?, 0, ?)";
        Instant now = Instant.now();
        Connection c = Database.get();
        try (PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, title);
            ps.setString(2, now.toString());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return new Task(keys.getLong(1), title, false, now, null);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public List<Task> listOpen() {
        String sql = "SELECT id, title, done, created_at, completed_at FROM tasks ORDER BY done ASC, id ASC";
        List<Task> out = new ArrayList<>();
        Connection c = Database.get();
        try (PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String completed = rs.getString("completed_at");
                out.add(new Task(
                        rs.getLong("id"),
                        rs.getString("title"),
                        rs.getInt("done") == 1,
                        Instant.parse(rs.getString("created_at")),
                        completed != null ? Instant.parse(completed) : null
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
        Connection c = Database.get();
        try (PreparedStatement ps = c.prepareStatement("DELETE FROM tasks WHERE id = ?")) {
            ps.setLong(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
