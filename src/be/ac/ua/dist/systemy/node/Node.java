package be.ac.ua.dist.systemy.node;

import be.ac.ua.dist.systemy.Ports;
import be.ac.ua.dist.systemy.namingServer.NamingServerInterface;

import java.io.*;
import java.net.*;
import java.rmi.AlreadyBoundException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.ServerNotActiveException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Node implements NodeInterface {

    private final InetAddress ownAddress;
    private final int ownHash;
    private List<String> localFiles;
    private List<String> replicatedFiles;
    private List<String> downloadedFiles;

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

        multicastSocket = new MulticastSocket(Ports.MULTICAST_PORT);
        multicastGroup = InetAddress.getByName("225.0.113.0");
    }

    public InetAddress getOwnAddress() {
        return ownAddress;
    }

    public int getOwnHash() {
        return ownHash;
    }

    public void setNamingServerAddress(InetAddress ipAddress) {
        this.namingServerAddress = ipAddress;
    }

    public InetAddress getNamingServerAddress() {
        return namingServerAddress;
    }

    public InetAddress getPrevAddress() {
        return prevAddress;
    }

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
    public List<String> getLocalFileList() throws RemoteException {
        return localFiles;
    }

    @Override
    public List<String> getReplicatedFileList() throws RemoteException {
        return replicatedFiles;
    }

    @Override
    public List<String> getDownloadedFileList() throws RemoteException {
        return downloadedFiles;
    }

    public void downloadFile(String fileName, InetAddress remoteAddress) {
        Socket clientSocket;

        try {
            //Open tcp socket to server @remoteAddress:port
            clientSocket = new Socket(remoteAddress, Ports.TCP_PORT);
            PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
            DataInputStream in = new DataInputStream(clientSocket.getInputStream());
            FileOutputStream fos = new FileOutputStream(fileName);

            //Request file at the server
            out.println("REQUESTFILE");
            out.println(fileName);
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
     * Gets invoked when a new Node is joining the network. (via NodeMultiCastServer)
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
                Socket clientSocket = new Socket();
                clientSocket.setSoLinger(true, 5);
                clientSocket.connect(new InetSocketAddress(newAddress, Ports.TCP_PORT));
                sendTcpCmd(clientSocket, "PREV_NEXT_NEIGHBOUR", ownHash, ownHash);
                clientSocket.close();
            } catch (IOException e) {
                handleFailure(newHash);
                e.printStackTrace();
            }

            updatePrev(newAddress, newHash);
            updateNext(newAddress, newHash);

        } else if ((prevHash < ownHash && newHash < ownHash && newHash > prevHash)
                || (prevHash > ownHash && newHash < ownHash)
                || (prevHash >= nextHash && newHash > prevHash && newHash < ownHash)) {
            // Joining Node sits between previous neighbour and this Node.

            updatePrev(newAddress, newHash);
        } else if ((nextHash > ownHash && newHash > ownHash && newHash < nextHash)
                || (nextHash < ownHash && newHash > ownHash)
                || (nextHash <= prevHash && newHash < nextHash)) {
            // Joining Node sits between this Node and next neighbour.

            try {
                Socket clientSocket = new Socket();
                clientSocket.setSoLinger(true, 5);
                clientSocket.connect(new InetSocketAddress(newAddress, Ports.TCP_PORT));
                sendTcpCmd(clientSocket, "PREV_NEXT_NEIGHBOUR", ownHash, nextHash);
                clientSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
                handleFailure(newHash);
            }

            updateNext(newAddress, newHash);

        }
    }

    /**
     * Sends a command via tcp with optional extra String parameters.
     *
     * @param socket to use for sending
     * @param cmd    to send
     * @param args   to include (optional)
     */
    public void sendTcpCmd(Socket socket, String cmd, String... args) {
        try {
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            out.println(cmd);
            for (String arg : args) {
                out.println(arg);
            }

            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendTcpCmd(Socket socket, String cmd, int... args) {
        try {
            DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
            PrintWriter out = new PrintWriter(dos, true);
            out.println(cmd);
            for (int arg : args) {
                dos.writeInt(arg);
            }

            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private InetAddress getAddressByHash(int hash) throws IOException {
        byte[] buf;
        buf = ("GETIP|" + hash).getBytes();
        DatagramPacket packet = new DatagramPacket(buf, buf.length, multicastGroup, Ports.MULTICAST_PORT);
        multicastSocket.send(packet);

        DatagramSocket uniSocket = new DatagramSocket(Ports.UNICAST_PORT, ownAddress);

        buf = new byte[256];
        packet = new DatagramPacket(buf, buf.length);
        uniSocket.receive(packet);

        String received = new String(buf).trim();
        uniSocket.close();

        if (received.startsWith("REIP")) {
            String[] split = received.split("\\|");
            String returnedHostname = split[1];
            String ip = split[2];
            if (ip.equals("NOT_FOUND")) {
                return null;
            }
            return InetAddress.getByName(ip);
        }
        return null;
    }

    public void joinNetwork() throws IOException {
        byte[] buf;
        buf = ("HELLO|" + ownHash).getBytes();
        DatagramPacket packet = new DatagramPacket(buf, buf.length, multicastGroup, Ports.MULTICAST_PORT);
        multicastSocket.send(packet);

        multicastSocket.joinGroup(multicastGroup);
    }

    public void leaveNetwork() throws IOException {
        byte[] buf;
        buf = ("QUITNAMING|" + ownHash).getBytes();
        DatagramPacket packet = new DatagramPacket(buf, buf.length, multicastGroup, Ports.MULTICAST_PORT);
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
            clientSocket.connect(new InetSocketAddress(prevAddress, Ports.TCP_PORT), 1000);
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
            clientSocket.connect(new InetSocketAddress(nextAddress, Ports.TCP_PORT), 1000);
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
     * Iterates through a folder and returns a List containing all filenames including extensions.
     *
     * @param folderPath to explore
     * @return the List containing the filenames
     */
    public List<String> discoverFiles(String folderPath) {
        File folder = new File(folderPath);
        if (!folder.exists())
            folder.mkdir();
        File[] listOfFiles = folder.listFiles();
        List<String> fileNames = new ArrayList<>();
        if (listOfFiles == null) {
            //TODO call failure?
            System.out.println("Folder path does not point to a folder (" + folderPath + ")");
            return null;
        }

        for (File file : listOfFiles) {
            if (file.isFile()) {
                System.out.println("Found file " + file.getName());
                fileNames.add(file.getName());
            } else if (file.isDirectory()) {
                System.out.println("Not checking files in nested folder " + file.getName());
            }
        }

        System.out.println("Finished discovery of " + folder.getName());
        return fileNames;
    }

    /**
     * Method should be run at Node startup
     * <p>
     * For each file in the list of local files, the NamingServer gets asked who the owner should be. If this Node should
     * be the owner, the file gets duplicated to the previous neighbour via RMI. If this node should not be the owner, the
     * file gets duplicated to the new owner and this Node updates itself to hold the file as replicated.
     */
    public void replicateFiles() {
        if (prevHash == ownHash && nextHash==ownHash) {
            // This node is the only node in the network and will always be owner of all files.
            return;
        }

        for (String fileName : localFiles) {
            InetAddress ownerAddress;

            try {
                // Get ownerAddress from NamingServer via RMI.
                Registry namingServerRegistry = LocateRegistry.getRegistry(namingServerAddress.getHostAddress(), Ports.RMI_PORT);
                NamingServerInterface namingServerStub = (NamingServerInterface) namingServerRegistry.lookup("NamingServer");
                ownerAddress = namingServerStub.getOwner(fileName);

                if (ownerAddress == null) {
                    continue;
                }

                if (ownerAddress.equals(ownAddress)) {
                    // Replicate to previous neighbour -> initiate downloadFile via RMI and update its replicatedFile List.
                    Registry nodeRegistry = LocateRegistry.getRegistry(prevAddress.getHostAddress(), Ports.RMI_PORT);
                    NodeInterface nodeStub = (NodeInterface) nodeRegistry.lookup("Node");
                    nodeStub.downloadFile(fileName, ownAddress);
                    nodeStub.getReplicatedFileList().add(fileName);
                } else {
                    // Else send copy to new owner and update own replicatedFile List.
                    Registry nodeRegistry = LocateRegistry.getRegistry(ownerAddress.getHostAddress(), Ports.RMI_PORT);
                    NodeInterface nodeStub = (NodeInterface) nodeRegistry.lookup("Node");
                    nodeStub.downloadFile(fileName, ownAddress);
                    localFiles.remove(fileName);
                    replicatedFiles.add(fileName);
                }
            } catch (IOException | NotBoundException e) {
                e.printStackTrace();
            }
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
            Registry registry = LocateRegistry.createRegistry(Ports.RMI_PORT);
            NodeInterface nodeStub = (NodeInterface) UnicastRemoteObject.exportObject(this, 0);
            registry.bind("Node", nodeStub);
        } catch (AlreadyBoundException | RemoteException e) {
            e.printStackTrace();
            //TODO: failure?
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
        node.initializeRMI();
        System.out.println("Hash: " + node.getOwnHash());


        // Start tcp and multiCast servers
        NodeMultiCastServer udpServer = new NodeMultiCastServer(node);
        udpServer.start();
        NodeTCPServer tcpServerThread = new NodeTCPServer(node);
        tcpServerThread.start();
        node.joinNetwork();


        // Discover local files
        while (node.namingServerAddress == null || node.prevHash == 0 || node.nextHash == 0) {
            try {
                System.out.println("Naming: " + node.namingServerAddress);
                System.out.println("Prev: " + node.prevHash);
                System.out.println("Next: " + node.nextHash);
                Thread.sleep(1);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        node.localFiles = node.discoverFiles("localFiles");
        node.replicatedFiles = node.discoverFiles("replicatedFiles");
        node.replicateFiles();


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