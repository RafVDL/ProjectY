package be.ac.ua.dist.systemy.nameserver;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.rmi.Remote;
import java.rmi.RemoteException;

public interface Nameserver extends Remote {

    InetAddress getOwner(String fileName) throws RemoteException;
    void addMeToNetwork(String computerName, InetAddress IP) throws RemoteException;
    void removeMeFromNetwork(String computerName) throws RemoteException;

}
