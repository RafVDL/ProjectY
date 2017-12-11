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

public class Node implements NodeInterface {

    private final InetAddress ownAddress;
    private final int ownHash;
    private Map<String, FileHandle> localFiles;
    private Map<String, FileHandle> replicatedFiles;
    private Map<String, FileHandle> ownerFiles;
    private Set<String> downloadingFiles;

    private volatile InetAddress namingServerAddress;
    private InetAddress prevAddress;
    private InetAddress nextAddress;

    private volatile int prevHash;
    private volatile int nextHash;

    private boolean running = true;

    private InetAddress multicastGroup;
    private Server multicastServer;
    private Server tcpServer;

    public Node(String nodeName, InetAddress address) throws IOException {
        this.ownAddress = address;
        this.ownHash = calculateHash(nodeName);

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
        localFiles.remove(fileHandle.getFile().getName());
        replicatedFiles.remove(fileHandle.getFile().getName());
        fileHandle.getFile().delete();
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
        if (!localFiles.containsKey(fileName)) {
            System.out.println("Error: trying to update a FileHandle of a non-local file.");
            return;
        }
        localFiles.get(fileName).getAvailableNodes().add(hashToAdd);
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
        if (!localFiles.containsKey(fileName)) {
            System.out.println("Error: trying to update a FileHandle of a non-local file.");
            return;
        }
        localFiles.get(fileName).getAvailableNodes().remove(hashToRemove);
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
            latch.await(1, TimeUnit.SECONDS);
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

    private int calculateHash(String name) {
        return Math.abs(name.hashCode() % 32768);
    }

    public boolean isRunning() {
        return running;
    }

    public void setRunning(boolean running) {
        this.running = running;
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
                addFileToNetwork(fileHandle);
            } else if (file.isDirectory()) {
                System.out.println("Not checking files in nested folder " + file.getName());
            }
        }

        System.out.println("Finished discovery of " + folder.getName());
    }

    public void replicateNewOwnerFiles() throws RemoteException, NotBoundException {
        Map<String, FileHandle> originalOwnerFiles = new HashMap<>(ownerFiles);

        originalOwnerFiles.forEach(((s, fileHandle) -> {
            try {
                replicateFile(fileHandle);
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

            replicateFile(fileHandle);
        } catch (IOException | NotBoundException e) {
            e.printStackTrace();
        }
    }

    public void replicateFile(FileHandle fileHandle) throws RemoteException, NotBoundException, UnknownHostException {
        File file = fileHandle.getFile();

        Registry namingServerRegistry = LocateRegistry.getRegistry(namingServerAddress.getHostAddress(), Constants.RMI_PORT);
        NamingServerInterface namingServerStub = (NamingServerInterface) namingServerRegistry.lookup("NamingServer");
        InetAddress ownerAddress = namingServerStub.getOwner(file.getName());

        if (ownerAddress == null)
            return;

        if (ownAddress.equals(nextAddress)) {
            ownerFiles.put(fileHandle.getFile().getName(), fileHandle);
            return;
        }

        if (ownerAddress.equals(ownAddress)) {
            // Replicate to previous neighbour -> initiate downloadFile via RMI and update its replicatedFiles.
            Registry nodeRegistry = LocateRegistry.getRegistry(prevAddress.getHostAddress(), Constants.RMI_PORT);
            NodeInterface nodeStub = (NodeInterface) nodeRegistry.lookup("Node");
            nodeStub.downloadFile(file.getPath(), Constants.REPLICATED_FILES_PATH + file.getName(), ownAddress);
            fileHandle.getAvailableNodes().add(prevHash);
            FileHandle newFileHandle = fileHandle.getAsReplicated();
            nodeStub.addReplicatedFileList(newFileHandle);
            ownerFiles.put(newFileHandle.getFile().getName(), fileHandle);
        } else {
            // Replicate to owner -> initiate downloadFile via RMI and update its replicatedFiles.
            Registry nodeRegistry = LocateRegistry.getRegistry(ownerAddress.getHostAddress(), Constants.RMI_PORT);
            NodeInterface nodeStub = (NodeInterface) nodeRegistry.lookup("Node");
            nodeStub.downloadFile(file.getPath(), Constants.REPLICATED_FILES_PATH + file.getName(), ownAddress);
            if (prevHash != nextHash)
                fileHandle.getAvailableNodes().remove(ownHash);
            fileHandle.getAvailableNodes().add(nodeStub.getOwnHash());
            FileHandle newFileHandle = fileHandle.getAsReplicated();
            nodeStub.addReplicatedFileList(newFileHandle);
            nodeStub.addOwnerFileList(newFileHandle);
            ownerFiles.remove(fileHandle.getFile().getName());
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

            // Only one other Node in the network -> always update the other Node
            if (prevHash == nextHash) {
                // Remove ownHash from availableNodes for all replicatedFiles
                for (Map.Entry<String, FileHandle> entry : replicatedFiles.entrySet()) {
                    try {
                        if (prevNodeStub == null) {
                            continue;
                        }
                        prevNodeStub.removeFromAvailableNodes(entry.getKey(), ownHash);
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
                        }
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }
            } else {

                // Transfer all replicated files
                for (Map.Entry<String, FileHandle> entry : replicatedFiles.entrySet()) {
                    try {
                        FileHandle replicatedFileHandle = entry.getValue().getAsReplicated();

                        if (prevNodeStub.getLocalFiles().containsValue(entry.getValue())) {
                            // Previous Node has the file as local file -> replicate to previous' previous neighbour
                            prevNodeStub.removeFromAvailableNodes(entry.getKey(), ownHash);
                            prevNodeStub.addToAvailableNodes(entry.getKey(), prevNodeStub.getPrevHash());

                            Registry prevPrevNodeRegistry = LocateRegistry.getRegistry(prevNodeStub.getPrevAddress().getHostAddress(), Constants.RMI_PORT);
                            NodeInterface prevPrevNodeStub = (NodeInterface) prevPrevNodeRegistry.lookup("Node");

                            prevPrevNodeStub.downloadFile(Constants.LOCAL_FILES_PATH + entry.getKey(), Constants.REPLICATED_FILES_PATH + entry.getKey(), ownAddress);
                            prevPrevNodeStub.addReplicatedFileList(replicatedFileHandle);
                        } else {
                            // Replicate to previous neighbour, it becomes the new owner of the file
                            removeFromAvailableNodes(entry.getKey(), ownHash);
                            addToAvailableNodes(entry.getKey(), prevHash);
                            prevNodeStub.downloadFile(Constants.REPLICATED_FILES_PATH + entry.getKey(), Constants.REPLICATED_FILES_PATH + entry.getKey(), ownAddress);
                            prevNodeStub.addReplicatedFileList(replicatedFileHandle);
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
                try {
                    replicateNewOwnerFiles();
                } catch (NotBoundException e) {
                    e.printStackTrace();
                }

                updateNeighbours(client.getAddress(), packet.getSenderHash());
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


        // Create Node object and initialize
        Node node = new Node(hostname, InetAddress.getByName(ip));

        node.clearDir(Constants.REPLICATED_FILES_PATH);
        node.localFiles = new HashMap<>();
        node.replicatedFiles = new HashMap<>();
        node.ownerFiles = new HashMap<>();
        node.downloadingFiles = new HashSet<>();

        node.initializeRMI();
        System.out.println("Hash: " + node.getOwnHash());

        Communications.setSenderHash(node.getOwnHash());

        node.setupMulticastServer();
        node.setupTCPServer();

        node.joinNetwork();


        // Discover local files
        while (node.namingServerAddress == null || node.prevHash == 0 || node.nextHash == 0) {
            try {
                Thread.sleep(500);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        node.discoverLocalFiles();


        // Start file update watcher
        FileUpdateWatcher fileUpdateWatcherThread = new FileUpdateWatcher(node, Constants.LOCAL_FILES_PATH);
        Thread thread = new Thread(fileUpdateWatcherThread);
        thread.start();


        // Listen for commands
        while (node.running) {
            String cmd = sc.nextLine().toLowerCase();
            switch (cmd) {
                case "debug":
                    Communications.setDebugging(true);
                    System.out.println("Debugging enabled");
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
            }
        }
    }
}
