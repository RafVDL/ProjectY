package be.ac.ua.dist.systemy.node;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface NodeInterface extends Remote {
    List<String> getLocalFileList() throws RemoteException;
    List<String> getReplicatedFileList() throws RemoteException;
    List<String> getDownloadedFileList() throws RemoteException;
}