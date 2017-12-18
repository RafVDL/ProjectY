package be.ac.ua.dist.systemy.networking.tcp;

import be.ac.ua.dist.systemy.networking.Communications;
import be.ac.ua.dist.systemy.networking.packet.Packet;

import java.io.DataInputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.Socket;

/**
 * Method(s) that's used by TCPServer and TCPConnection to read packets
 */
public class TCPUtil {

    /**
     * Blocks until a packet is received, and returns it.
     *
     * @param socket the socket that holds information of the connecting client
     * @param dis    the stream to use
     * @return The packet if one is received, null otherwise
     * @throws IOException If an I/O exception occurs.
     */
    public static Packet readPacket(Socket socket, DataInputStream dis) throws IOException {
        short packetId = dis.readShort();

        Class<? extends Packet> packetClazz = Communications.getPacketById(packetId);

        if (packetClazz == null) {
            System.err.println("[TCP] Received unknown packet id " + packetId + " from " + socket.getInetAddress().getHostAddress());
            dis.close();
            return null;
        }

        if (Communications.DEBUG())
            System.out.println("[TCP] Received packet " + packetId + " from " + socket.getInetAddress().getHostAddress());

        int senderHash = dis.readInt();

        Packet packet = null;
        try {
            packet = packetClazz.getConstructor().newInstance();
            packet.setSenderHash(senderHash);
            packet.receive(dis);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            e.printStackTrace();
        }

        return packet;
    }

}
