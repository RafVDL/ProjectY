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

    public NamingServer(InetAddress serverIP) throws UnknownHostException {
        this.serverIP = serverIP;
    }

    HashMap<Integer, InetAddress> IpAdresses = new HashMap<Integer, InetAddress>();
    public final InetAddress serverIP; //commentaar

    public int getHash(String nodeName) {
        return Math.abs(nodeName.hashCode() % 32768);
    }

    public void addNodeToNetwork(String nodeName, InetAddress ip) {
        System.out.println("Adding " + nodeName + " to table");
        int hash = getHash(nodeName);
        IpAdresses.put(hash, ip);
    }

    public void addMeToNetwork(String nodeName) throws ServerNotActiveException, UnknownHostException {
        InetAddress IP = InetAddress.getByName(RemoteServer.getClientHost());
        System.out.println("Adding " + nodeName + " from IP-table");
        int hashComputername = getHash(nodeName);
        IpAdresses.put(hashComputername, IP);
    }

    public void removeMeFromNetwork(String nodeName) throws ServerNotActiveException, UnknownHostException {
        InetAddress IP = InetAddress.getByName(RemoteServer.getClientHost());
        System.out.println("Removing " + nodeName + " from IP-table");
        int hashComputername = getHash(nodeName);
        IpAdresses.remove(hashComputername, IP);
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


        Iterator<HashMap.Entry<Integer, InetAddress>> it = IpAdresses.entrySet().iterator();

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
        Iterator<HashMap.Entry<Integer, InetAddress>> it = IpAdresses.entrySet().iterator();
        while (it.hasNext()) {
            HashMap.Entry pair = (HashMap.Entry) it.next();
            System.out.println("Hash: " + pair.getKey());
            System.out.println("IP: " + pair.getValue() + "\n");
        }
        System.out.println("Print completed \n");

    }

    public void exportIPadresses() {
        System.out.println("Exporting IP-adressess ...");
        String writeThis;
        Iterator<HashMap.Entry<Integer, InetAddress>> it = IpAdresses.entrySet().iterator();
        int i = 0;
        BufferedWriter outputWriter = null;
        try {
            File outputFile = new File("test.txt");
            outputWriter = new BufferedWriter(new FileWriter(outputFile));
            while (it.hasNext()) {
                HashMap.Entry pair = (HashMap.Entry) it.next();
                writeThis = "Hash: " + pair.getValue() + "  IP: " + pair.getKey();
                outputWriter.write(writeThis);
                outputWriter.newLine();
            }
            outputWriter.flush();
        } catch (Exception e) {
            e.printStackTrace();
            ;
        } finally {
            try {
                outputWriter.close();
            } catch (Exception e) {

            }
        }
        System.out.println("Export completed \n");
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
        System.out.print("Enter IP: ");
        String ip = sc.nextLine();

        NamingServer namingServer = new NamingServer(InetAddress.getByName(ip));
        NamingServerHelloThread helloThread = new NamingServerHelloThread(namingServer);
        helloThread.start();


    }


}