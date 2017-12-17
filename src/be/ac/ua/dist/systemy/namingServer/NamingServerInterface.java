package be.ac.ua.dist.systemy.namingServer;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.server.RemoteServer;
import java.rmi.server.ServerNotActiveException;

public interface NamingServerInterface extends Remote {

    InetAddress getOwner(String fileName) throws UnknownHostException, RemoteException;
//    void addMeToNetwork(String nodeName) throws RemoteException, ServerNotActiveException, UnknownHostException;
    void removeNodeFromNetwork(int hash) throws RemoteException;
    void exportIPadresses() throws RemoteException;
    void printIPadresses() throws RemoteException;
    int[] getNeighbours(int hashNode) throws RemoteException;
    InetAddress getIPNode(int hashNode) throws RemoteException;
    int getHashOfAddress(InetAddress thisAddress) throws RemoteException;

}
