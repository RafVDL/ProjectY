package be.ac.ua.dist.systemy.node;

import java.net.InetAddress;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Map;

public interface NodeInterface extends Remote {

    int getOwnHash() throws RemoteException;

    int getPrevHash() throws RemoteException;

    InetAddress getPrevAddress() throws RemoteException;

    InetAddress getNextAddress() throws RemoteException;

    Map<String, FileHandle> getLocalFiles() throws RemoteException;

    Map<String, FileHandle> getReplicatedFiles() throws RemoteException;

    void addLocalFileList(FileHandle fileHandle) throws RemoteException;

    void addReplicatedFileList(FileHandle fileHandle) throws RemoteException;

    void addOwnerFileList(FileHandle fileHandle) throws RemoteException;

    void removeLocalFile(FileHandle fileHandle) throws RemoteException;

    void removeReplicatedFile(FileHandle fileHandle) throws RemoteException;

    void removeOwnerFile(FileHandle fileHandle) throws RemoteException;

    void downloadFile(String sourceFileName, String targetFileName, InetAddress remoteAddress) throws RemoteException;

    void deleteFileFromNode(FileHandle fileHandle) throws RemoteException;

    void addToAvailableNodes(String fileName, int hashToAdd) throws RemoteException;

    void removeFromAvailableNodes(String fileName, int hashToRemove) throws RemoteException;

    void updateNext(InetAddress newAddress, int newHash) throws RemoteException;

    void updatePrev(InetAddress newAddress, int newHash) throws RemoteException;

}