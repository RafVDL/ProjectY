package be.ac.ua.dist.systemy.node;
import be.ac.ua.dist.systemy.Constants;
import be.ac.ua.dist.systemy.namingServer.NamingServerInterface;

import java.io.IOException;
import java.io.Serializable;
import java.net.InetAddress;
import java.rmi.NotBoundException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Set;
import java.util.TreeMap;

/*https://stackoverflow.com/questions/3429921/what-does-serializable-mean*/

public class FileAgent implements Runnable, Serializable{
    // to run: (new Thread(new FileAgent())).start();
    TreeMap<String, Integer> files = new TreeMap<>();
    Node node;
    private Set<String> localFiles;
    private String lockRequest;



    public FileAgent(TreeMap<String, Integer> files, Node node){ //integer is hash of node that is downloading file
        this.files = files;
        this.node = node;
        }

    public void run(){
        localFiles = node.getLocalFiles();
        //Stap 1: voeg localFiles toe aan map met files
        if(localFiles != null) {
            for (String s : localFiles) {
                if (!files.containsKey(s)) {
                    files.put(s, 0);
                }
            }
        }
        //Stap 2: update de lijst van bestanden
        node.emptyAllFileList();
        if(files != null) {
            files.forEach((key, value) -> {
                node.addAllFileList(key);
            });

            //Stap 3: checken naar lock request
            lockRequest = node.getFileLockRequest();
            if (files.get(lockRequest) == 0) {
                InetAddress ownerAddress = null;
                try {
                    // Get ownerAddress from NamingServer via RMI.
                    Registry namingServerRegistry = LocateRegistry.getRegistry(node.getNamingServerAddress().getHostAddress(), Constants.RMI_PORT);
                    NamingServerInterface namingServerStub = (NamingServerInterface) namingServerRegistry.lookup("NamingServer");
                    ownerAddress = namingServerStub.getOwner(lockRequest);
                } catch (IOException | NotBoundException e) {
                    e.printStackTrace();
                }
                if (ownerAddress == null) {
                    //Error
                } else {
                    node.downloadFile(lockRequest, lockRequest, ownerAddress);
                    files.put(lockRequest, node.getOwnHash());
                }
            } else if (files.get(lockRequest) == node.getOwnHash()) {
                if (!node.getDownloadingFiles().contains(lockRequest)) { //file downloaded
                    node.setFileLockRequest(null);
                    files.put(lockRequest, 0);
                }
            }


            node.setFiles(this.files);
        }
        return;


    }



}
