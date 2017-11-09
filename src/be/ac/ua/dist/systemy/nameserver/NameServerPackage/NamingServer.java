package be.ac.ua.dist.systemy.nameserver.NameServerPackage;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.rmi.server.RemoteServer;
import java.rmi.server.ServerNotActiveException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Scanner;

public class NamingServer implements Nameserver {
    public final InetAddress serverIP; //commentaar
    HashMap<Integer, InetAddress> ipAdresses = new HashMap<>();

    private boolean running = true;

    public NamingServer(InetAddress serverIP) throws UnknownHostException {
        this.serverIP = serverIP;
    }

    public int getHash(String nodeName) {
        return Math.abs(nodeName.hashCode() % 32768);
    }

    public void addNodeToNetwork(String nodeName, InetAddress ip) {
        int hash = getHash(nodeName);
        System.out.println("Adding " + nodeName + " (hash: " + hash + ")" + " to table");

        if (!ipAdresses.containsKey(hash)) {
            ipAdresses.put(hash, ip);
        } else {
            System.out.println(hash + " already exists in ipAdresses");
        }
    }

    public void removeNodeFromNetwork(String nodeName) {
        System.out.println("Removing " + nodeName + " from IP-table");
        int hash = getHash(nodeName);
        if (ipAdresses.containsKey(hash)) {
            ipAdresses.remove(hash);
        } else {
            System.out.println(hash + " does not exist in ipAdresses");
        }
    }

    public void addMeToNetwork(String nodeName) throws ServerNotActiveException, UnknownHostException {
        InetAddress IP = InetAddress.getByName(RemoteServer.getClientHost());
        System.out.println("Adding " + nodeName + " from IP-table");
        int hash = getHash(nodeName);
        if (ipAdresses.containsKey(hash)) {
            ipAdresses.put(hash, IP);
        } else {
            System.out.println(hash + " already exists in ipAdresses");
        }
    }

    public void removeMeFromNetwork(String nodeName) throws ServerNotActiveException, UnknownHostException {
        InetAddress IP = InetAddress.getByName(RemoteServer.getClientHost());
        System.out.println("Removing " + nodeName + " from IP-table");
        int hash = getHash(nodeName);
        if (ipAdresses.containsKey(hash)) {
            ipAdresses.remove(hash, IP);
        } else {
            System.out.println(hash + " does not exist in ipAdresses");
        }
    }

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


        Iterator<HashMap.Entry<Integer, InetAddress>> it = ipAdresses.entrySet().iterator();

        while (it.hasNext()) {
            HashMap.Entry<Integer, InetAddress> pair = it.next();
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
        System.out.println("Printing IP-adresses to Console:");
        Iterator<HashMap.Entry<Integer, InetAddress>> it = ipAdresses.entrySet().iterator();
        while (it.hasNext()) {
            HashMap.Entry pair = it.next();
            System.out.println("Hash: " + pair.getKey());
            System.out.println("IP: " + pair.getValue() + "\n");
        }
        System.out.println("Print completed \n");

    }

    public void exportIPadresses() {
        System.out.println("Exporting IP-adressess ...");
        String writeThis;
        Iterator<HashMap.Entry<Integer, InetAddress>> it = ipAdresses.entrySet().iterator();
        BufferedWriter outputWriter = null;
        try {
            File outputFile = new File("test.txt");
            outputWriter = new BufferedWriter(new FileWriter(outputFile));
            while (it.hasNext()) {
                HashMap.Entry pair = it.next();
                writeThis = "Hash: " + pair.getValue() + "  IP: " + pair.getKey();
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

    public boolean isRunning() {
        return running;
    }

    public void setRunning(boolean running) {
        this.running = running;
    }

    public static void main(String[] args) throws UnknownHostException {

//        try {
//            System.setProperty("java.rmi.server.hostname", serverIP);
//            NamingServer obj = new NamingServer();
//            Nameserver stub = (Nameserver) UnicastRemoteObject.exportObject(obj, 0);
//
//            Registry registry = LocateRegistry.createRegistry(Ports.RMI_PORT);
//            registry.bind("be.ac.ua.dist.systemy.nameserver.NameServerPackage.NamingServer", stub);
//
//            System.err.println("be.ac.ua.dist.systemy.nameserver.NameServerPackage.NamingServer Ready");
//            stub.addMeToNetwork("Thomas-Nameserver");
//
//        }catch (Exception e) {
//            System.err.println("be.ac.ua.dist.systemy.nameserver.NameServerPackage.NamingServer exception: " + e.toString());
//            e.printStackTrace();
//        }
        Scanner sc = new Scanner(System.in);
        System.out.println("(Detected localHost is: " + InetAddress.getLocalHost() + ")");
        System.out.print("Enter IP: ");
        String ip = sc.nextLine();

        NamingServer namingServer = new NamingServer(InetAddress.getByName(ip));
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
                    namingServer.printIPadresses();
                    break;
            }
        }
    }
}