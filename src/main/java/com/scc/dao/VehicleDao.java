package com.scc.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class VehicleDao {

    // Parehas nga database file sa imong project (FleetManagementSystem.db)
    private static final String URL = "jdbc:sqlite:FleetManagementSystem.db";

    public VehicleDao() {
        createTable();
    }

    // mo-create sa vehicles table kung wala pa
    private void createTable() {
        String sql = "CREATE TABLE IF NOT EXISTS vehicles ("
                + "vehicle_id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "plate TEXT NOT NULL, "
                + "model TEXT NOT NULL, "
                + "capacity INTEGER NOT NULL, "
                + "status TEXT NOT NULL)";
        try (Connection c = DriverManager.getConnection(URL);
             Statement st = c.createStatement()) {
            st.execute(sql);
        } catch (SQLException e) {
            System.out.println("Vehicle table error: " + e.getMessage());
        }
    }

    // Returns the new vehicle ID, or -1 if it failed
    public int addVehicle(String plate, String model, int capacity, String status) {
        String sql = "INSERT INTO vehicles (plate, model, capacity, status) VALUES (?, ?, ?, ?)";
        try (Connection c = DriverManager.getConnection(URL);
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, plate);
            ps.setString(2, model);
            ps.setInt(3, capacity);
            ps.setString(4, status);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
            }
        } catch (SQLException e) {
            System.out.println("Add vehicle error: " + e.getMessage());
        }
        return -1;
    }

    // Each row: {id, plate, model, capacity, status}
    public List<String[]> getAllVehicles() {
        List<String[]> rows = new ArrayList<>();
        String sql = "SELECT vehicle_id, plate, model, capacity, status FROM vehicles ORDER BY vehicle_id";
        try (Connection c = DriverManager.getConnection(URL);
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                rows.add(new String[]{
                        String.valueOf(rs.getInt("vehicle_id")),
                        rs.getString("plate"),
                        rs.getString("model"),
                        String.valueOf(rs.getInt("capacity")),
                        rs.getString("status")
                });
            }
        } catch (SQLException e) {
            System.out.println("Load vehicles error: " + e.getMessage());
        }
        return rows;
    }

    public boolean updateVehicle(int id, String plate, String model, int capacity, String status) {
        String sql = "UPDATE vehicles SET plate = ?, model = ?, capacity = ?, status = ? WHERE vehicle_id = ?";
        try (Connection c = DriverManager.getConnection(URL);
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, plate);
            ps.setString(2, model);
            ps.setInt(3, capacity);
            ps.setString(4, status);
            ps.setInt(5, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.out.println("Update vehicle error: " + e.getMessage());
            return false;
        }
    }

    public boolean deleteVehicle(int id) {
        String sql = "DELETE FROM vehicles WHERE vehicle_id = ?";
        try (Connection c = DriverManager.getConnection(URL);
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.out.println("Delete vehicle error: " + e.getMessage());
            return false;
        }
    }
}