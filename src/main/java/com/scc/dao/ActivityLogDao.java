package com.scc.dao;

import com.scc.model.ActivityLog;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ActivityLogDao {

    private static final String SELECT_COLUMNS =
            "SELECT log_id, user_id, action_type, description, timestamp FROM activity_logs ";

    // CREATE: record one audit entry (parameterized)
    public boolean logAction(int userId, String actionType, String description) {
        String sql = "INSERT INTO activity_logs (user_id, action_type, description) VALUES (?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            pstmt.setString(2, actionType);
            pstmt.setString(3, description);
            pstmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.err.println("Failed to write activity log: " + e.getMessage());
            return false;
        }
    }

    // READ (all) -> newest first
    public List<ActivityLog> getAllLogs() {
        String sql = SELECT_COLUMNS + "ORDER BY timestamp DESC, log_id DESC";
        List<ActivityLog> logs = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                logs.add(mapRow(rs));
            }
        } catch (SQLException e) {
            System.err.println("Failed to load activity logs: " + e.getMessage());
        }
        return logs;
    }

    // READ (by user) -> newest first
    public List<ActivityLog> getLogsByUserId(int userId) {
        String sql = SELECT_COLUMNS + "WHERE user_id = ? ORDER BY timestamp DESC, log_id DESC";
        List<ActivityLog> logs = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    logs.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Failed to load logs for user: " + e.getMessage());
        }
        return logs;
    }

    // DELETE: clear all historical logs (the caller must confirm first)
    public boolean clearLogs() {
        String sql = "DELETE FROM activity_logs";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(sql);
            return true;
        } catch (SQLException e) {
            System.err.println("Failed to clear activity logs: " + e.getMessage());
            return false;
        }
    }

    private ActivityLog mapRow(ResultSet rs) throws SQLException {
        return new ActivityLog(
                rs.getInt("log_id"),
                rs.getInt("user_id"),
                rs.getString("action_type"),
                rs.getString("description"),
                rs.getString("timestamp")
        );
    }
}