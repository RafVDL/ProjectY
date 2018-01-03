package be.ac.ua.dist.systemy.node;

import be.ac.ua.dist.systemy.Constants;
import be.ac.ua.dist.systemy.namingServer.NamingServerInterface;


import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.net.InetAddress;
import java.rmi.NotBoundException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Collection;


public class FailureAgent implements Runnable, Serializable {

    private int hashFailed;
    private int hashStart;
    private InetAddress currNode;
    private InetAddress nsAddress;
    private InetAddress ownerAddressForThisFile;
    private InetAddress addressOfPrevNeighbour;
    private InetAddress localAddress;
    private int ownerHashForThisFile;
    private int hashOfPrevNeighbour;
    private int localHash;
    private Collection<FileHandle> localFiles;
    private Collection<FileHandle> replicatedFiles;
    private String currFile;
    private int[] neighboursOfFailed;
    private String newFileName;

    public FailureAgent(int hashFailed, int hashStart, InetAddress currNode) { //integer is hash of node that is downloading file
        this.hashFailed = hashFailed;
        this.hashStart = hashStart;
        this.currNode = currNode;
    }

    public void run() {
        //initialize rmi connection
        try {
            System.out.println("running FailureAgent");
            Registry currNodeRegistry = LocateRegistry.getRegistry(currNode.getHostAddress(), Constants.RMI_PORT);
            NodeInterface currNodeStub = (NodeInterface) currNodeRegistry.lookup("Node");
            localFiles = currNodeStub.getLocalFiles().values();
            replicatedFiles = currNodeStub.getReplicatedFiles().values();

            nsAddress= currNodeStub.getNamingServerAddress();
            Registry namingServerRegistry = LocateRegistry.getRegistry(nsAddress.getHostAddress(), Constants.RMI_PORT);
            NamingServerInterface namingServerStub = (NamingServerInterface) namingServerRegistry.lookup("NamingServer");
            //Files replicared on failed node need to be rereplicated in the network
            //Files which were available locally on failed node need to be locally available on node which has this file replicated + replicated again in network

            //Step1: Localfiles
            for (FileHandle fileHandle : localFiles) {
                currFile = fileHandle.getFile().getName();
                //Check if file is replicated on failed node
                ownerAddressForThisFile = namingServerStub.getOwner(currFile);
                ownerHashForThisFile = namingServerStub.getHashOfAddress(ownerAddressForThisFile);
                //File needs to be rereplicated
                if (ownerHashForThisFile == hashFailed) {
                    //New replicated node will be previous neighbour of failed node
                    neighboursOfFailed = namingServerStub.getNeighbours(hashFailed);
                    hashOfPrevNeighbour = neighboursOfFailed[0];
                    addressOfPrevNeighbour = namingServerStub.getIPNode(hashOfPrevNeighbour);
                    fileHandle.removeAvailable(hashFailed);
                    currNodeStub.replicateFailed(fileHandle, addressOfPrevNeighbour);
                }
            }

            //Stap 2: Replicated files
            for (FileHandle fileHandle : replicatedFiles) {
                localAddress = fileHandle.getLocalAddress();
                localHash = namingServerStub.getHashOfAddress(localAddress);
                //Replicated file is locally available on failed node
                if (localHash == hashFailed){
                    //File needs locally available on this node and needs to be rereplicated
                    fileHandle.setLocalAddress(currNode);
                    fileHandle.removeAvailable(hashFailed);
                    //Verander map van file
                    //newFileName =  Constants.LOCAL_FILES_PATH + fileHandle.getFile().getName();
                    //File currFile = fileHandle.getFile();
                    //currFile.renameTo(new File(newFileName));
                    currNodeStub.replicateWhenJoining(fileHandle);
                }
            }
        } catch (IOException | NotBoundException e) {
            e.printStackTrace();
        }
    }
}
