package com.scc.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Locale;

public class UserDao {

    public boolean registerUser(String username, String password, String role){

        String sql ="INSERT INTO users (username, password, roles, status) VALUES(?, ?, ?, 'PENDING')";

        try(Connection conn = DatabaseConnection.getConnection();
         PreparedStatement pstmt = conn.prepareStatement(sql)){

            pstmt.setString(1,username);
            pstmt.setString(2,password);
            pstmt.setString(3, role.toUpperCase());
            pstmt.executeUpdate();
            System.out.println("-> Successful Registration! Waiting for Approval!");
            return true;


        } catch (SQLException e) {
            System.err.println("Failed: "+ e.getMessage() );
            return false;
        }
    }
}
