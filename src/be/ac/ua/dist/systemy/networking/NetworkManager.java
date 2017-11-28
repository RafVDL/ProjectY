package be.ac.ua.dist.systemy.networking;

import be.ac.ua.dist.systemy.networking.packet.*;
import be.ac.ua.dist.systemy.networking.tcp.TCPConnection;
import be.ac.ua.dist.systemy.networking.udp.UDPConnection;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NetworkManager {

    private static Map<Integer, Class<? extends Packet>> packets = new HashMap<>();
    private static Map<Class<? extends Packet>, List<PacketListener>> packetListeners = new HashMap<>();

    static {
        packets.put(Packet.ID.HELLO, HelloPacket.class);
        packets.put(Packet.ID.NODECOUNT, NodeCountPacket.class);
        packets.put(Packet.ID.GETIP, GetIPPacket.class);
        packets.put(Packet.ID.QUITNAMING, QuitNamingPacket.class);
        packets.put(Packet.ID.IPRESPONSE, IPResponsePacket.class);
    }

    private NetworkManager() {

    }

    /**
     * Returns the Class of the given packet ID
     *
     * @param id
     * @return
     */
    public static Class<? extends Packet> getPacketById(int id) {
        return packets.get(id);
    }

    public static <T extends Packet> void registerListener(Class<T> clazz, PacketListener<T> listener) {
        packetListeners.computeIfAbsent(clazz, k -> new ArrayList<>());
        packetListeners.get(clazz).add(listener);
    }

    public static List<PacketListener> getPacketListeners(Class<? extends Packet> clazz) {
        return packetListeners.getOrDefault(clazz, new ArrayList<>());
    }

    public static Client getUDPClient(InetAddress address, int port) throws SocketException {
        return new Client(address, port, new UDPConnection(address, port));
    }

    public static Client getTCPClient(InetAddress address, int port) throws IOException {
        return new Client(address, port, new TCPConnection(address, port));
    }

    public static Client getTCPClient(InetAddress address, int port, Socket socket) throws IOException {
        return new Client(address, port, new TCPConnection(socket));
    }

}