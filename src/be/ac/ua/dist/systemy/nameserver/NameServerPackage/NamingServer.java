package be.ac.ua.dist.systemy.nameserver.NameServerPackage;

import java.net.UnknownHostException;
import java.rmi.registry.Registry;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.RemoteServer;
import java.rmi.server.ServerNotActiveException;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import java.net.InetAddress;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.Iterator;

public class NamingServer implements Nameserver{

    public NamingServer(){
        super();
    }

    HashMap<Integer, InetAddress> IpAdresses = new HashMap<Integer, InetAddress>();
    private static final int PORT = 3733;
    private static final String serverIP = "192.168.137.10"; //commentaar


    public void addMeToNetwork(String nodeName) throws ServerNotActiveException, UnknownHostException {
        InetAddress IP = InetAddress.getByName(RemoteServer.getClientHost());
        System.out.println("Adding " + nodeName + " from IP-table");
        int hashComputername = Math.abs(nodeName.hashCode() % 32768);
        IpAdresses.put(hashComputername, IP);
    }

    public void removeMeFromNetwork(String nodeName) throws ServerNotActiveException, UnknownHostException {
        InetAddress IP = InetAddress.getByName(RemoteServer.getClientHost());
        System.out.println("Removing " + nodeName + " from IP-table");
        int hashComputername = Math.abs(nodeName.hashCode() % 32768);
        IpAdresses.remove(hashComputername, IP);
    }

    public InetAddress getOwner(String fileName) throws UnknownHostException {
        System.out.println("Getting owner of file: " + fileName);
        int hashFileName = Math.abs(fileName.hashCode() % 32768);
        System.out.println("Hash of file = " + hashFileName);
        InetAddress currentIP;
        currentIP = InetAddress.getByAddress(new byte[] {0, 0, 0, 0});
        int currentHash = 0;
        InetAddress highestIP;
        highestIP = InetAddress.getByAddress(new byte[] {0, 0, 0, 0});
        int highestHash = 0;


        Iterator<HashMap.Entry<Integer, InetAddress>> it = IpAdresses.entrySet().iterator();

        while(it.hasNext()){
            HashMap.Entry<Integer, InetAddress> pair = it.next();
            if(pair.getKey() < hashFileName){
                if(currentHash == 0){
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

        if(currentIP.equals(InetAddress.getByAddress(new byte[] {0, 0, 0, 0}))){
            System.out.println("No smaller hash found, owner is: " + highestHash + "\n");
            return highestIP;
        } else {
            System.out.println("Owner is " + currentHash + "\n");
            return currentIP;
        }

    }

    public void printIPadresses(){
        System.out.println("Printing IP-adresses to Console:");
        Iterator<HashMap.Entry<Integer, InetAddress>> it = IpAdresses.entrySet().iterator();
        while(it.hasNext()){
            HashMap.Entry pair = (HashMap.Entry)it.next();
            System.out.println("Hash: " + pair.getKey());
            System.out.println("IP: " + pair.getValue() + "\n");
        }
        System.out.println("Print completed \n");

    }

     public void exportIPadresses(){
        System.out.println("Exporting IP-adressess ...");
        String writeThis;
        Iterator<HashMap.Entry<Integer, InetAddress>> it = IpAdresses.entrySet().iterator();
        int i=0;
        BufferedWriter outputWriter = null;
        try {
            File outputFile = new File("test.txt");
            outputWriter = new BufferedWriter(new FileWriter(outputFile));
            while(it.hasNext()) {
                HashMap.Entry pair = (HashMap.Entry)it.next();
                writeThis = "Hash: " + pair.getValue() + "  IP: " + pair.getKey();
                outputWriter.write(writeThis);
                outputWriter.newLine();
            }
            outputWriter.flush();
        } catch (Exception e) {
            e.printStackTrace();;
        } finally{
            try {
                outputWriter.close();
            } catch(Exception e) {

            }
        }
        System.out.println("Export completed \n");
    }






    public static void main(String[] args) {

//        try {
//            System.setProperty("java.rmi.server.hostname", serverIP);
//            NamingServer obj = new NamingServer();
//            Nameserver stub = (Nameserver) UnicastRemoteObject.exportObject(obj, 0);
//
//            Registry registry = LocateRegistry.createRegistry(PORT);
//            registry.bind("be.ac.ua.dist.systemy.nameserver.NameServerPackage.NamingServer", stub);
//
//            System.err.println("be.ac.ua.dist.systemy.nameserver.NameServerPackage.NamingServer Ready");
//            stub.addMeToNetwork("Thomas-Nameserver");
//
//        }catch (Exception e) {
//            System.err.println("be.ac.ua.dist.systemy.nameserver.NameServerPackage.NamingServer exception: " + e.toString());
//            e.printStackTrace();
//        }
        NamingServer namingServer = new NamingServer();
        NamingServerHelloThread helloThread = new NamingServerHelloThread(namingServer);
        helloThread.start();


    }


}