package be.ac.ua.dist.systemy.node;

import be.ac.ua.dist.systemy.Constants;
import be.ac.ua.dist.systemy.namingServer.NamingServerInterface;
import be.ac.ua.dist.systemy.networking.Client;
import be.ac.ua.dist.systemy.networking.NetworkManager;
import be.ac.ua.dist.systemy.networking.packet.UpdateNeighboursPacket;

import java.io.IOException;
import java.net.InetAddress;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

import static be.ac.ua.dist.systemy.Constants.RMI_PORT;

public class FailureHandler {

    private Node node;
    private int hashFailedNode;
    private InetAddress namingServerAddress;
    private int[] neighboursHash;
    private InetAddress[] neighboursIP;

    public FailureHandler(int hashFailedNode, Node node) {
        this.hashFailedNode = hashFailedNode;
        this.node = node;
    }

    public void repairFailedNode() throws RemoteException, NotBoundException {
        Registry registry = LocateRegistry.getRegistry(node.getNamingServerAddress().getHostAddress(), RMI_PORT);
        NamingServerInterface stub = (NamingServerInterface) registry.lookup("NamingServer");

        neighboursHash = stub.getNeighbours(hashFailedNode);
        neighboursIP[0] = stub.getIPNode(neighboursHash[0]);
        neighboursIP[1] = stub.getIPNode(neighboursHash[1]);

        try {
            Client prevClient = NetworkManager.getTCPClient(neighboursIP[0], Constants.TCP_PORT);
            UpdateNeighboursPacket packet = new UpdateNeighboursPacket(-1, neighboursHash[0]);
            prevClient.sendPacket(packet);
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            Client prevClient = NetworkManager.getTCPClient(neighboursIP[1], Constants.TCP_PORT);
            UpdateNeighboursPacket packet = new UpdateNeighboursPacket(neighboursHash[1], -1);
            prevClient.sendPacket(packet);
        } catch (IOException e) {
            e.printStackTrace();
        }

        stub.removeNodeFromNetwork(hashFailedNode);

    }
}

