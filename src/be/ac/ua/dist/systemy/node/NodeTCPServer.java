package be.ac.ua.dist.systemy.node;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class NodeTCPServer extends Thread{

    /**
     * Starts the server -> listens for clients trying to establish a connection. Creates a new thread for each client
     * connecting
     */
    @Override
    public void run() {
        try {
            ServerSocket serverSocket = new ServerSocket(Node.TCP_PORT);
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
