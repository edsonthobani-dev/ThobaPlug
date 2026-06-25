/**
 * 
 */
package com.thobaplug.database;


import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * 
 */

public class DatabaseManager {

    private static final String SERVER   = "DESKTOP-VEHC780\\SQLEXPRESS";
    private static final String DATABASE = "dbThobaPlug";
    private static final String URL      = "jdbc:sqlserver://" + SERVER
                                         + ";databaseName=" + DATABASE
                                         + ";integratedSecurity=true;"
                                         + "trustServerCertificate=true;";

    private static DatabaseManager instance;
    private Connection connection;

    private DatabaseManager() {
        try {
            Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
            connection = DriverManager.getConnection(URL);
            System.out.println("✓ Connected to dbThobaPlug successfully.");
        } catch (ClassNotFoundException e) {
            System.out.println("✗ JDBC Driver not found: " + e.getMessage());
        } catch (SQLException e) {
            System.out.println("✗ Database connection failed: " + e.getMessage());
        }
    }

    public static DatabaseManager getInstance() {
        if (instance == null) {
            instance = new DatabaseManager();
        }
        return instance;
    }

    public Connection getConnection() {
        return connection;
    }

    public void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                System.out.println("✓ Database connection closed.");
            }
        } catch (SQLException e) {
            System.out.println("✗ Error closing connection: " + e.getMessage());
        }
    }
}
