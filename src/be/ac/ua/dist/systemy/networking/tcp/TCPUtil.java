package be.ac.ua.dist.systemy.networking.tcp;

import be.ac.ua.dist.systemy.networking.NetworkManager;
import be.ac.ua.dist.systemy.networking.packet.Packet;

import java.io.DataInputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.Socket;

public class TCPUtil {

    public static Packet readPacket(Socket socket, DataInputStream dis) throws IOException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        short packetId = dis.readShort();

        Class<? extends Packet> packetClazz = NetworkManager.getPacketById(packetId);

        if (packetClazz == null) {
            System.err.println("[TCP] Received unknown packet id " + packetId + " from " + socket.getInetAddress().getHostAddress());
            dis.close();
            return null;
        }

        if (NetworkManager.DEBUG())
            System.out.println("[TCP] Received packet " + packetId + " from " + socket.getInetAddress().getHostAddress());

        int senderHash = dis.readInt();

        Packet packet = packetClazz.getConstructor().newInstance();
        packet.setSenderHash(senderHash);
        packet.receive(dis);

        return packet;
    }

}
