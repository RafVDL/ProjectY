package be.ac.ua.dist.systemy.nameserver.NameServerPackage;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;

public class NamingServerHelloThread extends Thread {

    public boolean stop = false;
    private NamingServer namingServer;

    public NamingServerHelloThread(NamingServer namingServer) {
        this.namingServer = namingServer;
    }

    @Override
    public void run() {
        MulticastSocket socket;
        try {
            socket = new MulticastSocket(4446);
            InetAddress group = InetAddress.getByName("203.0.113.0");
            socket.joinGroup(group);

            DatagramPacket packet;
            while (!stop) {
                byte[] buf = new byte[1024];
                packet = new DatagramPacket(buf, buf.length);
                socket.receive(packet);

                String received = new String(packet.getData());
                if (received.startsWith("HELLO")) {
                    String[] split = received.split("\\|");
                    String hostname = split[1];

                    buf = ("NODECOUNT|" + namingServer.IpAdresses.size()).getBytes();

                    packet = new DatagramPacket(buf, buf.length);
                    socket.send(packet);
                } else if (received.startsWith("GETIP")) {
                    String[] split = received.split("\\|");
                    String hostname = split[1];
                    if (namingServer.IpAdresses.containsKey(Math.abs(hostname.hashCode() % 32768))) {
                        buf = ("REIP|" + hostname + "|" + namingServer.IpAdresses.get(Math.abs(hostname.hashCode() % 32768)).getHostAddress()).getBytes();
                    } else {
                        buf = ("REIP|" + hostname + "|NOT_FOUND").getBytes();
                    }

                    packet = new DatagramPacket(buf, buf.length);
                    socket.send(packet);
                }
            }

            socket.leaveGroup(group);
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
