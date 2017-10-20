package be.ac.ua.dist.systemy.nameserver;

import java.net.InetAddress;
import java.rmi.Remote;
import java.rmi.RemoteException;

public interface Nameserver extends Remote {

    InetAddress getOwner(String fileName) throws RemoteException;

}
