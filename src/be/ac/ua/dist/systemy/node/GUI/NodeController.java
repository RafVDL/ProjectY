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

        fileListView.setItems(node.getAllFilesObservable());
    }

    private void showNodeStartError() {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText("Node is null");
        alert.setContentText("The node was not initialised when launching the gui. Please restart.");
        alert.showAndWait();
    }

    @FXML
    private void handleOpen() {
        String fileName = fileListView.getSelectionModel().getSelectedItem();
        System.out.println("Open button pressed on: " + fileName);
        if (fileName != null) {
            node.openFile(fileName);
        }
    }

    @FXML
    private void handleDelete() {
        String fileName = fileListView.getSelectionModel().getSelectedItem();
        System.out.println("Delete button pressed on: " + fileName);
        if (fileName != null) {
            node.deleteFileFromNetwork(fileName);
            fileListView.getSelectionModel().selectFirst();
        }
    }

    @FXML
    private void handleDeleteLocal() {
        String fileName = fileListView.getSelectionModel().getSelectedItem();
        System.out.println("Delete local button pressed on: " + fileName);
        if (fileName != null) {
            node.deleteDownloadedFile(fileName);
        }
    }

    @FXML
    private void handlePrintObservable() {
        System.out.println("Contents of the observable: " + node.getAllFilesObservable());
        System.out.println("Contents of the map: " + node.getAllFiles());
    }
}
