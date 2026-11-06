package com.scc.dao;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class Databaseinitializer {

    // Creates the tables on startup if they do not exist yet.
    public static void initialize() {
        String createUsers =
                "CREATE TABLE IF NOT EXISTS users ("
                        + "user_id INTEGER PRIMARY KEY AUTOINCREMENT, "
                        + "name TEXT NOT NULL, "
                        + "username TEXT NOT NULL UNIQUE, "
                        + "password TEXT NOT NULL, "
                        + "roles TEXT NOT NULL, "
                        + "status TEXT NOT NULL DEFAULT 'PENDING'"
                        + ")";

        String createActivityLogs =
                "CREATE TABLE IF NOT EXISTS activity_logs ("
                        + "log_id INTEGER PRIMARY KEY AUTOINCREMENT, "
                        + "user_id INTEGER NOT NULL, "
                        + "action_type TEXT NOT NULL, "
                        + "description TEXT, "
                        + "timestamp DATETIME DEFAULT (datetime('now', 'localtime'))"
                        + ")";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(createUsers);
            stmt.execute(createActivityLogs);
        } catch (SQLException e) {
            System.err.println("Error creating tables: " + e.getMessage());
        }
    }
}