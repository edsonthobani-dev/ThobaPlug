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

    private Client client;

    @FXML
    public void initialize() {
        client = new Client(this);
        if (!client.connect()) {
            showError("Cannot connect to server. Is it running?");
            loginButton.setDisable(true);
        }
    }

    @FXML
    private void handleLogin() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            showError("Please fill in all fields");
            return;
        }

        loginButton.setDisable(true);
        client.login(username, password);
    }

    @FXML
    private void handleRegister() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/resources/RegisterScreen.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) usernameField.getScene().getWindow();
            stage.setScene(new Scene(root, 480, 600));
        } catch (Exception e) {
            showError("Could not open register screen");
            e.printStackTrace();
        }
    }

    @Override
    public void onMessageReceived(JsonObject message) {
        String type = message.get("type").getAsString();
        switch (type) {
            case "AUTH_SUCCESS":
                Platform.runLater(() -> openChatScreen());
                break;
            case "AUTH_FAIL":
                String reason = message.get("message").getAsString();
                Platform.runLater(() -> {
                    showError(reason);
                    loginButton.setDisable(false);
                });
                break;
        }
    }

    @Override
    public void onDisconnected() {
        Platform.runLater(() -> showError("Lost connection to server"));
    }

    private void openChatScreen() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/resources/ChatScreen.fxml"));
            Parent root = loader.load();
            ChatController controller = loader.getController();
            controller.setClient(client, client.getUsername());
            Stage stage = (Stage) usernameField.getScene().getWindow();
            stage.setScene(new Scene(root, 800, 600));
            stage.setTitle("ThobaPlug - " + client.getUsername());
        } catch (Exception e) {
            showError("Could not open chat screen");
            e.printStackTrace();
        }
    }

    private void showError(String message) {
        errorLabel.setText(message);
    }
}