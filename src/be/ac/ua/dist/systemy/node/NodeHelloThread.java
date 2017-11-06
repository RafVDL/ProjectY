package be.ac.ua.dist.systemy.node;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;

public class NodeHelloThread extends Thread {

    private Node node;

    public NodeHelloThread(Node node) {
        this.node = node;
    }

    public boolean stop = false;

    @Override
    public void run() {
        MulticastSocket socket;
        try {
            socket = new MulticastSocket(4446);
            InetAddress group = InetAddress.getByName("203.0.113.0");
            socket.joinGroup(group);

            DatagramPacket packet;
            while (!stop) {
                byte[] buf = new byte[256];
                packet = new DatagramPacket(buf, buf.length);
                socket.receive(packet);

                String received = new String(packet.getData());
                if (received.startsWith("HELLO")) { // message from new node
                    String[] split = received.split("\\|");
                    String hostname = split[1];

                    node.updateNeighbours(packet.getAddress(), hostname);
                }
//                } else if (received.startsWith("HELLOR")) { // response from already existing node
//                    String[] split = received.split("\\|");
//                    String hostname = split[1];
//                    String nextHostname = split[2];
//
//                    node.updateNeighbours(packet.getAddress(), hostname);
//                    node.updateNeighbours(null, nextHostname);
//                }
            }

            socket.leaveGroup(group);
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
