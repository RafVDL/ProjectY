import java.rmi.registry.Registry;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import java.net.InetAddress;

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

    InetAddress getOwner(String fileName){
        int hashFileName = Math.abs(fileName.hashCode() % 32768);
        int found = 0;
        InetAddress current;
        for(int i = hashFileName; !found && (i > 0); i--){
            current = IpAdresses.get(i);
            if(current != null){
                found = 1;
            }
        }
        if(current == null){
            //uitzondering: niets gevonden 
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