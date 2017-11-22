package be.ac.ua.dist.systemy.node;

import java.net.InetAddress;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Set;

public interface NodeInterface extends Remote {

    Set<String> getLocalFilesSet() throws RemoteException;

    Set<String> getReplicatedFilesSet() throws RemoteException;

    void addLocalFileList(String fileName) throws RemoteException;

    void addReplicatedFileList(String fileName) throws RemoteException;

    void downloadFile(String sourceFileName, String targetFileName, InetAddress remoteAddress) throws RemoteException;

    void updateNext(InetAddress newAddress, int newHash) throws RemoteException;

    void updatePrev(InetAddress newAddress, int newHash) throws RemoteException;

}