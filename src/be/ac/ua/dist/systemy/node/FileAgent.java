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
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

public class FileAgent implements Runnable, Serializable {
    private HashMap<String, Integer> files;
    private InetAddress nodeAddress;
    private Collection<String> localFiles;
    private String lockRequest;

    /**
     * Creates FileAgent
     *
     * @param files:       map of all files on the system with corresponding hash of node that has a lockrequest on this file
     * @param nodeAddress: InetAddress of the node on which the current FileAgent is running
     */
    public FileAgent(HashMap<String, Integer> files, InetAddress nodeAddress) {
        this.files = files;
        this.nodeAddress = nodeAddress;
    }

    /**
     * The FileAgent runs in several steps
     * 0) initialize RMI connection to node on which the file agent is running
     * 1) Add new local files of node to allFiles in FileAgent
     * 2) update list of all files on the node
     * 3) check if node wants to download a file in the network
     *      A  node can only download one file at a time
     * 4) check if a file needs to be deleted and delete from node
     * 5) check if current node wants to delete a file
     */
    public void run() {
        try { //Step 0
            Registry currNodeRegistry = LocateRegistry.getRegistry(nodeAddress.getHostAddress(), Constants.RMI_PORT);
            NodeInterface currNodeStub = (NodeInterface) currNodeRegistry.lookup("Node");
            localFiles = currNodeStub.getLocalFiles().keySet();
            Map<String, Integer> allFilesNode = currNodeStub.getAllFiles();
            //Step 1
            for (String fileHandle : localFiles) {
                files.putIfAbsent(fileHandle, 0);
            }
            //Step 2 and 4
            if (files != null) {
                Map<String, Integer> filesNode = currNodeStub.getAllFiles();
                Iterator it = files.entrySet().iterator();
                while (it.hasNext()){
                    try {
                        Entry<String, Integer> pair = (Entry<String, Integer>)it.next();
                        String key = pair.getKey();
                        int value = pair.getValue();
                        if (!allFilesNode.containsKey(key)) {
                            currNodeStub.addAllFileList(key, 0);
                        }
                        if (value == -1) { //file needs to be deleted
                            if (filesNode.containsKey(key) && filesNode.get(key) == -1) { //node that wanted the file deleted -> last node on which file needs to be deleted
                                currNodeStub.deleteFileFromNode(new FileHandle(key, true));
                                currNodeStub.removeAllFileList(key);
                                it.remove();
                            } else if (filesNode.containsKey(key)){
                                currNodeStub.deleteFileFromNode(new FileHandle(key, true));
                                currNodeStub.removeAllFileList(key);
                            }
                        }

                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }
                //Step 3
                lockRequest = currNodeStub.getFileLockRequest();
                if (currNodeStub.getDownloadFileGranted().equals("downloading") && !currNodeStub.getDownloadingFiles().contains(currNodeStub.getFileLockRequest())) {//if file downloaded
                    currNodeStub.setDownloadFileGranted("null");                                                //reset nodes downloadfilegranted
                    files.put(lockRequest, 0);                                                                  //lift up lock request in FileAgent
                    currNodeStub.addAllFileList(lockRequest, 0);//clear lockrequest on node
                }
                if (!lockRequest.equals("null") && !currNodeStub.getDownloadFileGranted().equals("downloading")) {  //if pending lockrequest, and node not downloading
                    if (files.containsKey(lockRequest) && files.get(lockRequest) == 0) {
                        currNodeStub.setDownloadFileGranted(lockRequest);                                           //tell node he can download the file
                        files.put(lockRequest, currNodeStub.getOwnHash());                                          //lock file in FileAgent
                    }
                }
                //Step 5
                filesNode.forEach((key, value) -> {
                    if(value == -1){
                        files.put(key, -1);
                    }
                        });

                currNodeStub.setFileAgentFiles(files);
            }
        } catch (IOException | NotBoundException e) {
            e.printStackTrace();
        }
    }
}
