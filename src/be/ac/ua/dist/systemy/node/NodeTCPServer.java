package be.ac.ua.dist.systemy.node;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class NodeTCPServer {

    /**
     * Starts the server -> listens for clients trying to establish a connection. Creates a new thread for each client
     * connecting
     */
    public void startServer() {
        try {
            ServerSocket serverSocket = new ServerSocket(Node.PORT);
            System.out.println("Started server, listening for clients.");
            while (true) {
                Socket clientSocket = serverSocket.accept();
                NodeTCPServerConnection c = new NodeTCPServerConnection(clientSocket);
                c.start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
