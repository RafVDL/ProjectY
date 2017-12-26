package be.ac.ua.dist.systemy.node;

import be.ac.ua.dist.systemy.node.GUI.LaunchController;
import javafx.application.Application;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

public class NodeMain extends Application {
    private static Node node;

    public static void main(String[] args) {
        Application.launch();
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        LaunchController launchController = new LaunchController(primaryStage);
        FXMLLoader loader = new FXMLLoader(getClass().getResource("GUI/LaunchView.fxml"));
        loader.setController(launchController);
        primaryStage.setScene(new Scene(loader.load()));
        primaryStage.setTitle("Node launcher");
        primaryStage.show();

        primaryStage.setOnCloseRequest(event -> node.shutdown());
    }

    public static Node getNode() {
        return node;
    }

    public static void setNode(Node node) {
        NodeMain.node = node;
    }
}
