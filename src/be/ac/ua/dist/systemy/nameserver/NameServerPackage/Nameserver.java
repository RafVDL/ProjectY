package be.ac.ua.dist.systemy.nameserver.NameServerPackage;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.rmi.Remote;
import java.rmi.RemoteException;

public interface Nameserver extends Remote {

    InetAddress getOwner(String fileName) throws UnknownHostException, RemoteException;
    void addMeToNetwork(InetAddress IP) throws RemoteException;
    void removeMeFromNetwork(InetAddress IP) throws RemoteException;
    void exportIPadresses() throws RemoteException;
    void printIPadresses() throws RemoteException;

}
