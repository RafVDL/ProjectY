package be.ac.ua.dist.systemy.namingServer;

import be.ac.ua.dist.systemy.Constants;
import be.ac.ua.dist.systemy.networking.Client;
import be.ac.ua.dist.systemy.networking.NetworkManager;
import be.ac.ua.dist.systemy.networking.Server;
import be.ac.ua.dist.systemy.networking.multicast.MulticastServer;
import be.ac.ua.dist.systemy.networking.packet.*;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.rmi.AlreadyBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Scanner;
import java.util.TreeMap;

public class NamingServer implements NamingServerInterface {
    public final InetAddress serverIP;
    TreeMap<Integer, InetAddress> ipAddresses = new TreeMap<>();

    private boolean running = true;

    public NamingServer(InetAddress serverIP) throws UnknownHostException {
        this.serverIP = serverIP;
    }

    public int getHash(String nodeName) {
        return Math.abs(nodeName.hashCode() % 32768);
    }

    public void addNodeToNetwork(int hash, InetAddress ip) {
        System.out.println("Adding " + " (hash: " + hash + ")" + " to table");

        if (!ipAddresses.containsKey(hash)) {
            ipAddresses.put(hash, ip);
        } else {
            System.out.println(hash + " already exists in ipAddresses");
        }
    }

    public void removeNodeFromNetwork(int hash) throws RemoteException {
        System.out.println("Removing " + hash + " from IP-table");
        if (ipAddresses.containsKey(hash)) {
            ipAddresses.remove(hash);
        } else {
            System.out.println(hash + " does not exist in ipAddresses");
        }
    }

//    public void addMeToNetwork(String nodeName) throws ServerNotActiveException, UnknownHostException {
//        InetAddress IP = InetAddress.getByName(RemoteServer.getClientHost());
//        System.out.println("Adding " + nodeName + " from IP-table");
//        int hash = getHash(nodeName);
//        if (ipAddresses.containsKey(hash)) {
//            ipAddresses.put(hash, IP);
//        } else {
//            System.out.println(hash + " already exists in ipAddresses");
//        }
//    }


    public InetAddress getOwner(String fileName) throws UnknownHostException {
        System.out.println("Getting owner of file: " + fileName);
        int hashFileName = getHash(fileName);
        System.out.println("Hash of file = " + hashFileName);
        Integer currentHash = ipAddresses.floorKey(hashFileName);

        if (currentHash == null)
            currentHash = ipAddresses.lastKey();

        InetAddress currentIP = ipAddresses.get(currentHash);

        System.out.println("Owner is " + currentHash);
        return currentIP;
    }

    public void printIPadresses() {
        System.out.println("Printing IP-addresses to Console:");
        ipAddresses.forEach((key, value) -> {
            System.out.println("Hash: " + key);
            System.out.println("IP: " + value + "\n");
        });
        System.out.println("Print completed \n");

    }

    public void exportIPadresses() {
        System.out.println("Exporting IP-addressess ...");
        String writeThis;
        Iterator<HashMap.Entry<Integer, InetAddress>> it = ipAddresses.entrySet().iterator();
        BufferedWriter outputWriter = null;
        try {
            File outputFile = new File("test.txt");
            outputWriter = new BufferedWriter(new FileWriter(outputFile));
            while (it.hasNext()) {
                HashMap.Entry pair = it.next();
                writeThis = "Hash: " + pair.getKey() + "  IP: " + pair.getValue();
                outputWriter.write(writeThis);
                outputWriter.newLine();
            }
            outputWriter.flush();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                outputWriter.close();
            } catch (Exception e) {
                //
            }
        }
        System.out.println("Export completed \n");
    }

    public InetAddress getIPNode(int hashNode) throws RemoteException {
        return ipAddresses.getOrDefault(hashNode, null);
    }

    public int[] getNeighbours(int hashNode) throws RemoteException {
        Iterator<HashMap.Entry<Integer, InetAddress>> it = ipAddresses.entrySet().iterator();
        int[] neighbours = new int[2];
        int prevHash = 0;
        int nextHash = 0;
        boolean found = false;
        while (it.hasNext() && !found) {
            HashMap.Entry<Integer, InetAddress> pair = it.next();
            if (pair.getKey() == hashNode) {
                prevHash = pair.getKey();
                if (it.hasNext()) {
                    pair = it.next();
                    nextHash = pair.getKey();
                } else {
                    it = ipAddresses.entrySet().iterator();
                    pair = it.next();
                    nextHash = pair.getKey();
                }
                found = true;
            }
        }
        neighbours[0] = prevHash;
        neighbours[1] = nextHash;
        return neighbours;
    }

    public boolean isRunning() {
        return running;
    }

    public void setRunning(boolean running) {
        this.running = running;
    }

    public void initializeRMI() {
        try {
            System.setProperty("java.rmi.server.hostname", serverIP.getHostAddress());
            Registry registry = LocateRegistry.createRegistry(Constants.RMI_PORT);
            NamingServerInterface stub = (NamingServerInterface) UnicastRemoteObject.exportObject(this, 0);
            registry.bind("NamingServer", stub);
        } catch (AlreadyBoundException | RemoteException e) {
            System.err.println("NamingServer exception: " + e.toString());
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws UnknownHostException {
        Scanner sc = new Scanner(System.in);
        InetAddress detectedHostAddress = InetAddress.getLocalHost();
        System.out.println("(Detected localHost is: " + detectedHostAddress + ")");
        System.out.print("Enter IP: ");
        String ip = sc.nextLine();
        if (ip.isEmpty()) {
            ip = detectedHostAddress.getHostAddress();
        }

        NamingServer namingServer = new NamingServer(InetAddress.getByName(ip));
        namingServer.initializeRMI();

        NetworkManager.setSenderHash(0); // NamingServer exclusive!

        NetworkManager.registerListener(HelloPacket.class, ((packet, client) -> {
            try {
                Client tcpClient = NetworkManager.getTCPClient(client.getAddress(), Constants.TCP_PORT);
                NodeCountPacket nodeCountPacket = new NodeCountPacket(namingServer.ipAddresses.size());
                tcpClient.sendPacket(nodeCountPacket);

                namingServer.addNodeToNetwork(packet.getSenderHash(), client.getAddress());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }));

        NetworkManager.registerListener(GetIPPacket.class, ((packet, client) -> {
            try {
                IPResponsePacket ipResponsePacket = new IPResponsePacket(namingServer.ipAddresses.get(packet.getHash()));
                client.sendPacket(ipResponsePacket);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }));

        NetworkManager.registerListener(QuitNamingPacket.class, ((packet, client) -> {
            try {
                namingServer.removeNodeFromNetwork(packet.getSenderHash());
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }));

        Server multicastServer = new MulticastServer();
        multicastServer.startServer(InetAddress.getByName("225.0.113.0"), Constants.MULTICAST_PORT);

        System.out.println("Namingserver started @" + ip);

        while (namingServer.running) {
            String cmd = sc.nextLine().toLowerCase();

            switch (cmd) {
                case "shutdown":
                case "shut":
                case "sh":
                    namingServer.setRunning(false);
                    multicastServer.stop();
                    System.out.println("Shutdown the network");
                    break;

                case "table":
                case "tab":
                case "tb":
                    namingServer.printIPadresses();
                    break;
            }
        }
    }
}