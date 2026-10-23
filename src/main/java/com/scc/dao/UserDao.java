package com.scc.dao;

import com.scc.model.User;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class UserDao {

    public UserDao() {

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement()) {
            defaultAdminGenerator(conn);
        } catch (SQLException e) {
            System.err.println("Error seeding default admin: " + e.getMessage());
        }

    }

    public void defaultAdminGenerator(Connection conn){
        String checksql = "SELECT COUNT(*) FROM users WHERE roles = 'ADMIN'";
        String insertSql = "INSERT INTO users(name, username, password, roles, status) VALUES('Administrator', 'admin', 'admin123', 'ADMIN', 'APPROVED')";

        try(Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(checksql)){
            if(rs.next() && rs.getInt(1) == 0){
                stmt.executeUpdate(insertSql);
                System.out.println("-> Default Admin created");
            }
        }catch (SQLException e){
            System.err.println("Error seeding defualt admin: " + e.getMessage());
        }

    }

    public boolean registerUser(String name, String username, String password, String role){

        String sql ="INSERT INTO users (name, username, password, roles, status) VALUES(?, ?, ?, ?, 'PENDING')";

        try(Connection conn = DatabaseConnection.getConnection();
            PreparedStatement pstmt = conn.prepareStatement(sql)){

            pstmt.setString(1, name);
            pstmt.setString(2,username);
            pstmt.setString(3,password);
            pstmt.setString(4, role.toUpperCase());
            pstmt.executeUpdate();
            System.out.println("\n -> Successful Registration! Waiting for Approval!");
            return true;


        } catch (SQLException e) {
            System.err.println("Failed: "+ e.getMessage() );
            return false;
        }
    }

    public User login(String username, String password){
        String sql = "SELECT * FROM users WHERE username = ? AND password = ?";

        try(Connection conn = DatabaseConnection.getConnection();
            PreparedStatement pstmt = conn.prepareStatement(sql)){
            pstmt.setString(1, username);
            pstmt.setString(2, password);
            try(ResultSet rs = pstmt.executeQuery()){
                // FIXED: ang status kuhaon sulod sa if(rs.next()), dili sa wala pa
                if(rs.next()){
                    String status = rs.getString("status");
                    if("PENDING".equalsIgnoreCase(status)){
                        System.out.println(" -> Account is pending, Waiting for Approval");
                        return null;
                    }
                    return new User(
                            rs.getInt("user_id"),
                            rs.getString("name"),
                            rs.getString("username"),
                            rs.getString("password"),
                            rs.getString("roles"),
                            status
                    );
                }else {
                    System.out.println(" -> Invalid username or password");
                }
            }
        }catch (SQLException e){
            System.out.println("Database error: " + e.getMessage());
        }
        return null;
    }

    // =====================================================================
    //  NEW: ADMIN CRUD
    // =====================================================================

    // CREATE
    public boolean addUser(String name, String username, String password, String role, String status) {
        String sql = "INSERT INTO users (name, username, password, roles, status) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, name);
            pstmt.setString(2, username);
            pstmt.setString(3, password);
            pstmt.setString(4, role.toUpperCase());
            pstmt.setString(5, status.toUpperCase());
            pstmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.err.println("Failed to add user: " + e.getMessage());
            return false;
        }
    }

    // READ (all) -> each row: {id, name, username, role, status}
    public List<String[]> getAllUsers() {
        List<String[]> rows = new ArrayList<>();
        String sql = "SELECT user_id, name, username, roles, status FROM users ORDER BY user_id";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                rows.add(new String[]{
                        String.valueOf(rs.getInt("user_id")),
                        rs.getString("name"),
                        rs.getString("username"),
                        rs.getString("roles"),
                        rs.getString("status")
                });
            }
        } catch (SQLException e) {
            System.err.println("Failed to load users: " + e.getMessage());
        }
        return rows;
    }

    // READ (one) -> {id, name, username, role, status} or null kung wala
    public String[] findUserRow(int id) {
        String sql = "SELECT user_id, name, username, roles, status FROM users WHERE user_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return new String[]{
                            String.valueOf(rs.getInt("user_id")),
                            rs.getString("name"),
                            rs.getString("username"),
                            rs.getString("roles"),
                            rs.getString("status")
                    };
                }
            }
        } catch (SQLException e) {
            System.err.println("Failed to find user: " + e.getMessage());
        }
        return null;
    }

    // UPDATE -> empty string = i-keep ang karaan nga value
    public boolean updateUser(int id, String name, String username, String password,
                              String role, String status) {
        String sql = "UPDATE users SET "
                + "name = COALESCE(NULLIF(?, ''), name), "
                + "username = COALESCE(NULLIF(?, ''), username), "
                + "password = COALESCE(NULLIF(?, ''), password), "
                + "roles = COALESCE(NULLIF(?, ''), roles), "
                + "status = COALESCE(NULLIF(?, ''), status) "
                + "WHERE user_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, name);
            pstmt.setString(2, username);
            pstmt.setString(3, password);
            pstmt.setString(4, role.toUpperCase());
            pstmt.setString(5, status.toUpperCase());
            pstmt.setInt(6, id);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Failed to update user: " + e.getMessage());
            return false;
        }
    }

    // DELETE
    public boolean deleteUser(int id) {
        String sql = "DELETE FROM users WHERE user_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Failed to delete user: " + e.getMessage());
            return false;
        }
    }

    // Aron dili ma-delete ang katapusang ADMIN
    public boolean isLastAdmin(int id) {
        String sql = "SELECT "
                + "(SELECT COUNT(*) FROM users WHERE roles = 'ADMIN') AS total, "
                + "(SELECT COUNT(*) FROM users WHERE roles = 'ADMIN' AND user_id = ?) AS isAdmin";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) return rs.getInt("total") == 1 && rs.getInt("isAdmin") == 1;
            }
        } catch (SQLException e) {
            System.err.println("Check failed: " + e.getMessage());
        }
        return false;
    }

    // =====================================================================
    //  NEW: APPROVE PENDING USER
    // =====================================================================

    // READ: PENDING lang nga users -> {id, name, username, role, status}
    public List<String[]> getPendingUsers() {
        List<String[]> rows = new ArrayList<>();
        String sql = "SELECT user_id, name, username, roles, status FROM users "
                + "WHERE status = 'PENDING' ORDER BY user_id";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                rows.add(new String[]{
                        String.valueOf(rs.getInt("user_id")),
                        rs.getString("name"),
                        rs.getString("username"),
                        rs.getString("roles"),
                        rs.getString("status")
                });
            }
        } catch (SQLException e) {
            System.err.println("Failed to load pending users: " + e.getMessage());
        }
        return rows;
    }

    // APPROVE: mo-approve lang kung PENDING pa gyud
    public boolean approveUser(int id) {
        String sql = "UPDATE users SET status = 'APPROVED' WHERE user_id = ? AND status = 'PENDING'";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Failed to approve user: " + e.getMessage());
            return false;
        }
    }
}