/**
 * 
 */
package com.thobaplug.database;

import com.thobaplug.model.User;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.*;

/**
 * 
 */
public class UserDAO {

    private Connection connection;

    public UserDAO() {
        this.connection = DatabaseManager.getInstance().getConnection();
    }

    // Register a new user
    public boolean registerUser(String username, String password) {
        String sql = "INSERT INTO Userr (username, password_hash) VALUES (?, ?)";
        try {
            // Check duplicate first
            if (usernameExists(username)) {
                System.out.println("Username already taken: " + username);
                return false;
            }
            String hashed = BCrypt.hashpw(password, BCrypt.gensalt(12));
            PreparedStatement stmt = connection.prepareStatement(sql);
            stmt.setString(1, username);
            stmt.setString(2, hashed);
            stmt.executeUpdate();
            System.out.println("User registered: " + username);
            return true;
        } catch (SQLException e) {
            System.out.println("Register failed: " + e.getMessage());
            return false;
        }
    }

    // Login - verify username and password
    public User loginUser(String username, String password) {
        String sql = "SELECT * FROM Userr WHERE username = ?";
        try {
            PreparedStatement stmt = connection.prepareStatement(sql);
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                String storedHash = rs.getString("password_hash");
                if (BCrypt.checkpw(password, storedHash)) {
                    User user = new User(username, storedHash);
                    user.setUserr_id(rs.getInt("userr_id"));
                    System.out.println("Login successful: " + username);
                    return user;
                } else {
                    System.out.println("Wrong password for: " + username);
                    return null;
                }
            } else {
                System.out.println("User not found: " + username);
                return null;
            }
        } catch (SQLException e) {
            System.out.println("Login failed: " + e.getMessage());
            return null;
        }
    }

    // Check if username already exists
    public boolean usernameExists(String username) {
        String sql = "SELECT COUNT(*) FROM Userr WHERE username = ?";
        try {
            PreparedStatement stmt = connection.prepareStatement(sql);
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            System.out.println("Username check failed: " + e.getMessage());
        }
        return false;
    }

    // Get user by username
    public User getUserByUsername(String username) {
        String sql = "SELECT * FROM Userr WHERE username = ?";
        try {
            PreparedStatement stmt = connection.prepareStatement(sql);
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                User user = new User(rs.getString("username"), rs.getString("password_hash"));
                user.setUserr_id(rs.getInt("userr_id"));
                return user;
            }
        } catch (SQLException e) {
            System.out.println("Get user failed: " + e.getMessage());
        }
        return null;
    }
}
