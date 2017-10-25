package be.ac.ua.dist.systemy.node;

import be.ac.ua.dist.systemy.nameserver.NameServerPackage.Nameserver;

import java.io.DataInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.List;

public class Node implements NodeInterface {
    public static final int PORT = 6969;

    private String nodeName;
    private InetAddress address;
    private int addressHash;
    private List<String> localFiles;
    private List<String> replicatedFiles;
    private List<String> downloadedFiles;

    public Node(String nodeName, InetAddress address) {
        this.nodeName = nodeName;
        this.address = address;
        this.addressHash = Math.abs(nodeName.hashCode() % 32768);
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
            //get ip via rmi
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

    public static void main(String[] args) throws RemoteException, NotBoundException, UnknownHostException {
        Registry registry = LocateRegistry.getRegistry("192.168.137.1", 3733);
        Nameserver stub = (Nameserver) registry.lookup("be.ac.ua.dist.systemy.nameserver.NameServerPackage.NamingServer");

        stub.addMeToNetwork("test", InetAddress.getByName("192.168.137.2"));
        stub.printIPadresses();

        NodeTCPServer tcpServer = new NodeTCPServer();
        tcpServer.startServer();
    }
}