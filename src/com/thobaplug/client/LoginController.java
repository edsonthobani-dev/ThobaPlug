package com.thobaplug.client;

import com.google.gson.JsonObject;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

public class LoginController implements IMessageListener {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;
    @FXML private Button loginButton;
    private long sceneLoadTime = 0;
    private boolean loginInProgress = false;
    static boolean screenSwitched = false;
    

    private Client client;

    @FXML
    public void initialize() {
        client = Client.getInstance();
        client.setMessageListener(this);
        if (!client.isConnected()) {
            showError("Cannot connect to server. Is it running?");
            loginButton.setDisable(true);
        }
    }
    
    @FXML
    private void handleLogin() {
        if (loginInProgress) return;
        
        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            showError("Please fill in all fields");
            return;
        }

        loginInProgress = true;
        loginButton.setDisable(true);
        client.login(username, password);
    }

    @FXML
    private void handleRegister() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/resources/RegisterScreen.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) usernameField.getScene().getWindow();
            sceneLoadTime = System.currentTimeMillis();
            stage.setScene(new Scene(root, 480, 600));
        } catch (Exception e) {
            showError("Could not open register screen");
            e.printStackTrace();
        }
    }

   

    @Override
    public void onMessageReceived(JsonObject message) {
        String type = message.get("type").getAsString();
        System.out.println("LoginController received: " + type);
      
        switch (type) {
            case "AUTH_SUCCESS":
                if (!screenSwitched) {
                    screenSwitched = true;
                    Platform.runLater(() -> openChatScreen());
                }
                break;
            case "AUTH_FAIL":
                String reason = message.get("message").getAsString();
                Platform.runLater(() -> {
                    showError(reason);
                    loginButton.setDisable(false);
                    loginInProgress = false;
                    screenSwitched = false;
                });
                break;
        }
    }

    @Override
    public void onDisconnected() {
        Platform.runLater(() -> showError("Lost connection to server"));
    }

    private void openChatScreen() {
        Platform.runLater(() -> {
            try {
                FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/resources/ChatScreen.fxml"));
                Parent root = loader.load();
                ChatController controller = loader.getController();

                client.setMessageListener(null);

                final long sceneLoadTime = System.currentTimeMillis();
                Scene chatScene = new Scene(root, 800, 600);
                chatScene.addEventFilter(javafx.scene.input.MouseEvent.MOUSE_RELEASED, event -> {
                    if (System.currentTimeMillis() - sceneLoadTime < 500) {
                        System.out.println("Blocked bleed mouse event");
                        event.consume();
                    }
                });

                Stage stage = (Stage) usernameField.getScene().getWindow();
                stage.setScene(chatScene);
                stage.setTitle("ThobaPlug - " + client.getUsername());

                client.setMessageListener(controller);
                controller.initChat(client, client.getUsername());

            } catch (Exception e) {
                System.out.println("SCREEN SWITCH ERROR: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }
    private void showError(String message) {
        errorLabel.setText(message);
    }
}