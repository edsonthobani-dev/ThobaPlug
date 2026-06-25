package com.thobaplug.server;

import com.thobaplug.database.UserDAO;
import com.thobaplug.model.User;

public class Server {
    public static void main(String[] args) {

        UserDAO userDAO = new UserDAO();

        // Test registration
        userDAO.registerUser("Thobani", "password123");
        userDAO.registerUser("Thobani", "password123"); // should say duplicate

        // Test login
        User user = userDAO.loginUser("Thobani", "password123");
        if (user != null) {
            System.out.println("Logged in as: " + user);
        }

        // Test wrong password
        User fail = userDAO.loginUser("Thobani", "wrongpassword");
        if (fail == null) {
            System.out.println("✓ Wrong password correctly rejected");
        }
    }
}