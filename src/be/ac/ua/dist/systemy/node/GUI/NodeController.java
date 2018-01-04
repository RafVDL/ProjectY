package be.ac.ua.dist.systemy.node.GUI;

import be.ac.ua.dist.systemy.Constants;
import be.ac.ua.dist.systemy.namingServer.NamingServerInterface;
import be.ac.ua.dist.systemy.node.Node;
import be.ac.ua.dist.systemy.node.NodeInterface;
import be.ac.ua.dist.systemy.node.NodeMain;
import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.image.Image;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class NodeController {
    private Node node;
    private Stage primaryStage;
    private String selectedFileName;

    @FXML
    private BorderPane borderPane;
    @FXML
    private ListView<String> fileListView;
    @FXML
    private Button deleteLocalBtn;

    public NodeController(Stage primaryStage) {
        this.primaryStage = primaryStage;
    }

    @FXML
    private void initialize() {
        node = NodeMain.getNode();
        if (node == null) {
            showNodeStartError();
            Platform.exit();
        }
        primaryStage.setTitle("Node (" + node.getOwnHash() + ") running in SystemY " + node.getOwnAddress());
        BackgroundImage backgroundImage = new BackgroundImage(new Image(getClass().getResource("Pictures/background.png").toExternalForm(), 650, 500, true, true),
                BackgroundRepeat.REPEAT, BackgroundRepeat.NO_REPEAT, BackgroundPosition.DEFAULT, BackgroundSize.DEFAULT);
        borderPane.setBackground(new Background(backgroundImage));


        fileListView.setItems(node.getAllFilesObservable());
        fileListView.setCellFactory(lv -> new ListCell<String>() {
            @Override
            protected void updateItem(String text, boolean empty) {
                super.updateItem(text, empty);
                if (text == null) {
                    setText(null);
                    setTextFill(null);
                } else {
                    File selectedFile = new File(Constants.DOWNLOADED_FILES_PATH + selectedFileName);
                    Color color = Constants.DEFAULT_COLOR;
                    if (node.getLocalFiles().containsKey(text)) {
                        color = Constants.LOCAL_COLOR;
                    } else if (node.getReplicatedFiles().containsKey(text)) {
                        color = Constants.REPLICATED_COLOR;
                    } else if (selectedFile.isFile()) {
                        color = Constants.DOWNLOADED_COLOR;
                    }
                    setTextFill(color);
                    setText(text);
                }
            }
        });

        // Workaround for the colors not always updating
        node.getAllFilesObservable().addListener((ListChangeListener<String>) c -> {
            Thread thread = new Thread(() -> {
                try {
                    Thread.sleep(500);
                    fileListView.refresh();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            });
            thread.start();
        });

        fileListView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            selectedFileName = newValue;

            // If the file is not downloaded, disable the delete local button
            File selectedFile = new File(Constants.DOWNLOADED_FILES_PATH + selectedFileName);
            if (!selectedFile.isFile()) {
                deleteLocalBtn.setDisable(true);
            }
        });
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
        if (selectedFileName != null) {
            File fileToOpen;

            if (node.getLocalFiles().containsKey(selectedFileName)) {
                fileToOpen = new File(Constants.LOCAL_FILES_PATH + selectedFileName);
            } else if (node.getReplicatedFiles().containsKey(selectedFileName)) {
                fileToOpen = new File(Constants.REPLICATED_FILES_PATH + selectedFileName);
            } else {
                node.downloadAFile(selectedFileName);
                fileToOpen = new File(Constants.DOWNLOADED_FILES_PATH + selectedFileName);
                while (!fileToOpen.isFile()) {
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        System.err.println("Interrupted while waiting file to finish downloading.");
                    }
                }
                deleteLocalBtn.setDisable(true);
            }

            // Contact owner of the file and increase the downloads counter
            try {
                Registry namingServerRegistry = LocateRegistry.getRegistry(node.getNamingServerAddress().getHostAddress(), Constants.RMI_PORT);
                NamingServerInterface namingServerStub = (NamingServerInterface) namingServerRegistry.lookup("NamingServer");
                InetAddress ownerAddress = namingServerStub.getOwner(selectedFileName);

                Registry ownerNodeRegistry = LocateRegistry.getRegistry(ownerAddress.getHostAddress(), Constants.RMI_PORT);
                NodeInterface ownerNodeStub = (NodeInterface) ownerNodeRegistry.lookup("Node");
                ownerNodeStub.increaseDownloads(selectedFileName);

            } catch (RemoteException | UnknownHostException | NotBoundException e) {
                e.printStackTrace();
            }

            // Actually open the file
            try {
                Desktop.getDesktop().open(fileToOpen);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @FXML
    private void handleDelete() {
        if (selectedFileName != null) {
            node.deleteFileFromNetwork(selectedFileName);
            fileListView.getSelectionModel().selectFirst();
        }
    }

    @FXML
    private void handleDeleteLocal() {
        if (selectedFileName != null) {
            node.deleteDownloadedFile(selectedFileName);
        }
    }

//    public class ColoredText{
//        private final String text;
//        private final Color color;
//
//        public ColoredText(String text, Color color) {
//            this.text = text;
//            this.color = color;
//        }
//
//        public String getText() {
//            return text;
//        }
//
//        public Color getColor() {
//            return color;
//        }
//    }
}
