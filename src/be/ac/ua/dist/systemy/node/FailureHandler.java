package be.ac.ua.dist.systemy.node;

import be.ac.ua.dist.systemy.Constants;
import be.ac.ua.dist.systemy.namingServer.NamingServerInterface;
import be.ac.ua.dist.systemy.networking.Client;
import be.ac.ua.dist.systemy.networking.Communications;
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
    private int[] neighboursHashOfFailedNode;
    private InetAddress[] neighboursIP;

    public FailureHandler(int hashFailedNode, Node node) {
        this.hashFailedNode = hashFailedNode;
        this.node = node;
    }

    public void repairFailedNode() throws RemoteException, NotBoundException {
        try {
            Registry registry = LocateRegistry.getRegistry(node.getNamingServerAddress().getHostAddress(), RMI_PORT);
            NamingServerInterface stub = (NamingServerInterface) registry.lookup("NamingServer");

            neighboursHashOfFailedNode = stub.getNeighbours(hashFailedNode);
            neighboursIP[0] = stub.getIPNode(neighboursHashOfFailedNode[0]);
            neighboursIP[1] = stub.getIPNode(neighboursHashOfFailedNode[1]);

            try {
                Client prevClient = Communications.getTCPClient(neighboursIP[0], Constants.TCP_PORT);
                //Update previous neighbour of failed node so that his next neighbour will be next neighbour of failed node
                UpdateNeighboursPacket packet = new UpdateNeighboursPacket(-1, neighboursHashOfFailedNode[1]);
                prevClient.sendPacket(packet);
            } catch (IOException e) {
                System.out.println("Failed to update previous neighbour of failed node");
                e.printStackTrace();
            }

            try {
                Client nextClient = Communications.getTCPClient(neighboursIP[1], Constants.TCP_PORT);
                //Update next neighbour of failed node so that his prev neighbour will be prev neighbour of failed node
                UpdateNeighboursPacket packet = new UpdateNeighboursPacket(neighboursHashOfFailedNode[0], -1);
                nextClient.sendPacket(packet);
            } catch (IOException e) {
                System.out.println("Failed to update next neighbour of failed node");
                e.printStackTrace();
            }
            stub.removeNodeFromNetwork(hashFailedNode);
        } catch (IOException | NotBoundException e) {
            e.printStackTrace();
        }
    }
}

