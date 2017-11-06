package be.ac.ua.dist.systemy.node;

import java.net.InetAddress;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface NodeInterface extends Remote {
    List<String> getLocalFileList() throws RemoteException;
    List<String> getReplicatedFileList() throws RemoteException;
    List<String> getDownloadedFileList() throws RemoteException;
    void updateNext(InetAddress newAddress, String newName) throws  RemoteException;
    void updatePrev(InetAddress newAddress, String newName) throws  RemoteException;
}