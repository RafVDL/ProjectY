package be.ac.ua.dist.systemy.node;
import be.ac.ua.dist.systemy.Constants;

import java.io.IOException;
import java.io.Serializable;
import java.net.InetAddress;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Set;
import java.util.TreeMap;

/*https://stackoverflow.com/questions/3429921/what-does-serializable-mean*/

public class FileAgent implements Runnable, Serializable{
    // to run: (new Thread(new FileAgent())).start();
    TreeMap<String, Integer> files = new TreeMap<>();
    InetAddress nodeAddress;
    private Set<String> localFiles;
    private String lockRequest;



    public FileAgent(TreeMap<String, Integer> files, InetAddress nodeAddress){ //integer is hash of node that is downloading file
        this.files = files;
        this.nodeAddress = nodeAddress;
        }

    public void run(){
        //initialize rmi connection
        try {
            Registry currNodeRegistry = LocateRegistry.getRegistry(nodeAddress.getHostAddress(), Constants.RMI_PORT);
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
