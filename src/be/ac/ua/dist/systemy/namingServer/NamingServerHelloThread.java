package be.ac.ua.dist.systemy.namingServer;

import be.ac.ua.dist.systemy.Constants;

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
        MulticastSocket multicastSocket;
        try {
            multicastSocket = new MulticastSocket(Constants.MULTICAST_PORT);
            DatagramSocket uniSocket = new DatagramSocket(Constants.UNICAST_PORT, namingServer.serverIP);
            InetAddress group = InetAddress.getByName(Constants.MULTICAST_ADDRESS);
            multicastSocket.joinGroup(group);

            DatagramPacket packet;
            while (namingServer.isRunning()) {
                byte[] buf = new byte[1024];
                packet = new DatagramPacket(buf, buf.length);
                multicastSocket.receive(packet);

                String received = new String(packet.getData()).trim();
                if (received.startsWith("HELLO")) {
                    String[] split = received.split("\\|");
                    int hash = Integer.parseInt(split[1]);

                    Socket tcpSocket;
                    DataOutputStream dos;
                    PrintWriter out;
                    try {
                        tcpSocket = new Socket();
                        tcpSocket.setSoLinger(true, 5);
                        tcpSocket.connect(new InetSocketAddress(packet.getAddress(), Constants.TCP_PORT), 1000);
                        dos = new DataOutputStream(tcpSocket.getOutputStream());
                        out = new PrintWriter(dos, true);

                        out.println("NODECOUNT");
                        dos.writeInt(namingServer.ipAddresses.size());
                        dos.flush();

                        //Close everything.
                        out.close();
                        tcpSocket.close();
                    } catch (SocketTimeoutException e) {
                        // handle node disconnected
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    namingServer.addNodeToNetwork(hash, packet.getAddress());

                } else if (received.startsWith("GETIP")) {
                    String[] split = received.split("\\|");
                    int hash = Integer.parseInt(split[1]);
                    if (namingServer.ipAddresses.containsKey(hash)) {
                        buf = ("REIP|" + hash + "|" + namingServer.ipAddresses.get(hash).getHostAddress()).getBytes();
                    } else {
                        buf = ("REIP|" + hash + "|NOT_FOUND").getBytes();
                    }

                    packet = new DatagramPacket(buf, buf.length, packet.getAddress(), Constants.UNICAST_PORT);
                    uniSocket.send(packet);
                } else if (received.startsWith("QUITNAMING")) {
                    String[] split = received.split("\\|");
                    int hash = Integer.parseInt(split[1]);
                    namingServer.removeNodeFromNetwork(hash);
                }
            }

            multicastSocket.leaveGroup(group);
            multicastSocket.close();
            uniSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
