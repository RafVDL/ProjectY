package be.ac.ua.dist.systemy.node;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class NodeMain extends Application {
    private static Node node;

    public static void main(String[] args){
        Application.launch();

//        try {
//            // Get IP and hostname
//            Scanner sc = new Scanner(System.in);
//            System.out.println("(Detected localHostName is: " + InetAddress.getLocalHost() + ")");
//            System.out.print("Enter hostname: ");
//            String hostname = sc.nextLine();
//            if (hostname.isEmpty()) {
//                hostname = InetAddress.getLocalHost().getHostName();
//            }
//            System.out.println("(Detected localHostAddress is: " + InetAddress.getLocalHost() + ")");
//            System.out.print("Enter IP: ");
//            String ip = sc.nextLine();
//            if (ip.isEmpty()) {
//                ip = InetAddress.getLocalHost().getHostAddress();
//            }
//
//
//            // Create Node object and initialize
//            Node node = Node.startNode(hostname, InetAddress.getByName(ip));
//        } catch (UnknownHostException e) {
//            e.printStackTrace();
//            System.err.println("Unknown host detected, failed to start the Node.");
//            return;
//        }


    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("GUI/LaunchView.fxml"));
        primaryStage.setScene(new Scene(root));
        primaryStage.setTitle("Node launcher");
        primaryStage.show();
    }

    public static Node getNode(){
        return node;
    }

    public static void setNode(Node node){
        NodeMain.node = node;
    }
}
