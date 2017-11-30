package be.ac.ua.dist.systemy.networking.tcp;

import be.ac.ua.dist.systemy.networking.NetworkManager;
import be.ac.ua.dist.systemy.networking.packet.Packet;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.Socket;

public class TCPListenerRunnable implements Runnable {

    private final TCPServer tcpServer;
    private final Socket socket;

    public TCPListenerRunnable(TCPServer tcpServer, Socket socket) {
        this.tcpServer = tcpServer;
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            InputStream is = socket.getInputStream();
            DataInputStream dis = new DataInputStream(is);
            while (!socket.isClosed()) {
                short packetId = dis.readShort();

                Class<? extends Packet> packetClazz = NetworkManager.getPacketById(packetId);

                if (packetClazz == null) {
                    System.err.println("[TCP] Received unknown packet id " + packetId + " from " + socket.getInetAddress().getHostAddress());
                    dis.close();
                    return;
                }

                if (NetworkManager.DEBUG())
                    System.out.println("[TCP] Received packet " + packetId + " from " + socket.getInetAddress().getHostAddress());

                int senderHash = dis.readInt();

                Packet packet = packetClazz.getConstructor().newInstance();
                packet.setSenderHash(senderHash);
                packet.receive(dis);

                tcpServer.getPacketListeners(packetClazz).forEach(packetListener -> {
                    try {
                        packetListener.receivePacket(packet, NetworkManager.getTCPClient(socket.getInetAddress(), socket.getPort(), socket));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
            }
        } catch (IOException | IllegalAccessException | InstantiationException | NoSuchMethodException | InvocationTargetException e) {
            e.printStackTrace();
        }
    }

}
