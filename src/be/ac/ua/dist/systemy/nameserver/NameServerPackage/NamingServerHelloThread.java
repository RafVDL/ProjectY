package be.ac.ua.dist.systemy.nameserver.NameServerPackage;

import be.ac.ua.dist.systemy.Ports;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.*;

public class NamingServerHelloThread extends Thread {
    private NamingServer namingServer;

    public NamingServerHelloThread(NamingServer namingServer) {
        this.namingServer = namingServer;
    }

    @Override
    public void run() {
        MulticastSocket socket;
        try {
            socket = new MulticastSocket(Ports.MULTICAST_PORT);
            DatagramSocket uniSocket = new DatagramSocket(Ports.UNICAST_PORT, namingServer.serverIP);
            InetAddress group = InetAddress.getByName("225.0.113.0");
            socket.joinGroup(group);

            DatagramPacket packet;
            while (namingServer.isRunning()) {
                byte[] buf = new byte[1024];
                packet = new DatagramPacket(buf, buf.length);
                socket.receive(packet);

                String received = new String(packet.getData()).trim();
                if (received.startsWith("HELLO")) {
                    String[] split = received.split("\\|");
                    String hostname = split[1];

                    namingServer.addNodeToNetwork(hostname, packet.getAddress());

                    Socket clientSocket;
                    DataOutputStream dos;
                    PrintWriter out;
                    try {
                        clientSocket = new Socket();
                        clientSocket.connect(new InetSocketAddress(packet.getAddress(), Ports.TCP_PORT), 1000);
                        dos = new DataOutputStream(clientSocket.getOutputStream());
                        out = new PrintWriter(dos);

                        out.println("NODECOUNT");
                        dos.writeInt(namingServer.IpAdresses.size());

                        //Close everything.
                        dos.close();
                        clientSocket.close();
                    } catch (SocketTimeoutException e) {
                        // handle node disconnected
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else if (received.startsWith("GETIP")) {
                    String[] split = received.split("\\|");
                    String hostname = split[1];
                    if (namingServer.IpAdresses.containsKey(Math.abs(hostname.hashCode() % 32768))) {
                        buf = ("REIP|" + hostname + "|" + namingServer.IpAdresses.get(Math.abs(hostname.hashCode() % 32768)).getHostAddress()).getBytes();
                    } else {
                        buf = ("REIP|" + hostname + "|NOT_FOUND").getBytes();
                    }

                    packet = new DatagramPacket(buf, buf.length, packet.getAddress(), Ports.UNICAST_PORT);
                    uniSocket.send(packet);
                } else if (received.startsWith("QUITNAMING")) {
                    String[] split = received.split("\\|");
                    String hostname = split[1];
                    namingServer.removeNodeFromNetwork(hostname);
                }
            }

            socket.leaveGroup(group);
            socket.close();
            uniSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
