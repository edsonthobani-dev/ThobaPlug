/**
 * 
 */
package com.thobaplug.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.time.LocalDateTime;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ChatController implements IMessageListener {

    @FXML private VBox messagesBox;
    @FXML private ScrollPane scrollPane;
    @FXML private TextField messageField;
    @FXML private Button sendButton;
    @FXML private ListView<String> onlineUsersList;
    @FXML private Label typingLabel;
    @FXML private Label onlineCountLabel;
    @FXML private Label usernameLabel;
    @FXML private Button logoutButton;
    private volatile long chatLoadedAt = 0;
    
    
    private Client client;
    private String username;
    private Gson gson;
    private ScheduledExecutorService typingClearScheduler;

    public void initChat(Client client, String username) {
       
        this.client   = client;
        this.username = username;
        usernameLabel.setText(username);
        this.gson = new GsonBuilder()
        	    .registerTypeAdapter(LocalDateTime.class,
        	        (com.google.gson.JsonDeserializer<LocalDateTime>)
        	        (json, type, context) -> LocalDateTime.parse(json.getAsString()))
        	    .create();
        // Set ready after short delay to prevent logout bleed
        new Thread(() -> {
            try {
                Thread.sleep(1300);
                chatLoadedAt = System.currentTimeMillis();
                System.out.println("Chat ready - logout enabled");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }
    @FXML
    public void initialize() {
       //
    }
    @FXML
    private void handleSend() {
        String content = messageField.getText().trim();
        if (content.isEmpty()) return;
        if (content.length() > 1000) {
            typingLabel.setText("Message too long (max 1000 characters)");
            return;
        }
        if (!client.isConnected()) {
            typingLabel.setText("Not connected to server");
            return;
        }
        client.sendBroadcast(content);
        messageField.clear();
    }

    @FXML
    private void handleKeyPress(KeyEvent event) {
        if (event.getCode() == KeyCode.ENTER) {
            handleSend();
        }
    }

    @FXML
    private void handleTyping() {
        client.sendTyping(null);
    }
    @FXML
    private void handleLogout() {
        long now = System.currentTimeMillis();
        long diff = now - chatLoadedAt;
        if (chatLoadedAt == 0 || diff < 2000) {
            System.out.println("Logout blocked");
            return;
        }
        client.setMessageListener(null);
        LoginController.screenSwitched = false;
        client.disconnect();
        Client.resetInstance();

        try {
            // Create fresh client for next login
            Client newClient = Client.getInstance();
            newClient.connect();

            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/resources/LoginScreen.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) messageField.getScene().getWindow();
            stage.setScene(new Scene(root, 480, 600));
            stage.setTitle("ThobaPlug");
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onMessageReceived(JsonObject message) {
        String type = message.get("type").getAsString();
        switch (type) {
            case "BROADCAST":
                String sender    = message.get("sender").getAsString();
                String content   = message.get("content").getAsString();
                String timestamp = message.get("timestamp").getAsString();
                Platform.runLater(() -> addMessage(sender, content, timestamp,
                        sender.equals(username)));
                break;

            case "USER_LIST":
                String usersJson = message.get("users").getAsString();
                JsonArray users  = gson.fromJson(usersJson, JsonArray.class);
                Platform.runLater(() -> updateUserList(users));
                break;

            case "HISTORY":
                String histJson = message.get("messages").getAsString();
                
                JsonArray history = gson.fromJson(histJson, JsonArray.class);
                Platform.runLater(() -> loadHistory(history));
                break;

            case "TYPING":
                String typingSender = message.get("sender").getAsString();
                Platform.runLater(() -> showTyping(typingSender));
                break;

            case "PRIVATE_MSG":
                String pmSender  = message.get("sender").getAsString();
                String pmContent = message.get("content").getAsString();
                String pmTime    = message.get("timestamp").getAsString();
                Platform.runLater(() -> addPrivateMessage(pmSender, pmContent, pmTime));
                break;
        }
    }

    @Override
    public void onDisconnected() {
        // Only show error if we didn't intentionally logout
        if (chatLoadedAt != 0) {
            Platform.runLater(() -> {
                addSystemMessage("Disconnected from server");
                sendButton.setDisable(true);
                messageField.setDisable(true);
            });
        }
    }

    private void addMessage(String sender, String content,
                            String timestamp, boolean isOwn) {
        HBox wrapper = new HBox();
        wrapper.setMaxWidth(Double.MAX_VALUE);

        VBox bubble = new VBox(3);
        bubble.setMaxWidth(500);
        bubble.setPadding(new Insets(8, 14, 8, 14));

        if (isOwn) {
            wrapper.setAlignment(Pos.CENTER_RIGHT);
            bubble.setStyle("-fx-background-color: #1D4ED8;" +
                            "-fx-background-radius: 16 16 4 16;");
        } else {
            wrapper.setAlignment(Pos.CENTER_LEFT);
            bubble.setStyle("-fx-background-color: #1E293B;" +
                            "-fx-background-radius: 16 16 16 4;");
            Label nameLabel = new Label(sender);
            nameLabel.setStyle("-fx-text-fill: #60A5FA;" +
                               "-fx-font-size: 11px;" +
                               "-fx-font-weight: bold;");
            bubble.getChildren().add(nameLabel);
        }

        Label msgLabel = new Label(content);
        msgLabel.setWrapText(true);
        msgLabel.setStyle("-fx-text-fill: white; -fx-font-size: 14px;");

        String time = timestamp.length() > 16
                      ? timestamp.substring(11, 16) : timestamp;
        Label timeLabel = new Label(time);
        timeLabel.setStyle("-fx-text-fill: #94A3B8; -fx-font-size: 10px;");

        bubble.getChildren().addAll(msgLabel, timeLabel);
        wrapper.getChildren().add(bubble);
        messagesBox.getChildren().add(wrapper);
        scrollToBottom();
    }

    private void addPrivateMessage(String sender, String content, String timestamp) {
        HBox wrapper = new HBox();
        wrapper.setMaxWidth(Double.MAX_VALUE);
        wrapper.setAlignment(sender.equals(username)
                             ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);

        VBox bubble = new VBox(3);
        bubble.setMaxWidth(500);
        bubble.setPadding(new Insets(8, 14, 8, 14));
        bubble.setStyle("-fx-background-color: #4C1D95;" +
                        "-fx-background-radius: 16;");

        Label dmTag = new Label("DM — " + sender);
        dmTag.setStyle("-fx-text-fill: #C4B5FD;" +
                       "-fx-font-size: 11px;" +
                       "-fx-font-weight: bold;");

        Label msgLabel = new Label(content);
        msgLabel.setWrapText(true);
        msgLabel.setStyle("-fx-text-fill: white; -fx-font-size: 14px;");

        bubble.getChildren().addAll(dmTag, msgLabel);
        wrapper.getChildren().add(bubble);
        messagesBox.getChildren().add(wrapper);
        scrollToBottom();
    }

    private void addSystemMessage(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-text-fill: #475569;" +
                       "-fx-font-size: 12px;" +
                       "-fx-font-style: italic;");
        HBox wrapper = new HBox(label);
        wrapper.setAlignment(Pos.CENTER);
        messagesBox.getChildren().add(wrapper);
        scrollToBottom();
    }

    private void loadHistory(JsonArray history) {
        System.out.println("loadHistory() called with " + history.size() + " messages");
        if (history.size() == 0) {
            addSystemMessage("No previous messages. Start the conversation!");
            return;
        }
        addSystemMessage("— Chat history —");
        for (JsonElement el : history) {
            JsonObject msg = el.getAsJsonObject();
            System.out.println("Displaying: " + msg.keySet());
            addMessage(
                msg.get("senderUsername").getAsString(),
                msg.get("content").getAsString(),
                msg.get("sentAt").getAsString(),
                msg.get("senderUsername").getAsString().equals(username)
            );
        }
        addSystemMessage("— Live —");
    }

    private void updateUserList(JsonArray users) {
        onlineUsersList.getItems().clear();
        for (JsonElement el : users) {
            String user = el.getAsString();
            onlineUsersList.getItems().add(user);
        }
        onlineCountLabel.setText(users.size() + " online");

        onlineUsersList.setCellFactory(lv -> new ListCell<String>() {
            @Override
            protected void updateItem(String user, boolean empty) {
                super.updateItem(user, empty);
                if (empty || user == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText("🟢  " + user);
                    setStyle("-fx-text-fill: #E2E8F0;" +
                             "-fx-font-size: 13px;" +
                             "-fx-background-color: transparent;" +
                             "-fx-padding: 6 12 6 12;");
                }
            }
        });
    }

    private void showTyping(String sender) {
        typingLabel.setText(sender + " is typing...");
        if (typingClearScheduler != null) typingClearScheduler.shutdownNow();
        typingClearScheduler = Executors.newSingleThreadScheduledExecutor();
        typingClearScheduler.schedule(
            () -> Platform.runLater(() -> typingLabel.setText("")),
            3, TimeUnit.SECONDS
        );
    }

    private void scrollToBottom() {
        scrollPane.layout();
        scrollPane.setVvalue(1.0);
    }
}