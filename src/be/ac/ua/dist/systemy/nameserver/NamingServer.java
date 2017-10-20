import java.rmi.registry.Registry;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;

public class NamingServer implements Nameserver{

    public NamingServer(){
        super();
    }

    IpAdresses HashMap<int, InetAddress>;


    

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