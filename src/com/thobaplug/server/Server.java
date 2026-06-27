package com.thobaplug.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {

    private static final int PORT = 5000;
    private static ConcurrentHashMap<String, ClientHandler> onlineClients
            = new ConcurrentHashMap<>();

    public static void main(String[] args) {
        ExecutorService threadPool = Executors.newCachedThreadPool();
        System.out.println("✓ ThobaPlug server started on port " + PORT);

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("→ New connection from: " +
                    clientSocket.getInetAddress().getHostAddress());
                ClientHandler handler = new ClientHandler(clientSocket, onlineClients);
                threadPool.execute(handler);
            }
        } catch (IOException e) {
            System.out.println("✗ Server error: " + e.getMessage());
        } finally {
            threadPool.shutdown();
        }
    }
    
}