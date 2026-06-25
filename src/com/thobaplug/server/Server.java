/**
 * 
 */
package com.thobaplug.server;


import com.thobaplug.database.DatabaseManager;

/**
 * 
 */

public class Server {
    public static void main(String[] args) {
        DatabaseManager db = DatabaseManager.getInstance();
        db.closeConnection();
    }
}