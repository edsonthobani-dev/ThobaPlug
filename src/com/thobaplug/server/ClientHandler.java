/**
 * 
 */
package com.thobaplug.server;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.thobaplug.database.MessageDAO;
import com.thobaplug.database.UserDAO;
import com.thobaplug.model.Message;
import com.thobaplug.model.User;

import java.io.*;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;
/**
 * 
 */
public class ClientHandler implements Runnable {

    private Socket socket;
    private BufferedReader reader;
    private PrintWriter writer;
    private User currentUser;
    private ConcurrentHashMap<String, ClientHandler> onlineClients;
    private UserDAO userDAO;
    private MessageDAO messageDAO;
    private Gson gson;

    public ClientHandler(Socket socket, ConcurrentHashMap<String, ClientHandler> onlineClients) {
        this.socket        = socket;
        this.onlineClients = onlineClients;
        this.userDAO       = new UserDAO();
        this.messageDAO    = new MessageDAO();
        this.gson          = new Gson();
    }

    @Override
    public void run() {
        try {
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            writer = new PrintWriter(socket.getOutputStream(), true);

            String rawMessage;
            while ((rawMessage = reader.readLine()) != null) {
                handleMessage(rawMessage);
            }
        } catch (IOException e) {
            System.out.println("Client disconnected: " +
                (currentUser != null ? currentUser.getUsername() : "unknown"));
        } finally {
            disconnect();
        }
    }

    private void handleMessage(String raw) {
        try {
            JsonObject json = gson.fromJson(raw, JsonObject.class);
            String type = json.get("type").getAsString();

            switch (type) {
                case "REGISTER":
                {
               handleRegister(json); 
               break;
                }
                case "LOGIN":
                	{
                		handleLogin(json);
                		break;
                	}
                case "BROADCAST":
                	{
                		handleBroadcast(json);
                		break;
                	}
                case "PRIVATE_MSG":
                	{
                		handlePrivateMsg(json);
                		break;
                	}
                case "TYPING":
                	{
                		handleTyping(json);
                		break;
                	}
                case "DISCONNECT":
                	{
                		disconnect();
                		break;
                	}
                default:
                    sendMessage(buildResponse("ERROR", "Unknown message type"));
            }
        } catch (Exception e) {
            sendMessage(buildResponse("ERROR", "Invalid message format"));
        }
    }

    private void handleRegister(JsonObject json) {
        String username = json.get("username").getAsString();
        String password = json.get("password").getAsString();

        if (username == null || username.trim().isEmpty()) {
            sendMessage(buildResponse("REGISTER_FAIL", "Username cannot be empty"));
            return;
        }
        if (password == null || password.length() < 8) {
            sendMessage(buildResponse("REGISTER_FAIL", "Password must be at least 8 characters"));
            return;
        }

        boolean success = userDAO.registerUser(username.trim(), password);
        if (success) {
            sendMessage(buildResponse("REGISTER_SUCCESS", "Account created successfully"));
        } else {
            sendMessage(buildResponse("REGISTER_FAIL", "Username already taken"));
        }
    }

    private void handleLogin(JsonObject json) {
        String username = json.get("username").getAsString();
        String password = json.get("password").getAsString();

        if (onlineClients.containsKey(username)) {
            sendMessage(buildResponse("AUTH_FAIL", "User already logged in"));
            return;
        }

        User user = userDAO.loginUser(username, password);
        if (user != null) {
            currentUser = user;
            onlineClients.put(username, this);

            // Send auth success
            sendMessage(buildResponse("AUTH_SUCCESS", "Welcome to ThobaPlug, " + username + "!"));

            // Send chat history
            sendHistory();

            // Broadcast updated user list to everyone
            broadcastUserList();

            System.out.println(" " + username + " joined. Online: " + onlineClients.size());
        } else {
            sendMessage(buildResponse("AUTH_FAIL", "Invalid username or password"));
        }
    }

    private void handleBroadcast(JsonObject json) {
        if (currentUser == null) {
            sendMessage(buildResponse("ERROR", "Not authenticated"));
            return;
        }
        String content = json.get("content").getAsString();

        // Save to database
        Message msg = new Message(currentUser.getUserr_id(), 0, content, false);
        msg.setSenderUsername(currentUser.getUsername());
        messageDAO.saveMessage(msg);

        // Build broadcast JSON
        JsonObject broadcast = new JsonObject();
        broadcast.addProperty("type", "BROADCAST");
        broadcast.addProperty("sender", currentUser.getUsername());
        broadcast.addProperty("content", content);
        broadcast.addProperty("timestamp", msg.getSent_at().toString());

        // Send to ALL connected clients
        for (ClientHandler client : onlineClients.values()) {
            client.sendMessage(broadcast.toString());
        }
    }

    private void handlePrivateMsg(JsonObject json) {
        if (currentUser == null) {
            sendMessage(buildResponse("ERROR", "Not authenticated"));
            return;
        }
        String recipient = json.get("recipient").getAsString();
        String content   = json.get("content").getAsString();

        ClientHandler recipientHandler = onlineClients.get(recipient);
        if (recipientHandler == null) {
            sendMessage(buildResponse("ERROR", recipient + " is not online"));
            return;
        }

        // Save to database
        Message msg = new Message(currentUser.getUserr_id(),
                recipientHandler.currentUser.getUserr_id(), content, true);
        msg.setSenderUsername(currentUser.getUsername());
        messageDAO.saveMessage(msg);

        // Build private message JSON
        JsonObject pm = new JsonObject();
        pm.addProperty("type", "PRIVATE_MSG");
        pm.addProperty("sender", currentUser.getUsername());
        pm.addProperty("content", content);
        pm.addProperty("timestamp", msg.getSent_at().toString());

        // Send to recipient and back to sender
        recipientHandler.sendMessage(pm.toString());
        sendMessage(pm.toString());
    }

    private void handleTyping(JsonObject json) {
        if (currentUser == null) return;
        String recipient = json.has("recipient") ? json.get("recipient").getAsString() : null;

        JsonObject typingEvent = new JsonObject();
        typingEvent.addProperty("type", "TYPING");
        typingEvent.addProperty("sender", currentUser.getUsername());

        if (recipient != null && onlineClients.containsKey(recipient)) {
            onlineClients.get(recipient).sendMessage(typingEvent.toString());
        } else {
            // Broadcast typing to everyone except sender
            for (ClientHandler client : onlineClients.values()) {
                if (!client.currentUser.getUsername().equals(currentUser.getUsername())) {
                    client.sendMessage(typingEvent.toString());
                }
            }
        }
    }

    private void sendHistory() {
        var history = messageDAO.loadGlobalHistory();
        JsonObject historyMsg = new JsonObject();
        historyMsg.addProperty("type", "HISTORY");
        historyMsg.addProperty("messages", gson.toJson(history));
        sendMessage(historyMsg.toString());
    }

    private void broadcastUserList() {
        JsonObject userList = new JsonObject();
        userList.addProperty("type", "USER_LIST");
        userList.addProperty("users", gson.toJson(onlineClients.keySet()));
        String json = userList.toString();
        for (ClientHandler client : onlineClients.values()) {
            client.sendMessage(json);
        }
    }

    private void disconnect() {
        try {
            if (currentUser != null) {
                onlineClients.remove(currentUser.getUsername());
                System.out.println("✗ " + currentUser.getUsername() +
                    " left. Online: " + onlineClients.size());
                broadcastUserList();
            }
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException e) {
            System.out.println("✗ Error closing socket: " + e.getMessage());
        }
    }

    public void sendMessage(String message) {
        if (writer != null) writer.println(message);
    }

    private String buildResponse(String type, String message) {
        JsonObject json = new JsonObject();
        json.addProperty("type", type);
        json.addProperty("message", message);
        return json.toString();
    }
}