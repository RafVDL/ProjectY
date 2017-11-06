package be.ac.ua.dist.systemy.node;

import be.ac.ua.dist.systemy.Ports;

import java.io.DataInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.*;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.server.ServerNotActiveException;
import java.util.List;
import java.util.Scanner;

public class Node implements NodeInterface {

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

    private boolean running = true;

    MulticastSocket socket;
    InetAddress group;

    public Node(String nodeName, InetAddress address) throws IOException {
        this.currentName = nodeName;
        this.currentAddress = address;
        this.currentHash = calculateHash(nodeName);

        socket = new MulticastSocket(Ports.MULTICAST_PORT);
        group = InetAddress.getByName("225.0.113.0");
    }

    public InetAddress getPrevAddress() {
        return prevAddress;
    }

    public void setPrevAddress(InetAddress prevAddress) {
        this.prevAddress = prevAddress;
    }

    public InetAddress getNextAddress() {
        return nextAddress;
    }

    public void setNextAddress(InetAddress nextAddress) {
        this.nextAddress = nextAddress;
    }

    public void setPrevName(String prevName) {
        this.prevName = prevName;
    }

    public void setNextName(String nextName) {
        this.nextName = nextName;
    }

    public String getNextName() {
        return nextName;
    }

    public String getPrevName() {
        return prevName;
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

    @Override
    public void updateNext(InetAddress newAddress, String newName) {
        if (newAddress == null) {
            try {
                newAddress = getAddressByName(newName);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        nextHash = calculateHash(newName);
        nextAddress = newAddress;
        nextName = newName;
    }

    @Override
    public void updatePrev(InetAddress newAddress, String newName) {
        if (newAddress == null) {
            try {
                newAddress = getAddressByName(newName);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        prevHash = calculateHash(newName);
        prevAddress = newAddress;
        prevName = newName;
    }

    public void updateNeighbours(InetAddress newAddress, String newName) {
        int newHash = calculateHash(newName);

        if (newHash > currentHash && newHash < nextHash) {
            // New node sits between this node en next node.
            if (newAddress == null) {
                try {
                    newAddress = getAddressByName(newName);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            Socket clientSocket;
            try {
                //Open tcp socket to newNode @newAddress:port
                clientSocket = new Socket(newAddress, Ports.TCP_PORT);
                PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);

                //Send neighbour update command.
                out.println("NEXT_NEIGHBOUR");
                //Send neighbours
                out.println(currentName);
                out.println(nextName);

                //Close everything.
                out.close();
                clientSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

            updateNext(newAddress, newName);

        } else if (newHash < currentHash && newHash > prevHash) {
            // New node sits between this node and the previous node.
            // Only update own neighbours.

            updatePrev(newAddress, newName);
        }
    }

    private InetAddress getAddressByName(String hostname) throws IOException {
        byte[] buf;
        buf = ("GETIP|" + currentName).getBytes();
        DatagramPacket packet = new DatagramPacket(buf, buf.length, group, Ports.MULTICAST_PORT);
        socket.send(packet);

        buf = new byte[256];
        packet = new DatagramPacket(buf, buf.length);
        socket.receive(packet);

        String received = new String(buf);
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
        buf = ("HELLO|" + currentName).getBytes();
        DatagramPacket packet = new DatagramPacket(buf, buf.length, group, Ports.MULTICAST_PORT);
        socket.send(packet);

        socket.joinGroup(group);

        DatagramSocket uniSocket = new DatagramSocket(Ports.UNICAST_PORT, currentAddress);

        buf = new byte[256];
        packet = new DatagramPacket(buf, buf.length);
        uniSocket.receive(packet);

        String received = new String(buf).trim();
        if (received.startsWith("NODECOUNT")) {
            String[] split = received.split("\\|");
            Integer nodeCount = Integer.parseInt(split[1]);
            if (nodeCount < 1) {
                updateNext(currentAddress, currentName);
                updatePrev(currentAddress, currentName);
            }
            System.out.println("Node connected");
        }

        uniSocket.close();
    }

    public void leaveNetwork() {
        Socket clientSocket;
        try {
            clientSocket = new Socket(prevAddress, Ports.TCP_PORT);
            PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);

            //Send neighbour update command.
            out.println("QUIT");
            //Send neighbours
            out.println(currentName);
            out.println(nextName);

            //Close everything.
            out.close();
            clientSocket.close();

            clientSocket = new Socket(nextAddress, Ports.TCP_PORT);
            out = new PrintWriter(clientSocket.getOutputStream(), true);

            //Send neighbour update command.
            out.println("QUIT");
            //Send neighbours
            out.println(currentName);
            out.println(prevName);

            //Close everything.
            out.close();
            clientSocket.close();
        } catch (IOException e) {
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

    public static void main(String[] args) throws IOException, NotBoundException, ServerNotActiveException {
//        Registry registry = LocateRegistry.getRegistry("192.168.137.1", RMI_PORT);
//        Nameserver stub = (Nameserver) registry.lookup("be.ac.ua.dist.systemy.nameserver.NameServerPackage.NamingServer");
//
//        stub.addMeToNetwork("e");
//        stub.printIPadresses();
//        stub.getOwner("test.txt");
//        stub.exportIPadresses();
//        stub.removeMeFromNetwork("e");
//        stub.printIPadresses();


//        Node node = new Node("Node1", InetAddress.getByName("192.168.137.10"));
//        NodeInterface nodeStub = (NodeInterface) UnicastRemoteObject.exportObject(node, 0);
//
//
//        try {
//            registry.bind("be.ac.ua.dist.systemy.node.Node", nodeStub);
//            node.joinNetwork();
//
//        } catch (AlreadyBoundException e) {
//            e.printStackTrace();
//        }
        Scanner sc = new Scanner(System.in);
        System.out.print("Enter hostname: ");
        String hostname = sc.nextLine();
        System.out.print("Enter IP: ");
        String ip = sc.nextLine();

        Node node = new Node(hostname, InetAddress.getByName(ip));
        NodeUDPServer helloThread = new NodeUDPServer(node);
        helloThread.start();
        NodeTCPServer tcpServerThread = new NodeTCPServer(node);
        tcpServerThread.start();
        node.joinNetwork();

        String cmd = sc.nextLine();

        switch (cmd) {
            case "shutdown":
                node.setRunning(false);
                node.leaveNetwork();
                break;
        }
    }
}