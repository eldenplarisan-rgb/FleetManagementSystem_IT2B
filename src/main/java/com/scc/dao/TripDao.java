package com.scc.dao;

import com.scc.model.Trip;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

// Trips are stored in the database so Admin, Dispatcher and Driver all see the same data.
// trips.driver_id is the user_id of an APPROVED user whose role is DRIVER.
public class TripDao {

    private static final String COLS =
            "trip_id, vehicle_id, driver_id, origin, destination, departure, trip_status";

    public TripDao() {
        String sql = "CREATE TABLE IF NOT EXISTS trips ("
                + "trip_id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "vehicle_id INTEGER NOT NULL, "
                + "driver_id INTEGER NOT NULL, "
                + "origin TEXT NOT NULL, "
                + "destination TEXT NOT NULL, "
                + "departure TEXT NOT NULL, "
                + "trip_status TEXT NOT NULL DEFAULT 'Scheduled', "
                + "created_by INTEGER)";
        try (Connection c = DatabaseConnection.getConnection();
             Statement st = c.createStatement()) {
            st.execute(sql);
        } catch (SQLException e) {
            System.err.println("Trip table error: " + e.getMessage());
        }
    }

    // CREATE -> returns the new trip ID, or -1 if it failed
    public int addTrip(int vehicleId, int driverId, String origin, String destination,
                       String departure, String status, int createdBy) {
        String sql = "INSERT INTO trips (vehicle_id, driver_id, origin, destination, departure, trip_status, created_by) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, vehicleId);
            ps.setInt(2, driverId);
            ps.setString(3, origin);
            ps.setString(4, destination);
            ps.setString(5, departure);
            ps.setString(6, status);
            ps.setInt(7, createdBy);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("Add trip error: " + e.getMessage());
        }
        return -1;
    }

    // READ
    public List<Trip> getAllTrips() {
        return query("SELECT " + COLS + " FROM trips ORDER BY departure DESC, trip_id DESC");
    }

    public List<Trip> getTripsByDriver(int driverId) {
        return query("SELECT " + COLS + " FROM trips WHERE driver_id = ? ORDER BY departure DESC, trip_id DESC", driverId);
    }

    public List<Trip> getTripsByStatus(String status) {
        return query("SELECT " + COLS + " FROM trips WHERE LOWER(trip_status) = LOWER(?) "
                + "ORDER BY departure DESC, trip_id DESC", status);
    }

    public Trip findById(int tripId) {
        List<Trip> r = query("SELECT " + COLS + " FROM trips WHERE trip_id = ?", tripId);
        return r.isEmpty() ? null : r.get(0);
    }

    public int countByStatus(String status) {
        String sql = "SELECT COUNT(*) FROM trips WHERE LOWER(trip_status) = LOWER(?)";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, status);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("Count trips error: " + e.getMessage());
        }
        return 0;
    }

    // Is this driver currently on an ONGOING trip?
    public boolean hasOngoingTrip(int driverId) {
        String sql = "SELECT 1 FROM trips WHERE driver_id = ? AND LOWER(trip_status) = 'ongoing' LIMIT 1";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, driverId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            System.err.println("Check driver error: " + e.getMessage());
            return false;
        }
    }

    // Scheduling conflict: same vehicle OR same driver already has an active trip
    // at the exact same departure. Returns the conflicting trip ID, or -1 if none.
    public int findConflict(int vehicleId, int driverId, String departure, int excludeTripId) {
        String sql = "SELECT trip_id FROM trips WHERE departure = ? "
                + "AND LOWER(trip_status) IN ('scheduled', 'ongoing') "
                + "AND (vehicle_id = ? OR driver_id = ?) AND trip_id <> ? LIMIT 1";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, departure);
            ps.setInt(2, vehicleId);
            ps.setInt(3, driverId);
            ps.setInt(4, excludeTripId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("Conflict check error: " + e.getMessage());
        }
        return -1;
    }

    // Returns an error message if the trip cannot be started right now, otherwise null.
    public String checkCanStart(Trip t) {
        if (hasOngoingTrip(t.driverId())) {
            return "This driver already has an ONGOING trip.";
        }
        String sql = "SELECT status FROM vehicles WHERE vehicle_id = ?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, t.vehicleId());
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return "The assigned vehicle no longer exists.";
                String vs = rs.getString(1);
                if (!"Available".equalsIgnoreCase(vs)) {
                    return "The assigned vehicle is not available (status: " + vs + ").";
                }
            }
        } catch (SQLException e) {
            return "Could not check the vehicle: " + e.getMessage();
        }
        return null;
    }

    // UPDATE details (not the status) -> use updateStatus() for that
    public boolean updateTrip(Trip t) {
        String sql = "UPDATE trips SET vehicle_id = ?, driver_id = ?, origin = ?, destination = ?, departure = ? "
                + "WHERE trip_id = ?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, t.vehicleId());
            ps.setInt(2, t.driverId());
            ps.setString(3, t.origin());
            ps.setString(4, t.destination());
            ps.setString(5, t.departure());
            ps.setInt(6, t.id());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Update trip error: " + e.getMessage());
            return false;
        }
    }

    // UPDATE status. Also keeps the vehicle status in sync, in one transaction:
    //   Ongoing              -> vehicle becomes "In Service"
    //   Completed/Cancelled  -> vehicle goes back to "Available"
    public boolean updateStatus(int tripId, String newStatus) {
        try (Connection c = DatabaseConnection.getConnection()) {
            c.setAutoCommit(false);
            try {
                int vehicleId = -1;
                try (PreparedStatement ps = c.prepareStatement("SELECT vehicle_id FROM trips WHERE trip_id = ?")) {
                    ps.setInt(1, tripId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) vehicleId = rs.getInt(1);
                    }
                }
                if (vehicleId < 0) { c.rollback(); return false; }

                try (PreparedStatement ps = c.prepareStatement("UPDATE trips SET trip_status = ? WHERE trip_id = ?")) {
                    ps.setString(1, newStatus);
                    ps.setInt(2, tripId);
                    ps.executeUpdate();
                }
                syncVehicle(c, vehicleId, newStatus);
                c.commit();
                return true;
            } catch (SQLException e) {
                c.rollback();
                throw e;
            }
        } catch (SQLException e) {
            System.err.println("Update trip status error: " + e.getMessage());
            return false;
        }
    }

    // DELETE (frees the vehicle if the trip was still ongoing)
    public boolean deleteTrip(int tripId) {
        try (Connection c = DatabaseConnection.getConnection()) {
            c.setAutoCommit(false);
            try {
                int vehicleId = -1;
                boolean wasOngoing = false;
                try (PreparedStatement ps = c.prepareStatement(
                        "SELECT vehicle_id, trip_status FROM trips WHERE trip_id = ?")) {
                    ps.setInt(1, tripId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            vehicleId = rs.getInt(1);
                            wasOngoing = Trip.ONGOING.equalsIgnoreCase(rs.getString(2));
                        }
                    }
                }
                if (vehicleId < 0) { c.rollback(); return false; }

                try (PreparedStatement ps = c.prepareStatement("DELETE FROM trips WHERE trip_id = ?")) {
                    ps.setInt(1, tripId);
                    ps.executeUpdate();
                }
                if (wasOngoing) syncVehicle(c, vehicleId, Trip.CANCELLED);
                c.commit();
                return true;
            } catch (SQLException e) {
                c.rollback();
                throw e;
            }
        } catch (SQLException e) {
            System.err.println("Delete trip error: " + e.getMessage());
            return false;
        }
    }

    private void syncVehicle(Connection c, int vehicleId, String tripStatus) throws SQLException {
        String sql = null;
        if (Trip.ONGOING.equalsIgnoreCase(tripStatus)) {
            sql = "UPDATE vehicles SET status = 'In Service' WHERE vehicle_id = ? AND LOWER(status) = 'available'";
        } else if (Trip.COMPLETED.equalsIgnoreCase(tripStatus) || Trip.CANCELLED.equalsIgnoreCase(tripStatus)) {
            sql = "UPDATE vehicles SET status = 'Available' WHERE vehicle_id = ? AND LOWER(status) = 'in service'";
        }
        if (sql == null) return;
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, vehicleId);
            ps.executeUpdate();
        }
    }

    private List<Trip> query(String sql, Object... params) {
        List<Trip> trips = new ArrayList<>();
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) ps.setObject(i + 1, params[i]);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    trips.add(new Trip(
                            rs.getInt("trip_id"),
                            rs.getInt("vehicle_id"),
                            rs.getInt("driver_id"),
                            rs.getString("origin"),
                            rs.getString("destination"),
                            rs.getString("departure"),
                            rs.getString("trip_status")));
                }
            }
        } catch (SQLException e) {
            System.err.println("Load trips error: " + e.getMessage());
        }
        return trips;
    }
}