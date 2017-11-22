package be.ac.ua.dist.systemy.node;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.SocketTimeoutException;

public class NodeMultiCastServer extends Thread {

    private Node node;

    public NodeMultiCastServer(Node node) {
        this.node = node;
    }

    @Override
    public void run() {
        MulticastSocket socket;
        try {
            socket = new MulticastSocket(4446);
            socket.setSoTimeout(2000);
            InetAddress group = InetAddress.getByName("225.0.113.0");
            socket.joinGroup(group);

            DatagramPacket packet;
            while (node.isRunning()) {
                byte[] buf = new byte[256];
                packet = new DatagramPacket(buf, buf.length);

                try {
                    socket.receive(packet);
                    String received = new String(packet.getData()).trim();
                    if (received.startsWith("HELLO")) { // message from new node
                        String[] split = received.split("\\|");
                        int hash = Integer.parseInt(split[1]);

                        if (hash != node.getOwnHash()) {
                            node.updateNeighbours(packet.getAddress(), hash);
                            node.replicateFiles();
                        }
                    }
//                } else if (received.startsWith("HELLOR")) { // response from already existing node
//                    String[] split = received.split("\\|");
//                    String hostname = split[1];
//                    String nextHostname = split[2];
//
//                    node.updateNeighbours(packet.getAddress(), hostname);
//                    node.updateNeighbours(null, nextHostname);
//                }
                } catch (SocketTimeoutException e) {
                    // ignore
                }
            }

            socket.leaveGroup(group);
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
