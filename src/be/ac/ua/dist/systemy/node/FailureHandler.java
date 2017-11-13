package be.ac.ua.dist.systemy.node;
import be.ac.ua.dist.systemy.Ports;
import be.ac.ua.dist.systemy.namingServer.*;

import java.io.DataInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.*;
import java.rmi.AlreadyBoundException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.*;
import java.rmi.server.UnicastRemoteObject;

import static be.ac.ua.dist.systemy.Ports.RMI_PORT;

public class FailureHandler {

    private Node node;
    private int hashFailedNode;
    private InetAddress namingServerAddress;
    private int[] neighboursHash;
    private InetAddress[] neighboursIP;

    public FailureHandler(int hashFailedNode, Node node){
        this.hashFailedNode = hashFailedNode;
        this.node = node;
    }

    public void repairFailedNode() throws RemoteException, NotBoundException {
        Registry registry = LocateRegistry.getRegistry(node.getNamingServerAddress().getHostAddress(), RMI_PORT);
        NameserverInterface stub = (NameserverInterface) registry.lookup("NamingServer");

        neighboursHash = stub.getNeighbours(hashFailedNode);
        neighboursIP[0] = stub.getIPNode(neighboursHash[0]);
        neighboursIP[1] = stub.getIPNode(neighboursHash[1]);

        try {
            Socket clientSocket = new Socket();
            clientSocket.setSoLinger(true, 5);
            clientSocket.connect(new InetSocketAddress(neighboursIP[0], Ports.TCP_PORT));
            node.sendTcpCmd(clientSocket, "NEXT_NEIGHBOUR", neighboursHash[0]);
            clientSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            Socket clientSocket = new Socket();
            clientSocket.setSoLinger(true, 5);
            clientSocket.connect(new InetSocketAddress(neighboursIP[1], Ports.TCP_PORT));
            node.sendTcpCmd(clientSocket, "PREV_NEIGHBOUR", neighboursHash[1]);
            clientSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        stub.removeNodeFromNetwork(hashFailedNode);

    }
}

