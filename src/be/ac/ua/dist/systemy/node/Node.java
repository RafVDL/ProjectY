package be.ac.ua.dist.systemy.node;

import be.ac.ua.dist.systemy.nameserver.NameServerPackage.Nameserver;

import java.io.*;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.Socket;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.ServerNotActiveException;
import java.util.List;

public class Node implements NodeInterface {
    public static final int PORT = 6969;

    private String currentName;
    private InetAddress currentAddress;
    private int currentHash;
    private List<String> localFiles;
    private List<String> replicatedFiles;
    private List<String> downloadedFiles;
    private InetAddress prevAddress;
    private InetAddress nextAddress;
    private String prevName;
    private String nextName;
    private int prevHash;
    private int nextHash;

    public Node(String nodeName, InetAddress address) {
        this.currentName = nodeName;
        this.currentAddress = address;
        this.currentHash = calculateHash(nodeName);

        NodeTCPServer tcpServer = new NodeTCPServer();
        tcpServer.startServer();
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

    public void downloadFile(String fileName) {
        InetAddress remoteAddress;
        Socket clientSocket;

        try {
            //TODO get ip via rmi
            remoteAddress = InetAddress.getByName("10.10.10.10");

            //Open tcp socket to server @remoteAddress:port
            clientSocket = new Socket(remoteAddress, PORT);
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

    private void updateNext(InetAddress newAddress, String newName) {
        nextHash = calculateHash(newName);
        nextAddress = newAddress;
        nextName = newName;
    }

    private void updatePrev(InetAddress newAddress, String newName) {
        prevHash = calculateHash(newName);
        prevAddress = newAddress;
        prevName = newName;
    }

    public void updateNeighbours(InetAddress newAddress, String newName) {
        int newHash = calculateHash(newName);

        if (newHash > currentHash && newHash < nextHash) {
            // New node sits between this node en next node.

            //TODO Send self and nextNode to newNode.
            updateNext(newAddress, newName);
        } else if (newHash < currentHash && newHash > prevHash) {
            // New node sits between this node and the previous node.

            //TODO Send previous node and self to newNode.
            updatePrev(newAddress, newName);
        } else {
            // New node does not become a neighbour of this node.

            // Do nothing
        }
    }

    public void joinNetwork() throws IOException {
        MulticastSocket socket = new MulticastSocket(4446);
        InetAddress group = InetAddress.getByName("203.0.113.0");
        socket.joinGroup(group);


        byte[] buf;
        buf = ("HELLO|" + currentName).getBytes();
        DatagramPacket packet = new DatagramPacket(buf, buf.length, group, 4446);
        socket.send(packet);

        buf = new byte[256];
        packet = new DatagramPacket(buf, buf.length);
        socket.receive(packet);

        boolean done = false;
        String received = new String(buf);
        if (received.startsWith("NODECOUNT")) {
            String[] split = received.split("\\|");
            Integer nodeCount = Integer.parseInt(split[1]);
            if (nodeCount < 1) {
                updateNext(currentAddress, currentName);
                updatePrev(currentAddress, currentName);
            }
        }

        socket.close();
    }

    private int calculateHash(String name) {
        return Math.abs(name.hashCode() % 32768);
    }

    public static void main(String[] args) throws IOException, NotBoundException, ServerNotActiveException {
        Registry registry = LocateRegistry.getRegistry("192.168.137.1", 3733);
        Nameserver stub = (Nameserver) registry.lookup("be.ac.ua.dist.systemy.nameserver.NameServerPackage.NamingServer");

        stub.addMeToNetwork("e");
        stub.printIPadresses();
        stub.getOwner("test.txt");
        stub.exportIPadresses();
        stub.removeMeFromNetwork("e");
        stub.printIPadresses();


    }
}