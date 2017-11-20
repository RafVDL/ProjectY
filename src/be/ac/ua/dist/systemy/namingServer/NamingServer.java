package be.ac.ua.dist.systemy.namingServer;

import be.ac.ua.dist.systemy.Ports;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.rmi.AlreadyBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Scanner;

public class NamingServer implements NamingServerInterface {
    public final InetAddress serverIP; //commentaar
    HashMap<Integer, InetAddress> ipAddresses = new HashMap<>();

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
        InetAddress currentIP;
        currentIP = InetAddress.getByAddress(new byte[]{0, 0, 0, 0});
        int currentHash = 0;
        InetAddress highestIP;
        highestIP = InetAddress.getByAddress(new byte[]{0, 0, 0, 0});
        int highestHash = 0;


        for (Map.Entry<Integer, InetAddress> pair : ipAddresses.entrySet()) {
            if (pair.getKey() < hashFileName) {
                if (currentHash == 0) {
                    currentHash = pair.getKey();
                    currentIP = pair.getValue();
                } else if (pair.getKey() > currentHash) {
                    currentHash = pair.getKey();
                    currentIP = pair.getValue();
                }
            } else if (pair.getKey() > highestHash) {
                highestHash = pair.getKey();
                highestIP = pair.getValue();
            }


        }

        if (currentIP.equals(InetAddress.getByAddress(new byte[]{0, 0, 0, 0}))) {
            System.out.println("No smaller hash found, owner is: " + highestHash + "\n");
            return highestIP;
        } else {
            System.out.println("Owner is " + currentHash + "\n");
            return currentIP;
        }

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
            Registry registry = LocateRegistry.createRegistry(Ports.RMI_PORT);
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
        NamingServerHelloThread helloThread = new NamingServerHelloThread(namingServer);
        helloThread.start();
        System.out.println("Namingserver started @" + ip);

        while (namingServer.running) {
            String cmd = sc.nextLine().toLowerCase();

            switch (cmd) {
                case "shutdown":
                case "shut":
                case "sh":
                    namingServer.setRunning(false);
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