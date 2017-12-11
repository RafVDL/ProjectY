package be.ac.ua.dist.systemy.node;

import java.net.InetAddress;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Set;
import java.util.TreeMap;

public interface NodeInterface extends Remote {

    InetAddress getPrevAddress() throws RemoteException;

//    InetAddress getNextAddress() throws RemoteException;

    Set getLocalFiles() throws RemoteException;

//    Set getReplicatedFiles() throws RemoteException;

    void addLocalFileList(String fileName) throws RemoteException;

    void addReplicatedFileList(String fileName) throws RemoteException;

    void downloadFile(String sourceFileName, String targetFileName, InetAddress remoteAddress) throws RemoteException;

//    void deleteFileFromNetwork(String path, String fileName) throws RemoteException;

    void updateNext(InetAddress newAddress, int newHash) throws RemoteException;

    void updatePrev(InetAddress newAddress, int newHash) throws RemoteException;

    void runFileAgent(TreeMap<String, Integer> files) throws RemoteException, InterruptedException, NotBoundException;

    void emptyAllFileList() throws RemoteException;

    void addAllFileList(String file) throws RemoteException;

    String getFileLockRequest() throws RemoteException;

    void setDownloadFileGranted(String download) throws RemoteException;

    int getOwnHash() throws RemoteException;

    void setFileLockRequest(String filename) throws RemoteException;

    String getDownloadFileGranted() throws RemoteException;

    Set getDownloadingFiles() throws RemoteException;

    void setFiles(TreeMap<String, Integer> files) throws RemoteException;

}