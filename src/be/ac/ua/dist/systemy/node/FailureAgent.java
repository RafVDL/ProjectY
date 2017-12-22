package be.ac.ua.dist.systemy.node;

import be.ac.ua.dist.systemy.Constants;
import be.ac.ua.dist.systemy.namingServer.NamingServerInterface;


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

            //Bestanden die op de gefaalde node gerepliceerd zijn, moeten verplaatst worden naar zijn vorige node en de eigenaar moet verwittigd zodat deze downloadlocaties kan updaten
            //Bestanden die op de gefaalde node lokaal zijn, de node die deze heeft gerepliceerd wordt de nieuwe eigenaar

            //Stap 1: We bekijken of een of meerdere lokale bestanden van deze node gerepliceerd zijn op de gefaalde node.
            for (FileHandle fileHandle : localFiles) {
                currFile = fileHandle.getFile().getName();
                //Kijken of file naar failed node verwijst
                ownerAddressForThisFile = namingServerStub.getOwner(currFile);
                ownerHashForThisFile = namingServerStub.getHashOfAddress(ownerAddressForThisFile);
                //Bestand gerepliceerd op failed node dus naar nieuwe eigenaar sturen
                if (ownerHashForThisFile == hashFailed) {
                    //Bestand sturen naar nieuwe node, deze node zal de vorige buur zijn van de gefaalde node
                    neighboursOfFailed = namingServerStub.getNeighbours(hashFailed);
                    hashOfPrevNeighbour = neighboursOfFailed[0];
                    addressOfPrevNeighbour = namingServerStub.getIPNode(hashOfPrevNeighbour);
                    currNodeStub.replicateFailed(fileHandle, addressOfPrevNeighbour);
                }
            }

            //Stap 2: We bekijken of een of meerdere gerepliceerde bestanden van deze node lokaal zijn op de gefaalde node.
            for (FileHandle fileHandle : replicatedFiles) {
                localAddress = fileHandle.getLocalAddress();
                localHash = namingServerStub.getHashOfAddress(localAddress);
                //Bestand dat op deze node gerepliceerd is, is lokaal op gefaalde node
                if (localHash == hashFailed){
                    //Bestand moet nu lokaal op deze node bijgehouden worden en opnieuw gerepliceerd worden
                    fileHandle.setLocalAddress(currNode);
                    //Verander map van file - laten voor nu
                    //newFileName =  "localFiles/" + fileHandle.getFile().getName();
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
