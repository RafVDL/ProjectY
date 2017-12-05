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
    private static Map<Class<? extends Packet>, Short> reversePacketLookup = new HashMap<>();

    private static int senderHash;

    static {
        registerPacket(Packet.ID.HELLO, HelloPacket.class);
        registerPacket(Packet.ID.NODE_COUNT, NodeCountPacket.class);
        registerPacket(Packet.ID.GET_IP, GetIPPacket.class);
        registerPacket(Packet.ID.QUIT_NAMING, QuitNamingPacket.class);
        registerPacket(Packet.ID.IP_RESPONSE, IPResponsePacket.class);
        registerPacket(Packet.ID.UPDATE_NEIGHBOUR, UpdateNeighboursPacket.class);
        registerPacket(Packet.ID.FILE_REQUEST, FileRequestPacket.class);
    }

    private NetworkManager() {

    }

    /**
     * Registers a packet. Only packets that are registered can be received.
     *
     * @param id    the id of the packet
     * @param clazz the packet class
     */
    public static void registerPacket(short id, Class<? extends Packet> clazz) {
        if (debug)
            System.out.println("[NetworkManager] Registered packet " + id + " to class " + clazz.getName());

        packets.put(id, clazz);
        reversePacketLookup.put(clazz, id);
    }

    /**
     * Returns the Class of the given packet ID.
     *
     * @param id The id of the packet
     * @return the class if packet id is recognized, null otherwise
     */
    public static Class<? extends Packet> getPacketById(short id) {
        return packets.get(id);
    }

    /**
     * Returns the id of the given packet class.
     *
     * @param clazz the packet class
     * @return the id if class is recognized, or -1
     */
    public static short getPacketIdByClass(Class<? extends Packet> clazz) {
        return reversePacketLookup.getOrDefault(clazz, (short) -1);
    }

    /**
     * Returns the id of the given packet.
     *
     * @param packet the packet
     * @return the id if class is recognized, or -1
     */
    public static short getPacketIdByObject(Packet packet) {
        return getPacketIdByClass(packet.getClass());
    }

    /**
     * Creates and returns a new {@link Client} with a UDP connection.
     *
     * @param address The address to connect to
     * @param port    The UDP port to connect to
     * @return A UDP client
     * @throws SocketException if the socket could not be opened, or the socket could not bind to the specified local port.
     */
    public static Client getUDPClient(InetAddress address, int port) throws SocketException {
        return new Client(address, port, new UDPConnection(address, port));
    }

    /**
     * Creates and returns a new {@link Client} with a TCP connection.
     *
     * @param address The address to connect to
     * @param port    The UDP port to connect to
     * @return A TCP client
     * @throws IOException if an error occurs during the connection
     */
    public static Client getTCPClient(InetAddress address, int port) throws IOException {
        return new Client(address, port, new TCPConnection(address, port));
    }

    /**
     * Creates and returns a new {@link Client} with a TCP connection using the given {@link Socket}.
     *
     * @param address The address to connect to
     * @param port    The UDP port to connect to
     * @param socket  The socket to use
     * @return A TCP client
     * @throws IOException if an error occurs during the connection
     */
    public static Client getTCPClient(InetAddress address, int port, Socket socket) throws IOException {
        return new Client(address, port, new TCPConnection(socket));
    }

    /**
     * Returns if debugging is enabled.
     *
     * @return true if debugging is enabled
     */
    public static boolean DEBUG() {
        return debug;
    }

    /**
     * Sets if debugging is enabled.
     *
     * @param debug true if debugging should be enabled
     */
    public static void setDebugging(boolean debug) {
        NetworkManager.debug = debug;
    }

    /**
     * Returns the hash of the current machine. The naming server uses hash 0.
     *
     * @return the hash of the current machine
     */
    public static int getSenderHash() {
        return senderHash;
    }

    /**
     * Sets the hash of the current machine. The naming server should use hash 0.
     *
     * @param senderHash the hash of the current system
     */
    public static void setSenderHash(int senderHash) {
        NetworkManager.senderHash = senderHash;
    }

}