package com.thobaplug.client;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        // Create and connect client once at app startup
        Client client = Client.getInstance();
        client.connect();

        Parent root = FXMLLoader.load(
        			getClass().getResource("/resources/LoginScreen.fxml"));
        Scene scene = new Scene(root, 480, 600);
        primaryStage.setTitle("ThobaPlug");
        primaryStage.setScene(scene);
        primaryStage.setResizable(false);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}