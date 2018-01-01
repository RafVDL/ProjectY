package be.ac.ua.dist.systemy.node;

import be.ac.ua.dist.systemy.Constants;
import be.ac.ua.dist.systemy.namingServer.NamingServerInterface;
import be.ac.ua.dist.systemy.networking.Client;
import be.ac.ua.dist.systemy.networking.Communications;
import be.ac.ua.dist.systemy.networking.Server;
import be.ac.ua.dist.systemy.networking.packet.*;
import be.ac.ua.dist.systemy.networking.tcp.TCPServer;
import be.ac.ua.dist.systemy.networking.udp.MulticastServer;
import be.ac.ua.dist.systemy.networking.udp.UnicastServer;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.io.*;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.rmi.AlreadyBoundException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static be.ac.ua.dist.systemy.Constants.RMI_PORT;

public class Node implements NodeInterface {

    private final InetAddress ownAddress;
    private final int ownHash;
    private String grantedDownloadFile;
    private Map<String, FileHandle> localFiles;
    private Map<String, FileHandle> replicatedFiles;
    private Map<String, FileHandle> ownerFiles;
    private Set<String> downloadingFiles;
    private Map<String, Integer> allFiles;
    private ObservableList<String> allFilesObservable;
    private HashMap<String, Integer> fileAgentFiles;


    private volatile InetAddress namingServerAddress;
    private InetAddress prevAddress;
    private InetAddress nextAddress;
    private InetAddress ownerAddress;

    private volatile int prevHash;
    private volatile int nextHash;

    private boolean isRunning = true;
    private boolean isGUIStarted;
    private boolean isInitialized = false;

    private InetAddress multicastGroup;
    private Server multicastServer;
    private Server tcpServer;

    public Node(String nodeName, InetAddress address, boolean isGUIStarted) throws UnknownHostException {
        this.isGUIStarted = isGUIStarted;

        this.ownAddress = address;
        this.ownerAddress = ownAddress;
        this.ownHash = calculateHash(nodeName);
        this.allFiles = new HashMap<>();
        this.allFilesObservable = FXCollections.observableArrayList(new ArrayList<>());
        this.grantedDownloadFile = "null";

        this.localFiles = new HashMap<>();
        this.replicatedFiles = new HashMap<>();
        this.ownerFiles = new HashMap<>();
        this.downloadingFiles = new HashSet<>();

        multicastGroup = InetAddress.getByName(Constants.MULTICAST_ADDRESS);
    }

    public InetAddress getOwnAddress() {
        return ownAddress;
    }

    @Override
    public InetAddress getPrevAddress() {
        return prevAddress;
    }

    @Override
    public InetAddress getNextAddress() {
        return nextAddress;
    }

    @Override
    public int getOwnHash() {
        return ownHash;
    }

    @Override
    public int getPrevHash() {
        return prevHash;
    }

    @Override
    public int getNextHash() {
        return nextHash;
    }

    public boolean isInitialized() {
        return isInitialized;
    }

    public void setInitialized(boolean initialized) {
        isInitialized = initialized;
    }

    public void setNamingServerAddress(InetAddress ipAddress) {
        this.namingServerAddress = ipAddress;
    }

    public InetAddress getNamingServerAddress() {
        return namingServerAddress;
    }


    @Override
    public Map<String, FileHandle> getLocalFiles() {
        return localFiles;
    }

    @Override
    public Map<String, FileHandle> getReplicatedFiles() {
        return replicatedFiles;
    }

    public Set getDownloadingFiles() {
        return downloadingFiles;
    }

    public String getFileLockRequest() {
        AtomicReference<String> fileLockRequest = new AtomicReference<>("null");
        allFiles.forEach((String key, Integer value) -> {
            if (ownHash == value) fileLockRequest.set(key);
        });
        return fileLockRequest.get();
    }

    public void downloadAFile(String filename) {
        if (allFiles.containsKey(filename)) {
            allFiles.put(filename, ownHash);
        } else {
            System.out.println("File does not exist or you are the owner, try again");
        }
    }

    @Override
    public Map<String, Integer> getAllFiles() {
        return allFiles;
    }

    public ObservableList<String> getAllFilesObservable() {
        return allFilesObservable;
    }

    @Override
    public void addLocalFileList(FileHandle fileHandle) {
        localFiles.put(fileHandle.getFile().getName(), fileHandle);
    }

    @Override
    public void addReplicatedFileList(FileHandle fileHandle) {
        replicatedFiles.put(fileHandle.getFile().getName(), fileHandle);
    }

    @Override
    public void addOwnerFileList(FileHandle fileHandle) {
        ownerFiles.put(fileHandle.getFile().getName(), fileHandle);
    }

    @Override
    public void removeLocalFile(FileHandle fileHandle) {
        localFiles.remove(fileHandle.getFile().getName());
    }

    @Override
    public void removeReplicatedFile(FileHandle fileHandle) {
        replicatedFiles.remove(fileHandle.getFile().getName());
    }

    @Override
    public void removeOwnerFile(FileHandle fileHandle) {
        ownerFiles.remove(fileHandle.getFile().getName());
    }

    public void addAllFileList(String file, int value) {
        this.allFiles.put(file, value);
    }

    public void removeAllFileList(String file){
        this.allFiles.remove(file);
    }

    public void setFileAgentFiles(HashMap<String, Integer> files) {
        this.fileAgentFiles = files;
    }

    public void setDownloadFileGranted(String download) {
        this.grantedDownloadFile = download;
    }

    public String getDownloadFileGranted() {
        return this.grantedDownloadFile;
    }

    @Override
    public void downloadFile(String remoteFileName, String localFileName, InetAddress remoteAddress) {
        downloadingFiles.add(localFileName.split("/")[1]);
        System.out.println("Adding " + localFileName + " to downloading files");
        //Open tcp socket to server @remoteAddress:port

        try {
            Client client = Communications.getTCPClient(remoteAddress, Constants.TCP_PORT);
            DataInputStream dis = client.getConnection().getDataInputStream();
            FileOutputStream fos = new FileOutputStream(localFileName);

            FileRequestPacket fileRequestPacket = new FileRequestPacket(remoteFileName);
            client.sendPacket(fileRequestPacket);

            int fileSize = dis.readInt();

            byte[] buffer = new byte[4096];
            int read;
            int remaining = fileSize;

            while ((read = dis.read(buffer, 0, Math.min(buffer.length, remaining))) > 0) {
                remaining -= read;
                fos.write(buffer, 0, read);
            }

            fos.close();
            client.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        downloadingFiles.remove(localFileName.split("/")[1]);
        System.out.println("Removing " + localFileName + " from downloading files");
    }

    /**
     * Removes a file from the Node. This includes the actual file on disk as well as the lists.
     *
     * @param fileHandle the fileHandle of the file to remove
     */
    @Override
    public void deleteFileFromNode(FileHandle fileHandle) {
        ownerFiles.remove(fileHandle.getFile().getName());
        localFiles.remove(fileHandle.getFile().getName());
        replicatedFiles.remove(fileHandle.getFile().getName());
        if(fileHandle.getFile().exists()) {
            fileHandle.getFile().delete();
        } else if(fileHandle.getAsReplicated().getFile().exists()){
            fileHandle.getAsReplicated().getFile().delete();
        }
    }

    public void deleteFileFromNetwork(String filename){
        if (allFiles.containsKey(filename)) {
            allFiles.put(filename, -1);
        } else {
            System.out.println("File does not exist or you are the owner, try again");
        }
    }


    /**
     * Add a Node's hash from the list of available hashes. Note that this method only work for updating the FileHandle
     * of a local file (otherwise it is unnecessary anyways).
     *
     * @param fileName  the fileName of corresponding fileHandle to update
     * @param hashToAdd the hash to add
     */
    @Override
    public void addToAvailableNodes(String fileName, int hashToAdd) {
        if (!ownerFiles.containsKey(fileName)) {
            System.err.println("Error: trying to update a FileHandle of a file that this Node does not own.");
            return;
        }
        ownerFiles.get(fileName).getAvailableNodes().add(hashToAdd);
    }

    /**
     * Remove a Node's hash from the list of available hashes. Note that this method only work for updating the FileHandle
     * of a local file (otherwise it is unnecessary anyways).
     *
     * @param fileName     the fileName of corresponding fileHandle to update
     * @param hashToRemove the hash to remove
     */
    @Override
    public void removeFromAvailableNodes(String fileName, int hashToRemove) {
        if (!ownerFiles.containsKey(fileName)) {
            System.out.println("Error: trying to update a FileHandle of a file that this Node does not own.");
            return;
        }
        ownerFiles.get(fileName).getAvailableNodes().remove(hashToRemove);
    }

    /**
     * Updates the next neighbour of this node
     *
     * @param newAddress of the next neighbour
     * @param newHash    of the next neighbour
     */
    @Override
    public void updateNext(InetAddress newAddress, int newHash) {
        if (newAddress == null) {
            try {
                newAddress = getAddressByHash(newHash);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        nextAddress = newAddress;
        nextHash = newHash;
    }

    /**
     * Updates the previous neighbour of this node
     *
     * @param newAddress of the previous neighbour
     * @param newHash    of the previous neighbour
     */
    @Override
    public void updatePrev(InetAddress newAddress, int newHash) {
        if (newAddress == null) {
            try {
                newAddress = getAddressByHash(newHash);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        prevAddress = newAddress;
        prevHash = newHash;
    }

    /**
     * Starts isRunning the file agent, this method can be run via RMI
     *
     * @param fileAgentFiles: list of all files
     *                        String: filename
     *                        Integer: hash of node that has a lock request on that file
     */
    @Override
    public void runFileAgent(HashMap<String, Integer> fileAgentFiles) throws InterruptedException {
        // This method can get called (RMI) before the node has finished initializing -> wait
        while (!isInitialized) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
                System.err.println("Interrupted while waiting for Node to initialize.");
            }
        }


        ownerAddress = ownAddress;
        Thread t = new Thread(new FileAgent(fileAgentFiles, ownAddress));
        t.start();
        t.join(); //wait for thread to stop

        // Update the observable for the GUI if the node has a GUI
        if (isGUIStarted) {
            Platform.runLater(() -> {
                for (Iterator<String> iterator = fileAgentFiles.keySet().iterator(); iterator.hasNext(); ) {
                    String fileName = iterator.next();
                    if (!allFilesObservable.contains(fileName)) {
                        allFilesObservable.add(fileName);
                    }
                }

                allFilesObservable.retainAll(fileAgentFiles.keySet());
            });
        }


        Thread t2 = new Thread(() -> {
            try {
                Registry registry = LocateRegistry.getRegistry(nextAddress.getHostAddress(), RMI_PORT);
                NodeInterface stub = (NodeInterface) registry.lookup("Node");
                stub.runFileAgent(fileAgentFiles);
            } catch (RemoteException | NotBoundException | InterruptedException e) {
                try {
                    //failureAgent wordt voor de eerste keer gestart en zal uitgevoerd vooraleer de fileAgent terug zal worden opgestart
                    runFailureAgent(nextHash, ownHash, ownAddress);
                } catch (InterruptedException e1) {
                    e1.printStackTrace();
                } catch (RemoteException e1) {
                    e1.printStackTrace();
                } catch (NotBoundException e1) {
                    e1.printStackTrace();
                }
            }

        });
        String fileLockRequest = getFileLockRequest();
        if (!grantedDownloadFile.equals("null") && !grantedDownloadFile.equals("downloading") && !fileLockRequest.equals("null")) { //download file
            Thread t3 = new Thread(() -> {
                try {
                    // Get ownerAddress from NamingServer via RMI.
                    Registry namingServerRegistry = LocateRegistry.getRegistry(getNamingServerAddress().getHostAddress(), Constants.RMI_PORT);
                    NamingServerInterface namingServerStub = (NamingServerInterface) namingServerRegistry.lookup("NamingServer");
                    ownerAddress = namingServerStub.getOwner(grantedDownloadFile);
                    if (ownerAddress.equals(ownAddress)) {
                        ownerAddress = getLocalAddressOfFile(grantedDownloadFile);
                    } else {
                        Registry registry = LocateRegistry.getRegistry(ownerAddress.getHostAddress(), RMI_PORT);
                        NodeInterface ownerStub = (NodeInterface) registry.lookup("Node");
                        ownerAddress = ownerStub.getLocalAddressOfFile(grantedDownloadFile);
                    }
                } catch (IOException | NotBoundException e) {
                    e.printStackTrace();
                }

                if (ownerAddress.equals(ownAddress)) {
                    System.out.println("You are the owner");
                    grantedDownloadFile = "null";
                    allFiles.put(fileLockRequest, 0);
                } else {
                    downloadFile(Constants.LOCAL_FILES_PATH + grantedDownloadFile, Constants.DOWNLOADED_FILES_PATH + grantedDownloadFile, ownerAddress);
                    grantedDownloadFile = "downloading";
                }
            });
            t3.start();
            Thread.sleep(1000);
        }
        t2.start();
    }

    @Override
    public void runFailureAgent(int hashFailed, int hashStart, InetAddress currNode) throws InterruptedException, RemoteException, NotBoundException {
        //failureAgent niet starten als er maar 2 nodes in het netwerk zitten waarvan er 1 gefaald is
        if (!(hashFailed == prevHash && hashFailed == nextHash)) {
            Thread t = new Thread(new FailureAgent(hashFailed, hashStart, currNode));
            t.start();
            t.join(); //wait for thread to stop
            //Kijken of hij niet alleen in het netwerk zit en dat zijn volgende node niet de gefaalde node is.
            if (ownHash != nextHash && hashFailed != nextHash && hashStart != nextHash) {
                Thread t4 = new Thread(() -> {
                    try {
                        Registry registry = LocateRegistry.getRegistry(nextAddress.getHostAddress(), RMI_PORT);
                        NodeInterface stub = (NodeInterface) registry.lookup("Node");
                        stub.runFailureAgent(hashFailed, hashStart, nextAddress);
                    } catch (RemoteException | NotBoundException | InterruptedException e) {
                        e.printStackTrace();
                    }
                });
                t4.start();
            }
            //Kijken of hij niet alleen in het netwerk zit en dat de volgende node de gefaalde node is.
            if (hashFailed == nextHash && hashFailed != prevHash && nextHash != hashStart) {
                Thread t5 = new Thread(() -> {
                    try {
                        //Volgende node is de gefaalde node dus agent moet deze overslaan en naar de volgende node in de cycle gaan (=volgende buur van de gefaalde node).
                        Registry namingServerRegistry = LocateRegistry.getRegistry(namingServerAddress.getHostAddress(), Constants.RMI_PORT);
                        NamingServerInterface namingServerStub = (NamingServerInterface) namingServerRegistry.lookup("NamingServer");
                        int[] neighboursOfFailed = namingServerStub.getNeighbours(hashFailed);
                        int hashOfNextNeighbour = neighboursOfFailed[1];
                        InetAddress addressOfNextNeighbour = namingServerStub.getIPNode(hashOfNextNeighbour);

                        Registry registry = LocateRegistry.getRegistry(addressOfNextNeighbour.getHostAddress(), RMI_PORT);
                        NodeInterface stub = (NodeInterface) registry.lookup("Node");
                        //Checken of volgende buur van de gefaalde node niet de node is waarop de agent werd gestart
                        if (hashOfNextNeighbour != hashStart) {
                            stub.runFailureAgent(hashFailed, hashStart, addressOfNextNeighbour);
                        } else {
                            FailureHandler failureHandler = new FailureHandler(hashFailed, this);
                            failureHandler.repairFailedNode();
                            stub.runFileAgent(fileAgentFiles);
                        }
                    } catch (RemoteException | NotBoundException | InterruptedException e) {
                        e.printStackTrace();
                    }
                });
                t5.start();
            }
            //Agent is de kring rond gegaan
            if (nextHash == hashStart) {
                Thread t6 = new Thread(() -> {
                    try {
                        Registry registry = LocateRegistry.getRegistry(nextAddress.getHostAddress(), RMI_PORT);
                        NodeInterface stub = (NodeInterface) registry.lookup("Node");
                        FailureHandler failureHandler = new FailureHandler(hashFailed, this);
                        failureHandler.repairFailedNode();
                        stub.runFileAgent(fileAgentFiles);
                    } catch (RemoteException | NotBoundException | InterruptedException e) {
                        e.printStackTrace();
                    }
                });
                t6.start();
            }
        }
        //node zit alleen in het netwerk, gefaalde node mag eruit verwijderd worden
        else {
            FailureHandler failureHandler = new FailureHandler(hashFailed, this);
            failureHandler.repairFailedNode();
        }
    }

    /**
     * Gets invoked when a new Node is joining the network.
     * <p>
     * Existing Node checks if the new Node becomes a new neighbour of this Node. If so, it checks whether the new node
     * becomes a previous or next neighbour. If it is a previous, the existing Node only updates its own neighbours.
     * Else, the Node also sends an update (via tcp) to the new node.
     * <p>
     * In the special case that there is only one existing Node in the network, its neighbours are the Node itself. In
     * this case the existing Node should always update the joining Node.
     *
     * @param newAddress the IP-address of the joining node
     * @param newHash    the hash of the joining node
     */
    public void updateNeighbours(InetAddress newAddress, int newHash) {
        if ((ownHash == prevHash) && (ownHash == nextHash)) {
            // NodeCount is currently 0, always update self and the joining Node.

            try {
                Client client = Communications.getTCPClient(newAddress, Constants.TCP_PORT);
                UpdateNeighboursPacket packet = new UpdateNeighboursPacket(ownHash, ownHash);
                client.sendPacket(packet);
            } catch (IOException e) {
                handleFailure(newHash);
                e.printStackTrace();
            }

            updatePrev(newAddress, newHash);
            updateNext(newAddress, newHash);


        } else if ((prevHash < ownHash && newHash < ownHash && newHash > prevHash) // 1, 13
                || (prevHash > ownHash && newHash < ownHash) // 6
                || (prevHash > ownHash && newHash > prevHash) // 5
                || ((prevHash == nextHash) && ((prevHash > ownHash && newHash > prevHash) // 15
                || (prevHash > ownHash && newHash < ownHash)))) { // 16
            // Joining Node sits between previous neighbour and this Node.

            updatePrev(newAddress, newHash);
        } else if ((nextHash > ownHash && newHash > ownHash && newHash < nextHash) // 2, 8
                || (nextHash < ownHash && newHash > ownHash) // 10
                || (nextHash < ownHash && newHash < nextHash) // 11
                || ((prevHash == nextHash) && (prevHash > ownHash && newHash < prevHash && newHash > ownHash) // 14
                || (prevHash < ownHash && newHash < prevHash) // 18
                || (prevHash < ownHash && newHash > ownHash) // 19
                || (prevHash < ownHash && newHash < ownHash && newHash < prevHash))) {
            // Joining Node sits between this Node and next neighbour.

            try {
                Client client = Communications.getTCPClient(newAddress, Constants.TCP_PORT);
                UpdateNeighboursPacket packet = new UpdateNeighboursPacket(ownHash, nextHash);
                client.sendPacket(packet);
            } catch (IOException e) {
                handleFailure(newHash);
                e.printStackTrace();
            }

            updateNext(newAddress, newHash);
        }
    }

    private InetAddress getAddressByHash(int hash) throws IOException {
        Client client = Communications.getUDPClient(multicastGroup, Constants.MULTICAST_PORT);

        final InetAddress[] address = new InetAddress[1];

        CountDownLatch latch = new CountDownLatch(1);

        Server unicastServer = new UnicastServer();
        unicastServer.registerListener(IPResponsePacket.class, ((packet, client1) -> {
            address[0] = packet.getAddress();
            unicastServer.stop();
            latch.countDown();
        }));
        unicastServer.startServer(ownAddress, Constants.UNICAST_PORT);

        GetIPPacket packet = new GetIPPacket(hash);
        client.sendPacket(packet);

        try {
            latch.await(10, TimeUnit.SECONDS);
            return address[0];
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void joinNetwork() throws IOException {
        HelloPacket helloPacket = new HelloPacket();
        Client client = Communications.getUDPClient(multicastGroup, Constants.MULTICAST_PORT);
        client.sendPacket(helloPacket);
    }

    public void leaveNetwork() throws IOException {
        QuitPacket quitPacket = new QuitPacket();
        Client client = Communications.getUDPClient(multicastGroup, Constants.MULTICAST_PORT);
        client.sendPacket(quitPacket);

        multicastServer.stop();

        if (ownHash == nextHash && ownHash == prevHash)
            return;

        if (prevHash == nextHash) {
            Client clientPrev = Communications.getTCPClient(prevAddress, Constants.TCP_PORT);
            clientPrev.sendPacket(new UpdateNeighboursPacket(nextHash, nextHash));
            return;
        }

        Client clientPrev = Communications.getTCPClient(prevAddress, Constants.TCP_PORT);
        clientPrev.sendPacket(new UpdateNeighboursPacket(-1, nextHash));

        Client clientNext = Communications.getTCPClient(nextAddress, Constants.TCP_PORT);
        clientNext.sendPacket(new UpdateNeighboursPacket(prevHash, -1));
    }

    public void handleFailure(int hashFailedNode) {
        try {
            FailureHandler failureHandler = new FailureHandler(hashFailedNode, this);
            failureHandler.repairFailedNode();
        } catch (RemoteException | NotBoundException e) {
            e.printStackTrace();
        }
    }

    public int calculateHash(String name) {
        return Math.abs(name.hashCode() % 32768);
    }

    public boolean isRunning() {
        return isRunning;
    }

    public void setRunning(boolean running) {
        this.isRunning = running;
    }

    public InetAddress getLocalAddressOfFile(String filename) {
        return ownerFiles.get(filename).getLocalAddress();
    }

    /**
     * Iterates through all files in the LOCAL_FILES_PATH and introduces each one in the network.
     */
    public void discoverLocalFiles() {
        File folder = new File(Constants.LOCAL_FILES_PATH);
        if (!folder.exists())
            folder.mkdir();
        File[] listOfFiles = folder.listFiles();

        for (File file : listOfFiles) {
            if (file.isFile()) {
                System.out.println("Found file " + file.getName());
                FileHandle fileHandle = new FileHandle(file.getName(), true);
                fileHandle.setLocalAddress(ownAddress);
                addFileToNetwork(fileHandle);
            } else if (file.isDirectory()) {
                System.out.println("Not checking files in nested folder " + file.getName());
            }
        }

        System.out.println("Finished discovery of " + folder.getName());
    }

    public void replicateNewOwnerFiles() {
        Map<String, FileHandle> originalOwnerFiles = new HashMap<>(ownerFiles);

        originalOwnerFiles.forEach(((s, fileHandle) -> {
            try {
                replicateToNewNode(fileHandle);
            } catch (RemoteException | NotBoundException | UnknownHostException e) {
                e.printStackTrace();
            }
        }));
    }

    /**
     * Introduces a new local file in the network.
     * <p>
     * The NamingServer gets asked who the owner of the file should be. If this Node should
     * be the owner, the file gets duplicated to the previous neighbour via RMI. If this node should not be the owner, the
     * file gets duplicated to the owner.
     *
     * @param fileHandle enclosing the file to introduce in the network
     */
    public void addFileToNetwork(FileHandle fileHandle) {
        File file = fileHandle.getFile();
        if (!file.isFile()) {
            System.out.println("Trying to add something that is not a file (skipping)");
            return;
        }

        try {
            // Put in local copy in localFiles and update the FileHandle
            localFiles.put(fileHandle.getFile().getName(), fileHandle);
            fileHandle.setLocal(true);
            fileHandle.getAvailableNodes().add(ownHash);

            replicateWhenJoining(fileHandle);
        } catch (IOException | NotBoundException e) {
            e.printStackTrace();
        }
    }

    public void replicateWhenJoining(FileHandle fileHandle) throws RemoteException, NotBoundException, UnknownHostException {
        File file = fileHandle.getFile();

        Registry namingServerRegistry = LocateRegistry.getRegistry(namingServerAddress.getHostAddress(), Constants.RMI_PORT);
        NamingServerInterface namingServerStub = (NamingServerInterface) namingServerRegistry.lookup("NamingServer");
        InetAddress ownerAddress = namingServerStub.getOwner(file.getName());

        if (ownerAddress == null) // no owner
            return;

        if (ownAddress.equals(nextAddress)) {
            ownerFiles.put(fileHandle.getFile().getName(), fileHandle);
            return;
        }

        Registry nodeRegistry;
        if (ownerAddress.equals(ownAddress)) {
            // Replicate to previous node
            nodeRegistry = LocateRegistry.getRegistry(prevAddress.getHostAddress(), Constants.RMI_PORT);
        } else {
            // Replicate to owner node
            nodeRegistry = LocateRegistry.getRegistry(ownerAddress.getHostAddress(), Constants.RMI_PORT);
        }

        NodeInterface nodeStub = (NodeInterface) nodeRegistry.lookup("Node");
        nodeStub.downloadFile(file.getPath(), Constants.REPLICATED_FILES_PATH + file.getName(), ownAddress);

        fileHandle.getAvailableNodes().add(ownerAddress.equals(ownAddress) ? prevHash : nodeStub.getOwnHash());

        FileHandle newFileHandle = fileHandle.getAsReplicated();
        nodeStub.addReplicatedFileList(newFileHandle);

        if (ownerAddress.equals(ownAddress)) {
            ownerFiles.put(newFileHandle.getFile().getName(), fileHandle);
        } else {
            nodeStub.addOwnerFileList(newFileHandle);
        }
    }

    public void replicateToNewNode(FileHandle fileHandle) throws RemoteException, NotBoundException, UnknownHostException {
        File file = fileHandle.getFile();

        Registry namingServerRegistry = LocateRegistry.getRegistry(namingServerAddress.getHostAddress(), Constants.RMI_PORT);
        NamingServerInterface namingServerStub = (NamingServerInterface) namingServerRegistry.lookup("NamingServer");
        InetAddress ownerAddress = namingServerStub.getOwner(file.getName());

        if (ownerAddress == null) // no owner
            return;

        if (ownAddress.equals(nextAddress)) {
            ownerFiles.put(fileHandle.getFile().getName(), fileHandle);
            return;
        }

        if (!ownerAddress.equals(ownAddress)) {
            Registry nextNodeRegistry = LocateRegistry.getRegistry(nextAddress.getHostAddress(), Constants.RMI_PORT);
            NodeInterface nextNodeStub = (NodeInterface) nextNodeRegistry.lookup("Node");

            nextNodeStub.downloadFile(file.getPath(), Constants.REPLICATED_FILES_PATH + file.getName(), ownAddress);

            if (prevHash != nextHash)
                fileHandle.getAvailableNodes().remove(ownHash);

            fileHandle.getAvailableNodes().add(nextHash);

            FileHandle newFileHandle = fileHandle.getAsReplicated();
            nextNodeStub.addReplicatedFileList(newFileHandle);
            nextNodeStub.addOwnerFileList(newFileHandle);

            replicatedFiles.remove(fileHandle.getFile().getName());
            ownerFiles.remove(fileHandle.getFile().getName());
        }
    }

    public void replicateFailed(FileHandle fileHandle, InetAddress receiveAddress) throws RemoteException, NotBoundException {
        File file = fileHandle.getFile();

        if (receiveAddress == null) // no owner
            return;

        if (receiveAddress.equals(nextAddress)) {
            ownerFiles.put(fileHandle.getFile().getName(), fileHandle);
            return;
        }

        Registry nodeRegistry;
        if (receiveAddress.equals(ownAddress)) {
            //Replicate to previous node
            nodeRegistry = LocateRegistry.getRegistry(prevAddress.getHostAddress(), Constants.RMI_PORT);
        } else {
            // Replicate to receive node
            nodeRegistry = LocateRegistry.getRegistry(receiveAddress.getHostAddress(), Constants.RMI_PORT);
        }

        NodeInterface nodeStub = (NodeInterface) nodeRegistry.lookup("Node");
        nodeStub.downloadFile(file.getPath(), Constants.REPLICATED_FILES_PATH + file.getName(), ownAddress);

        fileHandle.getAvailableNodes().add(receiveAddress.equals(ownAddress) ? prevHash : nodeStub.getOwnHash());

        FileHandle newFileHandle = fileHandle.getAsReplicated();
        nodeStub.addReplicatedFileList(newFileHandle);

        if (receiveAddress.equals(ownAddress)) {
            ownerFiles.put(newFileHandle.getFile().getName(), fileHandle);
        } else {
            nodeStub.addOwnerFileList(newFileHandle);
        }
    }

    /**
     * Transfer all replicated and process all local files. Then leave the network.
     */
    public void shutdown() {
        multicastServer.stop();

        if (!(prevHash == ownHash)) {
            // If alone in the network, just leave

            NodeInterface prevNodeStub = null;
            try {
                Registry prevNodeRegistry = LocateRegistry.getRegistry(prevAddress.getHostAddress(), Constants.RMI_PORT);
                prevNodeStub = (NodeInterface) prevNodeRegistry.lookup("Node");
            } catch (RemoteException | NotBoundException e) {
                e.printStackTrace();
            }

            // Only one other Node in the network -> always update the other Node and make it owner of the file
            if (prevHash == nextHash) {
                // Remove ownHash from availableNodes for all replicatedFiles
                for (Map.Entry<String, FileHandle> entry : replicatedFiles.entrySet()) {
                    try {
                        if (prevNodeStub == null) {
                            continue;
                        }
                        prevNodeStub.removeFromAvailableNodes(entry.getKey(), ownHash);
                        prevNodeStub.addOwnerFileList(entry.getValue());
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }

                // Process all local files
                for (Map.Entry<String, FileHandle> localEntry : (new HashMap<>(localFiles)).entrySet()) {
                    try {
                        int downloads;
                        if (prevNodeStub == null) {
                            continue;
                        }

                        // If this Node is the owner of the file -> check downloads and proceed
                        if (ownerFiles.containsKey(localEntry.getKey())) {
                            downloads = localEntry.getValue().getDownloads();
                        } else {
                            // The other Node is the owner -> check downloads there and proceed
                            downloads = prevNodeStub.getReplicatedFiles().get(localEntry.getKey()).getDownloads();
                        }

                        // If downloads = 0 -> delete local copy and copy of owner
                        if (downloads == 0) {
                            prevNodeStub.deleteFileFromNode(localEntry.getValue());
                            deleteFileFromNode(localEntry.getValue());
                        } else {
                            // Else update download locations in the FileHandle
                            prevNodeStub.removeFromAvailableNodes(localEntry.getKey(), ownHash);
                            prevNodeStub.addOwnerFileList(localEntry.getValue());
                        }
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }
            } else {
                // Not alone in the network

                // Transfer all replicated files
                for (Map.Entry<String, FileHandle> entry : replicatedFiles.entrySet()) {
                    try {
                        FileHandle replicatedFileHandle = entry.getValue().getAsReplicated();

                        if (prevNodeStub.getLocalFiles().containsValue(entry.getValue())) {
                            // Previous Node has the file as local file -> replicate to previous' previous neighbour and make it owner of the file
                            replicatedFileHandle.getAvailableNodes().remove(ownHash);
                            replicatedFileHandle.getAvailableNodes().add(prevNodeStub.getPrevHash());
                            prevNodeStub.addOwnerFileList(replicatedFileHandle);

                            Registry prevPrevNodeRegistry = LocateRegistry.getRegistry(prevNodeStub.getPrevAddress().getHostAddress(), Constants.RMI_PORT);
                            NodeInterface prevPrevNodeStub = (NodeInterface) prevPrevNodeRegistry.lookup("Node");

                            prevPrevNodeStub.downloadFile(Constants.REPLICATED_FILES_PATH + entry.getKey(), Constants.REPLICATED_FILES_PATH + entry.getKey(), ownAddress);
                            prevPrevNodeStub.addReplicatedFileList(replicatedFileHandle);
                        } else {
                            // Replicate to previous neighbour, it becomes the new owner of the file
                            prevNodeStub.downloadFile(Constants.REPLICATED_FILES_PATH + entry.getKey(), Constants.REPLICATED_FILES_PATH + entry.getKey(), ownAddress);
                            prevNodeStub.addReplicatedFileList(replicatedFileHandle);
                            prevNodeStub.addOwnerFileList(replicatedFileHandle);
                            prevNodeStub.removeFromAvailableNodes(entry.getKey(), ownHash);
                            prevNodeStub.addToAvailableNodes(entry.getKey(), prevHash);
                        }
                    } catch (RemoteException | NotBoundException e) {
                        e.printStackTrace();
                    }
                }

                // Process all local files
                for (Map.Entry<String, FileHandle> localEntry : (new HashMap<>(localFiles)).entrySet()) {
                    try {
                        // Get ownerAddress from NamingServer via RMI.
                        Registry namingServerRegistry = LocateRegistry.getRegistry(namingServerAddress.getHostAddress(), Constants.RMI_PORT);
                        NamingServerInterface namingServerStub = (NamingServerInterface) namingServerRegistry.lookup("NamingServer");
                        InetAddress ownerAddress = namingServerStub.getOwner(localEntry.getKey());

                        // Contact owner
                        Registry ownerNodeRegistry = LocateRegistry.getRegistry(ownerAddress.getHostAddress(), Constants.RMI_PORT);
                        NodeInterface ownerNodeStub = (NodeInterface) ownerNodeRegistry.lookup("Node");
//                        TODO: What to do is leaving Node is the owner -> where to update the handle to?

                        // If download count = 0 -> delete local copy and copy of owner
                        FileHandle fileHandle = ownerNodeStub.getReplicatedFiles().get(localEntry.getKey());
                        if (fileHandle != null) {
                            if (ownerNodeStub.getReplicatedFiles().get(localEntry.getKey()).getDownloads() == 0) {
                                ownerNodeStub.deleteFileFromNode(localEntry.getValue());
                                deleteFileFromNode(localEntry.getValue());
                            } else {
                                // Else update download locations in the FileHandle
                                ownerNodeStub.removeFromAvailableNodes(localEntry.getKey(), ownHash);
                            }
                        }

                    } catch (RemoteException | UnknownHostException | NotBoundException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        // Actually leave the network
        try {
            tcpServer.stop();
            setRunning(false);
            leaveNetwork();
            UnicastRemoteObject.unexportObject(this, true);
            System.out.println("Left the network+");
            System.exit(0);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Should be run at Node startup.
     * <p>
     * Export self to local RMI registry.
     */
    public void initializeRMI() {
        try {
            System.setProperty("java.rmi.server.hostname", ownAddress.getHostAddress());
            Registry registry = LocateRegistry.createRegistry(Constants.RMI_PORT);
            NodeInterface nodeStub = (NodeInterface) UnicastRemoteObject.exportObject(this, 0);
            registry.bind("Node", nodeStub);
        } catch (AlreadyBoundException | RemoteException e) {
            e.printStackTrace();
            //TODO: failure?
        }
    }

    /**
     * Clears all files and subfolders in a specified folder.
     *
     * @param folderPath to clear
     */
    public void clearDir(String folderPath) {
        File folder = new File(folderPath);
        if (!folder.exists())
            folder.mkdir();
        for (File file : folder.listFiles()) {
            if (file.isDirectory()) {
                clearDir(file.getPath());
            }
            file.delete();
        }
        System.out.println("Cleared dir " + folderPath);
    }

    public void setupMulticastServer() {
        multicastServer = new MulticastServer();

        multicastServer.registerListener(HelloPacket.class, ((packet, client) -> {
            if (packet.getSenderHash() != getOwnHash()) {
                updateNeighbours(client.getAddress(), packet.getSenderHash());

                replicateNewOwnerFiles();
            }
        }));

        multicastServer.startServer(multicastGroup, Constants.MULTICAST_PORT);
    }

    public void setupTCPServer() {
        tcpServer = new TCPServer();

        tcpServer.registerListener(NodeCountPacket.class, ((packet, client) -> {
            setNamingServerAddress(client.getAddress());
            if (packet.getNodeCount() < 1) {
                updatePrev(getOwnAddress(), getOwnHash());
                updateNext(getOwnAddress(), getOwnHash());
            }
            client.close();
        }));

        tcpServer.registerListener(UpdateNeighboursPacket.class, (((packet, client) -> {
            if (packet.getPreviousNeighbour() != -1) {
                updatePrev(null, packet.getPreviousNeighbour());
            }

            if (packet.getNextNeighbour() != -1) {
                updateNext(null, packet.getNextNeighbour());
            }
            client.close();
        })));

        tcpServer.registerListener(FileRequestPacket.class, ((packet, client) -> sendFile(packet.getFileName(), client)));

        tcpServer.startServer(ownAddress, Constants.TCP_PORT);
    }

    private void sendFile(String fileName, Client client) {
        try {
            int fileSize = (int) new File(fileName).length();
            FileInputStream fis = new FileInputStream(fileName);
            byte[] buffer = new byte[4096];
            DataOutputStream dos = client.getConnection().getDataOutputStream();

            int read;

            dos.writeInt(fileSize);

            while ((read = fis.read(buffer)) > 0) {
                dos.write(buffer, 0, read);
            }

            fis.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static Node startNode(String nodeName, InetAddress address, boolean isGUIStarted) {
        Node node;
        try {
            node = new Node(nodeName, address, isGUIStarted);
        } catch (UnknownHostException e) {
            e.printStackTrace();
            System.err.println("Unknown multicast address, cannot create Node.");
            return null;
        }

        node.clearDir(Constants.REPLICATED_FILES_PATH);
        node.clearDir(Constants.DOWNLOADED_FILES_PATH);
        node.initializeRMI();
        System.out.println("Hash: " + node.getOwnHash());
        Communications.setSenderHash(node.getOwnHash());
        node.setupMulticastServer();
        node.setupTCPServer();
        try {
            node.joinNetwork();
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Unable to join network.");
            return null;
        }


        // Discover local files
        while (node.namingServerAddress == null || node.prevHash == 0 || node.nextHash == 0 || node.prevAddress == null || node.nextAddress == null) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
                System.err.println("Interrupted while waiting for Node to initialize.");
                return null;
            }
        }
        node.setInitialized(true);
        node.discoverLocalFiles();

        // Start the FileAgent if this is the first Node in the network
        if (node.prevHash == node.ownHash) {
            HashMap<String, Integer> files = new HashMap<>();
            try {
                Thread.sleep(5000);
                node.runFileAgent(files);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }


        // Start file update watcher
        FileUpdateWatcher fileUpdateWatcherThread = new FileUpdateWatcher(node, Constants.LOCAL_FILES_PATH);
        Thread thread = new Thread(fileUpdateWatcherThread);
        thread.start();

        return node;
    }

    public static void main(String[] args) throws IOException {
        // Get IP and hostname
        Scanner sc = new Scanner(System.in);
        System.out.println("(Detected localHostName is: " + InetAddress.getLocalHost() + ")");
        System.out.print("Enter hostname: ");
        String hostname = sc.nextLine();
        if (hostname.isEmpty()) {
            hostname = InetAddress.getLocalHost().getHostName();
        }
        System.out.println("(Detected localHostAddress is: " + InetAddress.getLocalHost() + ")");
        System.out.print("Enter IP: ");
        String ip = sc.nextLine();
        if (ip.isEmpty()) {
            ip = InetAddress.getLocalHost().getHostAddress();
        }


        // Start the Node
        Node node = Node.startNode(hostname, InetAddress.getByName(ip), false);
        if (node == null) {
            return;
        }

        // Listen for commands
        while (node.isRunning) {
            String cmd = sc.nextLine().toLowerCase();
            switch (cmd) {
                case "debug":
                    Communications.setDebugging(true);
                    System.out.println("Debugging enabled");
                    break;

                case "undebug":
                    Communications.setDebugging(false);
                    System.out.println("Debugging disabled");
                    break;

                case "shutdown":
                case "shut":
                case "sh":
                    node.setRunning(false);
                    node.leaveNetwork();
                    node.tcpServer.stop();
                    node.multicastServer.stop();
                    UnicastRemoteObject.unexportObject(node, true);
                    System.out.println("Left the network");
                    System.exit(0);
                    break;

                case "shutdown+":
                case "shut+":
                case "sh+":
                    node.shutdown();
                    break;

                case "neighbours":
                case "neighbors":
                case "neigh":
                case "nb":
                    System.out.println("Prev: " + node.prevHash + " === Next: " + node.nextHash);
                    break;

                case "localFiles":
                case "lf":
                    System.out.println("Local files: " + node.localFiles);
                    break;

                case "replicatedFiles":
                case "rf":
                    System.out.println("Replicated files: " + node.replicatedFiles);
                    break;

                case "ownerFiles":
                case "of":
                    System.out.println("Owner files: " + node.ownerFiles);
                    break;

                case "allfiles":
                    System.out.println("All files: " + node.allFiles);
                    break;

                case "fafiles":
                    System.out.println("All fileagentfiles: " + node.fileAgentFiles);
                    break;

                case "dl":
                    System.out.print("Enter filename to download: ");
                    node.downloadAFile(sc.nextLine());
                    break;

            }
        }
    }
}
