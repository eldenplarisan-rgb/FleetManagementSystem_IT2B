package com.scc.model;

public record ActivityLog(int logId, int userId, String actionType, String description, String timestamp) {
    @Override
    public String toString() {
        return String.format("[%d] User %d | %s | %s | %s", logId, userId, actionType, description, timestamp);
    }
}