package be.ac.ua.dist.systemy.node;

import java.net.InetAddress;
import java.rmi.RemoteException;
import java.util.List;

public class Node implements NodeInterface {
    private String nodeName;
    private InetAddress address;
    private int addressHash;
    private List<String> localFiles;
    private List<String> replicatedFiles;
    private List<String> downloadedFiles;

    public Node(String nodeName, InetAddress address) {
        this.nodeName = nodeName;
        this.address = address;
        this.addressHash=Math.abs(nodeName.hashCode() % 32768);
    }

    @Override
    public List<String> getLocalFileList() throws RemoteException {
        return localFiles;
    }

    @Override
    public List<String> getReplicatedFileList() throws RemoteException {
        return replicatedFiles;
    }

    @Override
    public List<String> getDownloadedFileList() throws RemoteException {
        return downloadedFiles;
    }
}
