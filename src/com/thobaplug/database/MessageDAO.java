/**
 * 
 */
package com.thobaplug.database;


import com.thobaplug.model.Message;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 
 */

public class MessageDAO {

    private Connection connection;

    public MessageDAO() {
        this.connection = DatabaseManager.getInstance().getConnection();
    }

    // Save a message to the database
    public boolean saveMessage(Message message) {
        String sql = "INSERT INTO Messagee (sender_id, recipient_id, content, is_private) VALUES (?, ?, ?, ?)";
        try {
            PreparedStatement stmt = connection.prepareStatement(sql);
            stmt.setInt(1, message.getSender_id());
            if (message.getRecipient_id() == 0) {
                stmt.setNull(2, Types.INTEGER);
            } else {
                stmt.setInt(2, message.getRecipient_id());
            }
            stmt.setString(3, message.getContent());
            stmt.setBoolean(4, message.isIs_private());
            stmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.out.println("Save message failed: " + e.getMessage());
            return false;
        }
    }

    // Load last 50 global messages
    public List<Message> loadGlobalHistory() {
        List<Message> messages = new ArrayList<>();
        String sql = "SELECT TOP 50 m.message_id, m.sender_id, m.content, m.sent_at, u.username " +
                     "FROM Messagee m "
                     +"JOIN Userr u"
                     +"ON m.sender_id = u.userr_id " +
                     "WHERE m.is_private = 0 "
                     +"ORDER BY m.sent_at ASC";
        try {
            PreparedStatement stmt = connection.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                Message msg = new Message(
                    rs.getInt("sender_id"), 0,
                    rs.getString("content"), false
                );
                msg.setMessage_id(rs.getInt("message_id"));
                msg.setSenderUsername(rs.getString("username"));
                msg.setSent_at(rs.getTimestamp("sent_at").toLocalDateTime());
                messages.add(msg);
            }
        } catch (SQLException e) {
            System.out.println("Load global history failed: " + e.getMessage());
        }
        return messages;
    }

    // Load private message history between two users
    public List<Message> loadPrivateHistory(int userId1, int userId2) {
        List<Message> messages = new ArrayList<>();
        String sql = "SELECT TOP 50 m.message_id, m.sender_id, m.content, m.sent_at, u.username " +
                     "FROM Messagee m JOIN Userr u ON m.sender_id = u.userr_id " +
                     "WHERE m.is_private = 1 " +
                     "AND ((m.sender_id = ? AND m.recipient_id = ?) " +
                     "OR (m.sender_id = ? AND m.recipient_id = ?)) " +
                     "ORDER BY m.sent_at ASC";
        try {
            PreparedStatement stmt = connection.prepareStatement(sql);
            stmt.setInt(1, userId1);
            stmt.setInt(2, userId2);
            stmt.setInt(3, userId2);
            stmt.setInt(4, userId1);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                Message msg = new Message(
                    rs.getInt("sender_id"), userId2,
                    rs.getString("content"), true
                );
                msg.setMessage_id(rs.getInt("message_id"));
                msg.setSenderUsername(rs.getString("username"));
                msg.setSent_at(rs.getTimestamp("sent_at").toLocalDateTime());
                messages.add(msg);
            }
        } catch (SQLException e) {
            System.out.println("Load private history failed: " + e.getMessage());
        }
        return messages;
    }
}
