package be.ac.ua.dist.systemy.node.GUI;

import be.ac.ua.dist.systemy.Constants;
import be.ac.ua.dist.systemy.node.Node;
import be.ac.ua.dist.systemy.node.NodeMain;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ListView;

import java.awt.*;
import java.io.File;
import java.io.IOException;

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
            File fileToOpen;

            if(node.getLocalFiles().containsKey(fileName)){
                fileToOpen = new File(Constants.LOCAL_FILES_PATH+fileName);
            }else if (node.getReplicatedFiles().containsKey(fileName)){
                fileToOpen = new File(Constants.REPLICATED_FILES_PATH+fileName);
            }else {
            node.downloadAFile(fileName);
                fileToOpen = new File(Constants.DOWNLOADED_FILES_PATH+fileName);
                while (!fileToOpen.isFile()) {
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        System.err.println("Interrupted while waiting file to finish downloading.");
                    }
                }
            }

            try {
                Desktop.getDesktop().open(fileToOpen);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @FXML
    private void handleDelete() {
        String fileName = fileListView.getSelectionModel().getSelectedItem();
        System.out.println("Delete button pressed on: " + fileName);
        if (fileName != null) {
//            node.dosomething
            fileListView.getSelectionModel().selectFirst();
        }
    }

    @FXML
    private void handleDeleteLocal() {
        String fileName = fileListView.getSelectionModel().getSelectedItem();
        System.out.println("Delete local button pressed on: " + fileName);
        if (fileName != null) {
//            node.dosomething
            fileListView.getSelectionModel().selectFirst();
        }
    }

    @FXML
    private void handlePrintObservable() {
        System.out.println("Contents of the observable: " + node.getAllFilesObservable());
        System.out.println("Contents of the map: " + node.getAllFiles());
    }
}
