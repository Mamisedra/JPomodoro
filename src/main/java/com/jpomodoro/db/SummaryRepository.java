package com.jpomodoro.db;

import com.jpomodoro.model.Summary;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.Instant;
import java.util.Optional;

public class SummaryRepository {

    public Summary save(long sessionId, String content, String model) {
        String sql = "INSERT INTO summaries(session_id, content, model, created_at) VALUES (?, ?, ?, ?)";
        Instant now = Instant.now();
        Connection c = Database.get();
        try (PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, sessionId);
            ps.setString(2, content);
            if (model == null) ps.setNull(3, Types.VARCHAR); else ps.setString(3, model);
            ps.setString(4, now.toString());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return new Summary(keys.getLong(1), sessionId, content, model, now);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public Optional<Summary> findBySession(long sessionId) {
        String sql = """
            SELECT id, session_id, content, model, created_at
            FROM summaries WHERE session_id = ? ORDER BY id DESC LIMIT 1
            """;
        Connection c = Database.get();
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, sessionId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return Optional.empty();
                return Optional.of(new Summary(
                        rs.getLong(1), rs.getLong(2), rs.getString(3),
                        rs.getString(4), Instant.parse(rs.getString(5))));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
