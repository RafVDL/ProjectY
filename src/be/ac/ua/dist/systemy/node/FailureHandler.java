package be.ac.ua.dist.systemy.node;

import be.ac.ua.dist.systemy.Constants;
import be.ac.ua.dist.systemy.namingServer.NamingServerInterface;


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
    private InetAddress prevneighboursIP;
    private InetAddress nextneighboursIP;

    public FailureHandler(int hashFailedNode, Node node) {
        this.hashFailedNode = hashFailedNode;
        this.node = node;
    }

    public void repairFailedNode() throws RemoteException, NotBoundException {
        try {

            Registry registry = LocateRegistry.getRegistry(node.getNamingServerAddress().getHostAddress(), RMI_PORT);
            NamingServerInterface stub = (NamingServerInterface) registry.lookup("NamingServer");

            neighboursHashOfFailedNode = stub.getNeighbours(hashFailedNode);
            prevneighboursIP = stub.getIPNode(neighboursHashOfFailedNode[0]);
            nextneighboursIP = stub.getIPNode(neighboursHashOfFailedNode[1]);
            System.out.println("Neighbours of failed node: " + neighboursHashOfFailedNode[0] + " " + neighboursHashOfFailedNode[1]);

            //Check if only failedNode can be found in network
            if (neighboursHashOfFailedNode[0] == 0  && neighboursHashOfFailedNode[1] == 0) {
                System.out.println("Failed node not found in network.");
                return;
            }
            //Check if only 2 nodes in network and one of them is failed
            if (neighboursHashOfFailedNode[0] == neighboursHashOfFailedNode[1]) {
                node.updatePrev(prevneighboursIP, neighboursHashOfFailedNode[0]);
                node.updateNext(nextneighboursIP, neighboursHashOfFailedNode[1]);
                stub.removeNodeFromNetwork(hashFailedNode);
            }
            else {
                try {
                    //Check if prev neighbour of failed node equals the node where failureHandler is started
                    if (neighboursHashOfFailedNode[0] == node.getOwnHash()) {
                        node.updateNext(nextneighboursIP, neighboursHashOfFailedNode[1]);
                    }
                    else {
                        if(neighboursHashOfFailedNode[0] != 0 && neighboursHashOfFailedNode[1] != 0 && neighboursHashOfFailedNode[0] != hashFailedNode) {
                            Registry prevNodeRegistry = LocateRegistry.getRegistry(prevneighboursIP.getHostAddress(), Constants.RMI_PORT);
                            NodeInterface prevNodeStub = (NodeInterface) prevNodeRegistry.lookup("Node");
                            prevNodeStub.updateNext(nextneighboursIP, neighboursHashOfFailedNode[1]);
                        }
                    }

                } catch (IOException e) {
                    System.out.println("Failed to update previous neighbour of failed node");
                    e.printStackTrace();
                }

                try {
                    //Check if next neighbour of failed node equals the node where failureHandler is started
                    if (neighboursHashOfFailedNode[1] == node.getOwnHash()) {
                        node.updatePrev(prevneighboursIP, neighboursHashOfFailedNode[0]);
                    }
                    else {
                        if(neighboursHashOfFailedNode[0] != 0 && neighboursHashOfFailedNode[1] != 0 && neighboursHashOfFailedNode[1] != hashFailedNode) {
                            Registry nextNodeRegistry = LocateRegistry.getRegistry(nextneighboursIP.getHostAddress(), Constants.RMI_PORT);
                            NodeInterface nextNodeStub = (NodeInterface) nextNodeRegistry.lookup("Node");
                            nextNodeStub.updatePrev(prevneighboursIP, neighboursHashOfFailedNode[0]);
                        }
                    }
                } catch (IOException e) {
                    System.out.println("Failed to update next neighbour of failed node");
                    e.printStackTrace();
                }
                stub.removeNodeFromNetwork(hashFailedNode);
            }
        } catch (IOException | NotBoundException e) {
                e.printStackTrace();
        }
    }
}

