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
    private int currNodeHash;
    private int currNodePrevNeighbour;
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
            currNodeHash = currNodeStub.getOwnHash();
            currNodePrevNeighbour = currNodeStub.getPrevHash();
            localFiles = currNodeStub.getLocalFiles().values();
            replicatedFiles = currNodeStub.getReplicatedFiles().values();

            nsAddress= currNodeStub.getNamingServerAddress();
            Registry namingServerRegistry = LocateRegistry.getRegistry(nsAddress.getHostAddress(), Constants.RMI_PORT);
            NamingServerInterface namingServerStub = (NamingServerInterface) namingServerRegistry.lookup("NamingServer");
            //Files replicared on failed node need to be rereplicated in the network
            //Files which were available locally on failed node need to be locally available on node which has this file replicated + replicated again in network

            //Step1: Localfiles
            System.out.println("Handling local files from this node...");
            for (FileHandle fileHandle : localFiles) {
                currFile = fileHandle.getFile().getName();
                //Check if file is replicated on failed node
                ownerAddressForThisFile = namingServerStub.getOwner(currFile);
                ownerHashForThisFile = namingServerStub.getHashOfAddress(ownerAddressForThisFile);
                System.out.println("Replicated hash van "+ currFile + ": " + ownerHashForThisFile);
                //File needs to be rereplicated
                if (ownerHashForThisFile == hashFailed || (ownerHashForThisFile == currNodeHash && hashFailed == currNodePrevNeighbour)) {
                    System.out.println(currFile + "was replicated on failed node and will be processed...");
                    //New replicated node will be previous neighbour of failed node
                    neighboursOfFailed = namingServerStub.getNeighbours(hashFailed);
                    hashOfPrevNeighbour = neighboursOfFailed[0];
                    //If previous neighbour of failed node is this node, send replicated file to own previous neighbour
                    if(hashOfPrevNeighbour == currNodeHash) {
                        hashOfPrevNeighbour = currNodeStub.getPrevHash();
                    }
                    addressOfPrevNeighbour = namingServerStub.getIPNode(hashOfPrevNeighbour);
                    fileHandle.removeAvailable(hashFailed);
                    System.out.println(currFile + " will be replicated on " + addressOfPrevNeighbour + " " + hashOfPrevNeighbour);
                    currNodeStub.replicateFailed(fileHandle, addressOfPrevNeighbour);
                }
            }

            //Stap 2: Replicated files
            System.out.println("Handling replicated files from this node...");
            for (FileHandle fileHandle : replicatedFiles) {
                currFile = fileHandle.getFile().getName();
                localAddress = fileHandle.getLocalAddress();
                localHash = namingServerStub.getHashOfAddress(localAddress);
                System.out.println("Local hash van "+ currFile + ": " + localHash);
                //Replicated file is locally available on failed node
                if (localHash == hashFailed){
                    System.out.println(currFile + " was local on failed node and will be processed...");
                    //File needs locally available on this node and needs to be rereplicated
                    hashOfPrevNeighbour = currNodePrevNeighbour;
                    if(currNodePrevNeighbour == hashFailed) {
                        neighboursOfFailed = namingServerStub.getNeighbours(hashFailed);
                        hashOfPrevNeighbour = neighboursOfFailed[0];
                    }
                    addressOfPrevNeighbour = namingServerStub.getIPNode(hashOfPrevNeighbour);
                    fileHandle.setLocalAddress(currNode);
                    fileHandle.removeAvailable(hashFailed);
                    currNodeStub.replicateFailed(fileHandle, addressOfPrevNeighbour);
                    //Change file from replicated to local
                    String newFileName =  Constants.LOCAL_FILES_PATH + fileHandle.getFile().getName();
                    File currFile = fileHandle.getFile();
                    currFile.renameTo(new File(newFileName));
                    currNodeStub.removeReplicatedFile(fileHandle);
                }
            }

        } catch (IOException | NotBoundException e) {
            e.printStackTrace();
        }
    }
}
