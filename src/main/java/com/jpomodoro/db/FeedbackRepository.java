package com.jpomodoro.db;

import com.jpomodoro.model.Feedback;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;

public class FeedbackRepository {

    public Feedback save(long summaryId, boolean isPositive) {
        String sql = "INSERT INTO feedback_entries(summary_id, is_positive, created_at) VALUES (?, ?, ?)";
        Instant now = Instant.now();
        Connection c = Database.get();
        try (PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, summaryId);
            ps.setInt(2, isPositive ? 1 : 0);
            ps.setString(3, now.toString());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return new Feedback(keys.getLong(1), summaryId, isPositive, now);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
