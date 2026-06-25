package com.thobaplug.client;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.*;
import java.net.Socket;

public class Client {

    private static final String SERVER_HOST = "localhost";
    private static final int    SERVER_PORT = 5000;

    private Socket socket;
    private BufferedReader reader;
    private PrintWriter writer;
    private Gson gson;
    private IMessageListener messageListener;
    private String username;



    public Client(IMessageListener listener) {
        this.messageListener = listener;
        this.gson = new Gson();
    }

    // Connect to server
    public boolean connect() {
        try {
            socket = new Socket(SERVER_HOST, SERVER_PORT);
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            writer = new PrintWriter(socket.getOutputStream(), true);
            startListening();
            System.out.println("Connected to ThobaPlug server");
            return true;
        } catch (IOException e) {
            System.out.println("Could not connect to server: " + e.getMessage());
            return false;
        }
    }

    // Listen for messages from server on background thread
    private void startListening() {
        Thread listenerThread = new Thread(() -> {
            try {
                String raw;
                while ((raw = reader.readLine()) != null) {
                    JsonObject json = gson.fromJson(raw, JsonObject.class);
                    if (messageListener != null) {
                        messageListener.onMessageReceived(json);
                    }
                }
            } catch (IOException e) {
                System.out.println("Lost connection to server");
            } finally {
                if (messageListener != null) {
                    messageListener.onDisconnected();
                }
            }
        });
        listenerThread.setDaemon(true);
        listenerThread.start();
    }

    // Send registration request
    public void register(String username, String password) {
        JsonObject json = new JsonObject();
        json.addProperty("type", "REGISTER");
        json.addProperty("username", username);
        json.addProperty("password", password);
        send(json.toString());
    }

    // Send login request
    public void login(String username, String password) {
        this.username = username;
        JsonObject json = new JsonObject();
        json.addProperty("type", "LOGIN");
        json.addProperty("username", username);
        json.addProperty("password", password);
        send(json.toString());
    }

    // Send global message
    public void sendBroadcast(String content) {
        JsonObject json = new JsonObject();
        json.addProperty("type", "BROADCAST");
        json.addProperty("content", content);
        send(json.toString());
    }

    // Send private message
    public void sendPrivateMessage(String recipient, String content) {
        JsonObject json = new JsonObject();
        json.addProperty("type", "PRIVATE_MSG");
        json.addProperty("recipient", recipient);
        json.addProperty("content", content);
        send(json.toString());
    }

    // Send typing event
    public void sendTyping(String recipient) {
        JsonObject json = new JsonObject();
        json.addProperty("type", "TYPING");
        if (recipient != null) {
            json.addProperty("recipient", recipient);
        }
        send(json.toString());
    }

    // Disconnect cleanly
    public void disconnect() {
        try {
            JsonObject json = new JsonObject();
            json.addProperty("type", "DISCONNECT");
            send(json.toString());
            if (socket != null && !socket.isClosed()) socket.close();
            System.out.println("✓ Disconnected from server");
        } catch (IOException e) {
            System.out.println("✗ Error disconnecting: " + e.getMessage());
        }
    }

    private void send(String message) {
        if (writer != null) writer.println(message);
    }

    public String getUsername() { return username; }

    public boolean isConnected() {
        return socket != null && socket.isConnected() && !socket.isClosed();
    }
}