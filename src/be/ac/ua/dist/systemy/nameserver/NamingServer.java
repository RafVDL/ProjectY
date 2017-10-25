import java.net.UnknownHostException;
import java.rmi.registry.Registry;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import java.net.InetAddress;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.util.Iterator;

public class NamingServer implements Nameserver{

    public NamingServer(){
        super();
    }

    HashMap<int, InetAddress> IpAdresses = new HashMap<int, InetAddress>();

    void addMeToNetwork(String computerName, InetAddress IP){
        int hashComputername = Math.abs(computerName.hashCode() % 32768);
        IpAdresses.put(hashComputername, IP);
    }

    void removeMeFromNetwork(String computerName){
        IpAdresses.remove(Math.abs(computerName.hashCode() % 32768));
    }

    InetAddress getOwner(String fileName) throws UnknownHostException {
        int hashFileName = Math.abs(fileName.hashCode() % 32768);
        InetAddress currentIP;
        currentIP = InetAddress.getByAddress(new byte[] {0, 0, 0, 0});
        int currentHash = 0;
        InetAddress highestIP;
        int highestHash = 0;


        Iterator<HashMap.Entry<int, InetAddress>> it = IpAdresses.entrySet().iterator();

        while(it.hasNext()){
            HashMap.Entry<int, InetAddress> pair = it.next();
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

        if(currentIP.equals  == InetAddress.getByAddress(new byte[] {0, 0, 0, 0})){
            return highestIP;
        } else {
            return currentIP;
        }

    }

    void printIPadresses(){
        Iterator<HashMap.Entry<int, InetAddress>> it = IpAdresses.entrySet().iterator();
        while(it.hasNext()){
            System.out.println("Hash: " + it.getKey());
            System.out.println("IP: " + it.getValue() + "\n");
        }

    }

    void exportIPadresses(){
        String writeThis;
        Iterator<HashMap.Entry<int, InetAddress>> it = IpAdresses.entrySet().iterator();
        int i=0;
        BufferedWriter outputWriter = null;
        try {
            File outputFile = new File("test.txt");
            outputWriter = new BufferedWriter(new FileWriter(outputFile));
            while(it.hasNext()) {
                writeThis = "Hash: " + it.getValue() + "  IP: " + it.getKey();
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
    }






    public static void main(String[] args) {

        try {
            NamingServer obj = new NamingServer();
            Nameserver stub = (Nameserver) UnicastRemoteObject.exportObject(obj, 0);

            Registry registry = LocateRegistry.getRegistry();
            registry.bind("NamingServer", stub);

            System.err.println("NamingServer Ready");

        }catch (Exception e) {
            System.err.println("NamingServer exception: " + e.toString());
            e.printStackTrace();
        }
    }


}