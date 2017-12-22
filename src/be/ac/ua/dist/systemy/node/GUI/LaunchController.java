package be.ac.ua.dist.systemy.node.GUI;

import be.ac.ua.dist.systemy.node.Node;
import be.ac.ua.dist.systemy.node.NodeMain;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.UnknownHostException;

public class LaunchController {
    private Stage primaryStage;
    @FXML
    private TextField addressField;
    @FXML
    private TextField nameField;

    public LaunchController(Stage primaryStage) {
        this.primaryStage = primaryStage;
    }

    @FXML
    private void initialize() {

    }

    /**
     * Exits the application if the cancel button is pressed.
     */
    @FXML
    private void handleCancel() {
        Platform.exit();
    }

    @FXML
    private void handleLaunch() {
        String enteredAddress = addressField.getText();
        String enteredHostName = nameField.getText();

        if (enteredAddress.isEmpty() || enteredHostName.isEmpty()) {
            showJoinError();
        } else {
            try {
                InetAddress address = InetAddress.getByName(enteredAddress);
                if (NetworkInterface.getByInetAddress(address) == null) {
                    showIPNotLocal();
                    return;
                }
                //TODO: ENABLE THIS WHEN DONE TESTING
                NodeMain.setNode(Node.startNode(enteredHostName, address));
                startMainGUI();

            } catch (UnknownHostException e) {
                e.printStackTrace();
                showIPInvalid();
            } catch (IOException e) {
                e.printStackTrace();
                showIPNotLocal();
            }
        }
    }

    private void showJoinError() {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText("Incorrect credentials");
        alert.setContentText("Please fill in all fields");
        alert.showAndWait();
        addressField.clear();
        nameField.clear();
        addressField.requestFocus();
    }

    private void showIPInvalid() {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText("Incorrect address");
        alert.setContentText("Please fill in a valid network address");
        alert.showAndWait();
        addressField.clear();
        addressField.requestFocus();
    }

    private void showIPNotLocal() {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText("Address is not local");
        alert.setContentText("The provided network address could not be resolved as a local address");
        alert.showAndWait();
        addressField.clear();
        addressField.requestFocus();
    }

    /**
     * Starts the main node gui window
     */
    private void startMainGUI() {
        try {
            NodeController nodeController = new NodeController();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("NodeView.fxml"));
            loader.setController(nodeController);
            primaryStage.setScene(new Scene(loader.load()));
            primaryStage.setTitle("Node active");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
