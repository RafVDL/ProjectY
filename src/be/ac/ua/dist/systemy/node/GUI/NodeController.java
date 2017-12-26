package be.ac.ua.dist.systemy.node.GUI;

import be.ac.ua.dist.systemy.node.Node;
import be.ac.ua.dist.systemy.node.NodeMain;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ListView;

public class NodeController {
    private Node node;

    @FXML
    private ListView<String> fileListView;

    @FXML
    private void initialize() {
        node = NodeMain.getNode();

        if (node == null) {
            showNodeStartError();
            Platform.exit();
        }
        System.out.println("Allfiles: " + node.getAllFiles().entrySet().size());
        fileListView.getItems().addAll(node.getAllFiles().keySet());
    }

    private void showNodeStartError() {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText("Node is null");
        alert.setContentText("The node was not initialised when launching the gui. Please restart.");
        alert.showAndWait();
    }

    @FXML
    private void handleOpen(){
        System.out.println("Open button pressed");
    }

    @FXML
    private void handleDelete(){
        System.out.println("Delete button pressed");
    }

    @FXML
    private void handleDeleteLocal(){
        System.out.println("Delete local button pressed");
    }
}
