package be.ac.ua.dist.systemy.node;

import be.ac.ua.dist.systemy.Ports;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;

public class NodeTCPServer extends Thread {

    private Node node;

    public NodeTCPServer(Node node) {
        this.node = node;
    }

    /**
     * Starts the server -> listens for clients trying to establish a connection. Creates a new thread for each client
     * connecting
     */
    @Override
    public void run() {
        try {
            ServerSocket serverSocket = new ServerSocket(Ports.TCP_PORT);
            serverSocket.setSoTimeout(2000);
            System.out.println("Started NodeTCPServer, listening for other Nodes.");
            while (node.isRunning()) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    NodeTCPServerConnection c = new NodeTCPServerConnection(node, clientSocket);
                    c.start();
                } catch (SocketTimeoutException e) {
                    // nothing
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
