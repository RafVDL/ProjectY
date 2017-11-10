package be.ac.ua.dist.systemy.namingServer;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.server.RemoteServer;
import java.rmi.server.ServerNotActiveException;

public interface NameserverInterface extends Remote {

    InetAddress getOwner(String fileName) throws UnknownHostException, RemoteException;
//    void addMeToNetwork(String nodeName) throws RemoteException, ServerNotActiveException, UnknownHostException;
//    void removeMeFromNetwork(String nodeName) throws RemoteException, ServerNotActiveException, UnknownHostException;
    void exportIPadresses() throws RemoteException;
    void printIPadresses() throws RemoteException;
    int[] getNeighbours(int hashNode) throws RemoteException;

}
