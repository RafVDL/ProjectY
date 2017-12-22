package be.ac.ua.dist.systemy.node.GUI;

import be.ac.ua.dist.systemy.node.Node;
import be.ac.ua.dist.systemy.node.NodeMain;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class LaunchController {
    @FXML
    private TextField addressField;
    @FXML
    private TextField nameField;

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

        if (enteredAddress.isEmpty()||enteredHostName.isEmpty()) {
            System.out.println("Please fill in all fields");
            return;
        } else {
            try {
                InetAddress address = InetAddress.getByName(enteredAddress);
                NodeMain.setNode(Node.startNode(enteredHostName, address));

            } catch (UnknownHostException e) {
                e.printStackTrace();
                System.err.println("Invalid address (gui)");
                return;
            }
        }
    }
}
