package be.ac.ua.dist.systemy.node;

import be.ac.ua.dist.systemy.Constants;

import java.io.IOException;
import java.io.Serializable;
import java.net.InetAddress;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Collection;
import java.util.TreeMap;

/*https://stackoverflow.com/questions/3429921/what-does-serializable-mean*/

public class FileAgent implements Runnable, Serializable {
    // to run: (new Thread(new FileAgent())).start();
    private TreeMap<String, Integer> files = new TreeMap<>();
    private InetAddress nodeAddress;
    private Collection<FileHandle> localFiles;
    private String lockRequest;

    public FileAgent(TreeMap<String, Integer> files, InetAddress nodeAddress) { //integer is hash of node that is downloading file
        this.files = files;
        this.nodeAddress = nodeAddress;
    }

    public void run() {
        //initialize rmi connection
        try {
            Registry currNodeRegistry = LocateRegistry.getRegistry(nodeAddress.getHostAddress(), Constants.RMI_PORT);
            NodeInterface currNodeStub = (NodeInterface) currNodeRegistry.lookup("Node");
            localFiles = currNodeStub.getLocalFiles().values();
            //Stap 1: voeg localFiles toe aan map met files
            for (FileHandle fileHandle : localFiles) {
                files.putIfAbsent(fileHandle.getFile().getName(), 0);
            }
            //Stap 2: update de lijst van bestanden
            currNodeStub.emptyAllFileList();
            if (files != null) {
                files.forEach((key, value) -> {
                    try {
                        currNodeStub.addAllFileList(key, 0);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                });

                //Stap 3: checken naar lock request
                lockRequest = currNodeStub.getFileLockRequest();
                if (!lockRequest.equals("null") && !currNodeStub.getDownloadFileGranted().equals("downloading")) { //if pending lockrequest, and node not downloading
                    if (files.get(lockRequest) == 0) {
                        currNodeStub.setDownloadFileGranted(lockRequest);  //set lockrequest to downloadfilegranted
                        files.put(lockRequest, currNodeStub.getOwnHash()); //add lock request to file agent
                        currNodeStub.addAllFileList(lockRequest, 0); //delete lockrequest on node
                    }
                }
                if (currNodeStub.getDownloadFileGranted().equals("downloading")) { //if node downloading
                    if (!currNodeStub.getDownloadingFiles().contains(currNodeStub.getFileLockRequest())) { //file downloaded
                        currNodeStub.setDownloadFileGranted("null"); //reset nodes downloadfilegranted
                        files.put(lockRequest, 0);  //lift up lock request in file agent
                    }
                }


                currNodeStub.setFiles(this.files);
            }
        } catch (IOException | NotBoundException e) {
            e.printStackTrace();
        }
    }
}