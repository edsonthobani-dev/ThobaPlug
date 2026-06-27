package com.thobaplug.client;

import com.google.gson.JsonObject;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

public class RegisterController implements IMessageListener {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Label errorLabel;
    @FXML private Label successLabel;
    @FXML private Button registerButton;

    private Client client;

    @FXML
    public void initialize() {
        client = Client.getInstance();
        client.setMessageListener(this);
        if (!client.isConnected()) {
            showError("Cannot connect to server. Is it running?");
            registerButton.setDisable(true);
        }
    }
    
    @FXML
    private void handleRegister() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();
        String confirm  = confirmPasswordField.getText();

        if (username.isEmpty() || password.isEmpty() || confirm.isEmpty()) {
            showError("Please fill in all fields");
            return;
        }
        if (password.length() < 8) {
            showError("Password must be at least 8 characters");
            return;
        }
        if (!password.equals(confirm)) {
            showError("Passwords do not match");
            return;
        }

        registerButton.setDisable(true);
        client.register(username, password);
    }

    @FXML
    private void handleBack() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/resources/LoginScreen.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) usernameField.getScene().getWindow();
            stage.setScene(new Scene(root, 480, 600));
        } catch (Exception e) {
            showError("Could not go back");
            e.printStackTrace();
        }
    }

    @Override
    public void onMessageReceived(JsonObject message) {
        String type = message.get("type").getAsString();
        switch (type) {
            case "REGISTER_SUCCESS":
                Platform.runLater(() -> {
                    successLabel.setText("Account created! Redirecting to login...");
                    errorLabel.setText("");
                    new Thread(() -> {
                        try {
                            Thread.sleep(1500);
                            Platform.runLater(() -> handleBack());
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }).start();
                });
                break;
            case "REGISTER_FAIL":
                String reason = message.get("message").getAsString();
                Platform.runLater(() -> {
                    showError(reason);
                    registerButton.setDisable(false);
                });
                break;
        }
    }

    @Override
    public void onDisconnected() {
        Platform.runLater(() -> showError("Lost connection to server"));
    }

    private void showError(String message) {
        errorLabel.setText(message);
        successLabel.setText("");
    }
}