package be.ac.ua.dist.systemy.node;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

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

    void runFileAgent(TreeMap<String, Integer> files) throws RemoteException, InterruptedException, NotBoundException;

    void runFailureAgent(int hashFailed, int hashStart, InetAddress currNode) throws RemoteException, InterruptedException, NotBoundException;

    void emptyAllFileList() throws RemoteException;

    void addAllFileList(String file, int value) throws RemoteException;

    String getFileLockRequest() throws RemoteException;

    void setDownloadFileGranted(String download) throws RemoteException;

    String getDownloadFileGranted() throws RemoteException;

    Set getDownloadingFiles() throws RemoteException;

    void setFiles(TreeMap<String, Integer> files) throws RemoteException;

    int calculateHash(String name) throws RemoteException;

    InetAddress getNamingServerAddress() throws RemoteException;

    void replicateFailed(FileHandle fileHandle, InetAddress receiveAddress) throws RemoteException,NotBoundException, UnknownHostException;


}