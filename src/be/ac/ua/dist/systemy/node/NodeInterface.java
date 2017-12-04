package be.ac.ua.dist.systemy.node;

import java.net.InetAddress;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Set;

public interface NodeInterface extends Remote {

    InetAddress getPrevAddress() throws RemoteException;

    InetAddress getNextAddress() throws RemoteException;

    Set<FileHandle> getLocalFiles() throws RemoteException;

    Set<FileHandle> getReplicatedFiles() throws RemoteException;

    void addLocalFileList(FileHandle fileHandle) throws RemoteException;

    void addReplicatedFileList(FileHandle fileHandle) throws RemoteException;

    void downloadFile(String sourceFileName, String targetFileName, InetAddress remoteAddress) throws RemoteException;

//    void deleteFileFromNetwork(String path, String fileName) throws RemoteException;

    void updateNext(InetAddress newAddress, int newHash) throws RemoteException;

    void updatePrev(InetAddress newAddress, int newHash) throws RemoteException;

}