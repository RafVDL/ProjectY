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

    private static boolean debug = false;

    private static Map<Short, Class<? extends Packet>> packets = new HashMap<>();
    private static Map<Class<? extends Packet>, List<PacketListener>> packetListeners = new HashMap<>();

    private static int senderHash;

    static {
        registerPacket(Packet.ID.HELLO, HelloPacket.class);
        registerPacket(Packet.ID.NODECOUNT, NodeCountPacket.class);
        registerPacket(Packet.ID.GETIP, GetIPPacket.class);
        registerPacket(Packet.ID.QUITNAMING, QuitNamingPacket.class);
        registerPacket(Packet.ID.IPRESPONSE, IPResponsePacket.class);
    }

    private NetworkManager() {

    }

    public static void registerPacket(short id, Class<? extends Packet> clazz) {
        if (debug)
            System.out.println("[NetworkManager] Registered packet " + id + " to class " + clazz.getName());

        packets.put(id, clazz);
    }

    /**
     * Returns the Class of the given packet ID
     *
     * @param id
     * @return
     */
    public static Class<? extends Packet> getPacketById(short id) {
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

    public static boolean DEBUG() {
        return debug;
    }

    public static void setDebugging(boolean debug) {
        NetworkManager.debug = debug;
    }

    public static int getSenderHash() {
        return senderHash;
    }

    public static void setSenderHash(int senderHash) {
        NetworkManager.senderHash = senderHash;
    }

}