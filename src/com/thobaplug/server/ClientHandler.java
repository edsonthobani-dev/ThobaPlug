/**
 * 
 */
package com.thobaplug.server;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.thobaplug.database.MessageDAO;
import com.thobaplug.database.UserDAO;
import com.thobaplug.model.Message;
import com.thobaplug.model.User;

import java.io.*;
import java.net.Socket;
import java.time.LocalDateTime;
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
    private Gson gson = new GsonBuilder()
    	    .registerTypeAdapter(LocalDateTime.class, 
    	        (com.google.gson.JsonSerializer<java.time.LocalDateTime>) 
    	        (src, typeOfSrc, context) -> new com.google.gson.JsonPrimitive(src.toString()))
    	    .create();

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
        if (raw == null || raw.trim().isEmpty()) return;
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
                case "GET_HISTORY":
                    if (currentUser != null) sendHistory();
                    else sendMessage(buildResponse("ERROR", "Not authenticated"));
                    break;
                case "GET_USER_LIST":
                    if (currentUser != null) sendUserListToMe();
                    else sendMessage(buildResponse("ERROR", "Not authenticated"));
                    break;
                   
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
            ClientHandler oldHandler = onlineClients.get(username);
            if (oldHandler != this) {
                onlineClients.remove(username);
                System.out.println("Replaced stale session for: " + username);
            } else {
                sendMessage(buildResponse("AUTH_FAIL", "Already logged in"));
                return;
            }
        }

        User user = userDAO.loginUser(username, password);
        if (user != null) {
            currentUser = user;
            onlineClients.put(username, this);
            sendMessage(buildResponse("AUTH_SUCCESS",
                        "Welcome to ThobaPlug, " + username + "!"));
            
            // Small delay to let client switch to ChatController before sending data
            new Thread(() -> {
                try {
                    Thread.sleep(1500);
                    sendHistory();
                    broadcastUserList();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }).start();
            
            System.out.println("✓ " + username + " joined. Online: " +
                               onlineClients.size());
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

        Message msg = new Message(currentUser.getUserr_id(), 0, content, false);
        msg.setSenderUsername(currentUser.getUsername());
        
        boolean saved = messageDAO.saveMessage(msg);
        System.out.println("Message save result: " + saved);

        JsonObject broadcast = new JsonObject();
        broadcast.addProperty("type", "BROADCAST");
        broadcast.addProperty("sender", currentUser.getUsername());
        broadcast.addProperty("content", content);
        broadcast.addProperty("timestamp", msg.getSent_at().toString());

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
        try {
            var history = messageDAO.loadGlobalHistory();
            com.google.gson.JsonArray arr = new com.google.gson.JsonArray();
            for (com.thobaplug.model.Message msg : history) {
                com.google.gson.JsonObject obj = new com.google.gson.JsonObject();
                obj.addProperty("senderUsername", msg.getSenderUsername());
                obj.addProperty("content", msg.getContent());
                obj.addProperty("sentAt", msg.getSent_at().toString());
                arr.add(obj);
            }
            JsonObject historyMsg = new JsonObject();
            historyMsg.addProperty("type", "HISTORY");
            historyMsg.addProperty("messages", arr.toString());
            sendMessage(historyMsg.toString());
            System.out.println("✓ History sent: " + history.size() + " messages");
        } catch (Exception e) {
            System.out.println("✗ sendHistory error: " + e.getMessage());
        }
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
    private void sendUserListToMe() {
        JsonObject userList = new JsonObject();
        userList.addProperty("type", "USER_LIST");
        userList.addProperty("users", gson.toJson(onlineClients.keySet()));
        sendMessage(userList.toString());
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