package be.ac.ua.dist.systemy.nameserver.NameServerPackage;

import be.ac.ua.dist.systemy.Ports;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;

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

                    buf = ("NODECOUNT|" + namingServer.IpAdresses.size()).getBytes();

                    namingServer.addNodeToNetwork(hostname, packet.getAddress());

                    packet = new DatagramPacket(buf, buf.length, packet.getAddress(), Ports.UNICAST_PORT);
                    uniSocket.send(packet);
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
