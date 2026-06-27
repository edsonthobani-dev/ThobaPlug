package com.thobaplug.server;

import com.thobaplug.util.Logger;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {

    private static final int PORT = 5000;
    private static final Logger logger = Logger.getInstance();
    private static ConcurrentHashMap<String, ClientHandler> onlineClients
            = new ConcurrentHashMap<>();

    public static void main(String[] args) {
        ExecutorService threadPool = Executors.newCachedThreadPool();
        logger.info("ThobaPlug server started on port " + PORT);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Server shutting down...");
            threadPool.shutdown();
            logger.close();
        }));

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                Socket clientSocket = serverSocket.accept();
                logger.info("New connection from: " +
                    clientSocket.getInetAddress().getHostAddress());
                ClientHandler handler = new ClientHandler(
                    clientSocket, onlineClients, logger);
                threadPool.execute(handler);
            }
        } catch (IOException e) {
            logger.error("Server error", e);
        }
    }
}