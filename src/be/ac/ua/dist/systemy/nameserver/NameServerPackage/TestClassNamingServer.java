package be.ac.ua.dist.systemy.nameserver.NameServerPackage;
import be.ac.ua.dist.systemy.node.Node;
import be.ac.ua.dist.systemy.node.NodeTCPServer;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.ServerNotActiveException;


public class TestClassNamingServer {
    public TestClassNamingServer(){
        try {
            Node testNode1 = new Node("Computer 1", InetAddress.getByName("192.168.137.5"));
            Node testNode2 = new Node("Computer 2", InetAddress.getByName("192.168.137.6"));
            Node testNode3 = new Node("Computer 3", InetAddress.getByName("192.168.137.7"));
            Node testNode4 = new Node("Computer 4", InetAddress.getByName("192.168.137.8"));
            Node testNode5 = new Node("Computer 5", InetAddress.getByName("192.168.137.9"));

        } catch (UnknownHostException e) {
            e.printStackTrace();
        }

    }


    public static void main(String[] args) throws RemoteException, NotBoundException, UnknownHostException, ServerNotActiveException {
        Registry registry = LocateRegistry.getRegistry("192.168.137.1", 3733);
        Nameserver stub = (Nameserver) registry.lookup("be.ac.ua.dist.systemy.nameserver.NameServerPackage.NamingServer");

        stub.addMeToNetwork();
        stub.printIPadresses();
        stub.getOwner("test.txt");
        stub.exportIPadresses();
        stub.removeMeFromNetwork();
        stub.printIPadresses();


        NodeTCPServer tcpServer = new NodeTCPServer();
        tcpServer.startServer();
    }

}
