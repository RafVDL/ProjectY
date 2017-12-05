package be.ac.ua.dist.systemy.node;

import be.ac.ua.dist.systemy.Constants;
import be.ac.ua.dist.systemy.namingServer.NamingServerInterface;
import be.ac.ua.dist.systemy.networking.Client;
import be.ac.ua.dist.systemy.networking.NetworkManager;
import be.ac.ua.dist.systemy.networking.Server;
import be.ac.ua.dist.systemy.networking.packet.*;
import be.ac.ua.dist.systemy.networking.tcp.TCPServer;
import be.ac.ua.dist.systemy.networking.udp.MulticastServer;
import be.ac.ua.dist.systemy.networking.udp.UnicastServer;

import java.io.*;
import java.net.*;
import java.rmi.AlreadyBoundException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.ServerNotActiveException;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;

public class Node implements NodeInterface {

    private final InetAddress ownAddress;
    private final int ownHash;
    private Map<String, FileHandle> localFiles;
    private Map<String, FileHandle> replicatedFiles;
    private Set<String> downloadingFiles;

    private volatile InetAddress namingServerAddress;
    private InetAddress prevAddress;
    private InetAddress nextAddress;

    private volatile int prevHash;
    private volatile int nextHash;

    private boolean running = true;

    private MulticastSocket multicastSocket;
    private InetAddress multicastGroup;

    public Node(String nodeName, InetAddress address) throws IOException {
        this.ownAddress = address;
        this.ownHash = calculateHash(nodeName);

        multicastSocket = new MulticastSocket(Constants.MULTICAST_PORT);
        multicastGroup = InetAddress.getByName(Constants.MULTICAST_ADDRESS);
    }

    public InetAddress getOwnAddress() {
        return ownAddress;
    }

    @Override
    public int getOwnHash() {
        return ownHash;
    }

    public void setNamingServerAddress(InetAddress ipAddress) {
        this.namingServerAddress = ipAddress;
    }

    public InetAddress getNamingServerAddress() {
        return namingServerAddress;
    }

    @Override
    public InetAddress getPrevAddress() {
        return prevAddress;
    }

    @Override
    public InetAddress getNextAddress() {
        return nextAddress;
    }

    public int getPrevHash() {
        return prevHash;
    }

    public int getNextHash() {
        return nextHash;
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
    public void downloadFile(String remoteFileName, String localFileName, InetAddress remoteAddress) {
        Socket clientSocket;

        downloadingFiles.add(localFileName.split("/")[1]);
        System.out.println("Adding " + localFileName + " to downloading files");
        try {
            //Open tcp socket to server @remoteAddress:port
            clientSocket = new Socket(remoteAddress, Constants.TCP_PORT);
            PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
            DataInputStream in = new DataInputStream(clientSocket.getInputStream());
            FileOutputStream fos = new FileOutputStream(localFileName);

            //Request file at the server
            out.println("REQUESTFILE");
            out.println(remoteFileName);
            //Initialize buffers
            int fileSize = in.readInt();
            byte[] buffer = new byte[1024 * 10]; // 10 kB
            int read;
            int remaining = fileSize;
            //Receive the file
            while ((read = in.read(buffer, 0, Math.min(buffer.length, remaining))) > 0) {
                remaining -= read;
                fos.write(buffer, 0, read);
            }

            //Close everything
            fos.close();
            in.close();
            out.close();
            clientSocket.close();
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
                Client client = NetworkManager.getTCPClient(newAddress, Constants.TCP_PORT);
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
                Client client = NetworkManager.getTCPClient(newAddress, Constants.TCP_PORT);
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
        Client client = NetworkManager.getUDPClient(multicastGroup, Constants.MULTICAST_PORT);

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
        byte[] buf;
        buf = ("HELLO|" + ownHash).getBytes();
        DatagramPacket packet = new DatagramPacket(buf, buf.length, multicastGroup, Constants.MULTICAST_PORT);
        multicastSocket.send(packet);

        multicastSocket.joinGroup(multicastGroup);
    }

    public void leaveNetwork() throws IOException {
        byte[] buf;
        buf = ("QUITNAMING|" + ownHash).getBytes();
        DatagramPacket packet = new DatagramPacket(buf, buf.length, multicastGroup, Constants.MULTICAST_PORT);
        multicastSocket.send(packet);
        multicastSocket.leaveGroup(multicastGroup);
        multicastSocket.close();

        if (ownHash == nextHash && ownHash == prevHash)
            return;

        Socket clientSocket;
        DataOutputStream dos;
        PrintWriter out;
        try {
            clientSocket = new Socket();
            clientSocket.setSoLinger(true, 5);
            clientSocket.connect(new InetSocketAddress(prevAddress, Constants.TCP_PORT), 1000);
            dos = new DataOutputStream(clientSocket.getOutputStream());
            out = new PrintWriter(dos, true);

            //Send neighbour update command.
            out.println("QUIT");
            //Send neighbours
            dos.writeInt(ownHash);
            dos.writeInt(nextHash);

            //Close everything.
            out.close();
            clientSocket.close();
        } catch (SocketTimeoutException e) {
            // handle node disconnected
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (prevHash == nextHash) {
            return;
        }

        try {
            clientSocket = new Socket();
            clientSocket.setSoLinger(true, 5);
            clientSocket.connect(new InetSocketAddress(nextAddress, Constants.TCP_PORT), 1000);
            dos = new DataOutputStream(clientSocket.getOutputStream());
            out = new PrintWriter(dos, true);

            //Send neighbour update command.
            out.println("QUIT");
            //Send neighbours
            dos.writeInt(ownHash);
            dos.writeInt(prevHash);

            //Close everything.
            dos.close();
            clientSocket.close();
        } catch (SocketTimeoutException e) {
            // handle node disconnected
        } catch (IOException e) {
            e.printStackTrace();
        }
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
     * Iterates through a folder and returns a Set containing all filenames including extensions.
     *
     * @param folderPath to explore
     * @return the List containing the filenames
     */
    public void discoverFiles(String folderPath) {
        File folder = new File(folderPath);
        if (!folder.exists())
            folder.mkdir();
        File[] listOfFiles = folder.listFiles();
        if (listOfFiles == null) {
            //TODO call failure?
            System.out.println("Folder path does not point to a folder (" + folderPath + ")");
            return;
        }

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

    /**
     * Introduces a file in the network.
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
            // Get ownerAddress from NamingServer via RMI.
            Registry namingServerRegistry = LocateRegistry.getRegistry(namingServerAddress.getHostAddress(), Constants.RMI_PORT);
            NamingServerInterface namingServerStub = (NamingServerInterface) namingServerRegistry.lookup("NamingServer");
            InetAddress ownerAddress = namingServerStub.getOwner(file.getName());

            // Put in local copy in localFiles and update the FileHandle
            localFiles.put(fileHandle.getFile().getName(), fileHandle);
            fileHandle.setLocal(true);
            fileHandle.getAvailableNodes().add(ownHash);

            if (ownerAddress == null || ownAddress.equals(nextAddress))
                return;

            if (ownerAddress.equals(ownAddress)) {
                // Replicate to previous neighbour -> initiate downloadFile via RMI and update its replicatedFiles.
                Registry nodeRegistry = LocateRegistry.getRegistry(prevAddress.getHostAddress(), Constants.RMI_PORT);
                NodeInterface nodeStub = (NodeInterface) nodeRegistry.lookup("Node");
                nodeStub.downloadFile(Constants.LOCAL_FILES_PATH + file.getName(), Constants.REPLICATED_FILES_PATH + file.getName(), ownAddress);
                FileHandle newFileHandle = new FileHandle(file.getName(), false);
                fileHandle.getAvailableNodes().add(prevHash);
                newFileHandle.getAvailableNodes().addAll(fileHandle.getAvailableNodes());
                nodeStub.addReplicatedFileList(newFileHandle);
            } else {
                // Replicate to owner -> initiate downloadFile via RMI and update its replicatedFiles.
                Registry nodeRegistry = LocateRegistry.getRegistry(ownerAddress.getHostAddress(), Constants.RMI_PORT);
                NodeInterface nodeStub = (NodeInterface) nodeRegistry.lookup("Node");
                nodeStub.downloadFile(Constants.LOCAL_FILES_PATH + file.getName(), Constants.REPLICATED_FILES_PATH + file.getName(), ownAddress);
                FileHandle newFileHandle = new FileHandle(file.getName(), false);
                fileHandle.getAvailableNodes().add(nodeStub.getOwnHash());
                newFileHandle.getAvailableNodes().addAll(fileHandle.getAvailableNodes());
                nodeStub.addReplicatedFileList(newFileHandle);
            }
        } catch (IOException | NotBoundException e) {
            e.printStackTrace();
        }
    }

    /**
     * Transfer all replicated and process all local files. Then leave the network.
     */
    public void shutdown() {
        //TODO: Put a lock on the Node if it is in the process of leaving. This ensures no new files are replicated onto the Node.
        // Transfer all replicated files
        for (Map.Entry<String, FileHandle> entry : replicatedFiles.entrySet()) {
            try {
                Registry prevNodeRegistry = LocateRegistry.getRegistry(prevAddress.getHostAddress(), Constants.RMI_PORT);
                NodeInterface prevNodeStub = (NodeInterface) prevNodeRegistry.lookup("Node");

                FileHandle replicatedFileHandle = new FileHandle(entry.getKey(), false);

                if (prevNodeStub.getReplicatedFiles().containsValue(entry.getValue())) {
                    // Previous Node is already owner -> replicate to previous' previous neighbour
                    Registry prevPrevNodeRegistry = LocateRegistry.getRegistry(prevNodeStub.getPrevAddress().getHostAddress(), Constants.RMI_PORT);
                    NodeInterface prevPrevNodeStub = (NodeInterface) prevPrevNodeRegistry.lookup("Node");

                    prevPrevNodeStub.downloadFile(Constants.REPLICATED_FILES_PATH + entry.getKey(), Constants.REPLICATED_FILES_PATH + entry.getKey(), ownAddress);
                    prevPrevNodeStub.addReplicatedFileList(replicatedFileHandle);
                } else {
                    // Replicate to previous neighbour
                    prevNodeStub.downloadFile(Constants.REPLICATED_FILES_PATH + entry.getKey(), Constants.REPLICATED_FILES_PATH + entry.getKey(), ownAddress);
                    prevNodeStub.addReplicatedFileList(replicatedFileHandle);
                }
            } catch (RemoteException | NotBoundException e) {
                e.printStackTrace();
            }
        }

        // Process all local files
        for (Map.Entry<String, FileHandle> localEntry : localFiles.entrySet()) {
            try {
                // Get ownerAddress from NamingServer via RMI.
                Registry namingServerRegistry = LocateRegistry.getRegistry(namingServerAddress.getHostAddress(), Constants.RMI_PORT);
                NamingServerInterface namingServerStub = (NamingServerInterface) namingServerRegistry.lookup("NamingServer");
                InetAddress ownerAddress = namingServerStub.getOwner(localEntry.getKey());

                // Contact owner
                Registry ownerNodeRegistry = LocateRegistry.getRegistry(ownerAddress.getHostAddress(), Constants.RMI_PORT);
                NodeInterface ownerNodeStub = (NodeInterface) ownerNodeRegistry.lookup("Node");

                // If download count = 0 -> delete local copy and copy of owner
                if (ownerNodeStub.getReplicatedFiles().get(localEntry.getKey()).getDownloads() == 0) {
                    ownerNodeStub.deleteFileFromNode(ownerNodeStub.getReplicatedFiles().get(localEntry.getKey()));
                    deleteFileFromNode(localEntry.getValue());
                } else {
                    // Else update download locations in the FileHandle
                    ownerNodeStub.getReplicatedFiles().get(localEntry.getKey()).getAvailableNodes().remove(ownHash);
                }

            } catch (RemoteException | UnknownHostException | NotBoundException e) {
                e.printStackTrace();
            }
        }

        // Actually leave the network
        try {
            setRunning(false);
            leaveNetwork();
            System.out.println("Left the network+");
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
        Server multicastServer = new MulticastServer();

        multicastServer.registerListener(HelloPacket.class, ((packet, client) -> {
            if (packet.getSenderHash() != getOwnHash())
                updateNeighbours(client.getAddress(), packet.getSenderHash());
        }));

        multicastServer.startServer(multicastGroup, Constants.MULTICAST_PORT);
    }

    public void setupTCPServer() {
        Server tcpServer = new TCPServer();

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
                updatePrev(null, packet.getNextNeighbour());
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

    public static void main(String[] args) throws IOException, NotBoundException, ServerNotActiveException {
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
        node.downloadingFiles = new HashSet<>();

        node.initializeRMI();
        System.out.println("Hash: " + node.getOwnHash());

        NetworkManager.setSenderHash(node.getOwnHash());

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
        node.discoverFiles(Constants.LOCAL_FILES_PATH);


        // Start file update watcher
        FileUpdateWatcher fileUpdateWatcherThread = new FileUpdateWatcher(node, Constants.LOCAL_FILES_PATH);
        Thread thread = new Thread(fileUpdateWatcherThread);
        thread.start();


        // Listen for commands
        while (node.running) {
            String cmd = sc.nextLine().toLowerCase();
            switch (cmd) {
                case "shutdown":
                case "shut":
                case "sh":
                    node.setRunning(false);
                    node.leaveNetwork();
                    System.out.println("Left the network");
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
            }
        }
    }
}
