package be.ac.ua.dist.systemy.node.GUI;

import be.ac.ua.dist.systemy.node.Node;
import be.ac.ua.dist.systemy.node.NodeMain;
import javafx.fxml.FXML;
import javafx.scene.control.ListView;

public class NodeController {
    private Node node;

    @FXML
    private ListView<String> fileListView;

    @FXML
    private void initialize() {
        node = NodeMain.getNode();

        if (node != null) {
            System.out.println(node.getOwnHash());
        } else {
            System.out.println("Node is null");
        }
//        fileListView.getItems().addAll(node.getAllFiles().keySet());
    }
}
