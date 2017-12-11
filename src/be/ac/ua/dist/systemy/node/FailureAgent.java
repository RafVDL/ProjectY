package be.ac.ua.dist.systemy.node;

import be.ac.ua.dist.systemy.Constants;

import java.io.IOException;
import java.net.InetAddress;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Set;

public class FailureAgent implements Runnable, Serializable {

    private int hashFailed;
    private int hashStart;
    private InetAddress currNode;
    private Set<String> localFiles;

    public FailureAgent(int hashFailed, int hashStart, InetAddress currNode){ //integer is hash of node that is downloading file
        this.hashFailed = hashFailed;
        this.hashStart = hashStart;
        this.currNode = currNode;
    }

    public void run(){
        //initialize rmi connection
        try {
            Registry currNodeRegistry = LocateRegistry.getRegistry(currNode.getHostAddress(), Constants.RMI_PORT);
            NodeInterface currNodeStub = (NodeInterface) currNodeRegistry.lookup("Node");
            localFiles = currNodeStub.getLocalFiles();
            //Stap 1: voeg localFiles toe aan map met files
            if (localFiles != null) {
                for (String s : localFiles) {
                    if (!files.containsKey(s)) {
                        files.put(s, 0);
                    }
                }
            }
            //Stap 2: update de lijst van bestanden
            currNodeStub.emptyAllFileList();
            if (files != null) {
                files.forEach((key, value) -> {
                    try {
                        currNodeStub.addAllFileList(key);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                });

                //Stap 3: checken naar lock request
                lockRequest = currNodeStub.getFileLockRequest();
                if (!lockRequest.equals("null")) {
                    if (files.get(lockRequest) == 0) {
                        currNodeStub.setDownloadFileGranted(lockRequest);
                        files.put(lockRequest, currNodeStub.getOwnHash());
                        currNodeStub.setFileLockRequest("null");
                    }
                }
                if (!currNodeStub.getDownloadFileGranted().equals("null")) {
                    if (!currNodeStub.getDownloadingFiles().contains(currNodeStub.getDownloadFileGranted())) { //file downloaded
                        currNodeStub.setDownloadFileGranted("null");
                        files.put(lockRequest, 0);
                    }
                }


                currNodeStub.setFiles(this.files);
            }
        } catch (IOException | NotBoundException e ) {
            e.printStackTrace();

        }
        return;


    }
}
