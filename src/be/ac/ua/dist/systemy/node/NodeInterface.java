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

    void emptyAllFileList();

    void addAllFileList(String file);

    String getFileLockRequest();

    void setDownloadFileGranted(String download);

    int getOwnHash();

    void setFileLockRequest(String filename);

    String getDownloadFileGranted();

    Set getDownloadingFiles();

    void setFiles(TreeMap<String, Integer> files);

}