package be.ac.ua.dist.systemy.node.GUI;

import be.ac.ua.dist.systemy.node.Node;
import javafx.fxml.FXML;
import javafx.scene.control.ListView;

public class NodeController {
    private Node node;

    @FXML
    private ListView<String> fileListView;

//    public void initModel(Node node) {
//        if (this.node != null) {
//            // Model is already initialised!
//            System.err.println("Node model is already initialised in the Controller");
//            return;
//        }
//
//        this.node = node;
//    }

    @FXML
    private void initialize() {
        this.node = node;
        fileListView.getItems().addAll(node.getAllFiles().keySet());
    }
}
