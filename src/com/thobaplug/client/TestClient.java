package com.thobaplug.client;

import com.google.gson.JsonObject;

public class TestClient implements IMessageListener {

    private Client client;

    public TestClient() {
        client = new Client(this);
    }

    public void run() throws InterruptedException {
        if (!client.connect()) return;

        // Test registration
        client.register("Lungelo", "password123");
        Thread.sleep(1000);

        // Test login
        client.login("Lungelo", "password123");
        Thread.sleep(1000);

        // Send a global message
        client.sendBroadcast("Yoh ThobaPlug is live!");
        Thread.sleep(1000);

        client.disconnect();
    }

    @Override
    public void onMessageReceived(JsonObject message) {
        String type = message.get("type").getAsString();
        System.out.println("← Server says [" + type + "]: " + message);
    }

    @Override
    public void onDisconnected() {
        System.out.println("✗ Disconnected from server");
    }

    public static void main(String[] args) throws InterruptedException {
        new TestClient().run();
    }
}