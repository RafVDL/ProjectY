package be.ac.ua.dist.systemy.nameserver.NameServerPackage;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.server.ServerNotActiveException;

public interface Nameserver extends Remote {

    InetAddress getOwner(String fileName) throws UnknownHostException, RemoteException;
    void addMeToNetwork() throws RemoteException, ServerNotActiveException, UnknownHostException;
    void removeMeFromNetwork() throws RemoteException, ServerNotActiveException, UnknownHostException;
    void exportIPadresses() throws RemoteException;
    void printIPadresses() throws RemoteException;

}
