package be.ac.ua.dist.systemy.networking;

import be.ac.ua.dist.systemy.networking.packet.Packet;

import java.net.InetAddress;
import java.util.List;

/**
 * Implementations of this class handle incoming {@link Packet}s.
 */
public interface Server {

    /**
     * Starts the Server, and binds to the given IP address and port.
     *
     * @param address the address to bind to
     * @param port    the port to bind to
     * @return True if the server has started
     */
    boolean startServer(InetAddress address, int port);

    /**
     * Stops the Server.
     * <p>
     * It will no longer be listening to new packets, and the Socket will be closed.
     */
    void stop();

    /**
     * Adds the given listener to the listeners of the given packet class.
     *
     * @param clazz    the packet class to register a {@link PacketListener} for
     * @param listener the {@link PacketListener} to add
     * @param <T>      the type of {@link Packet} the {@link PacketListener} will receive
     */
    <T extends Packet> void registerListener(Class<T> clazz, PacketListener<T> listener);

    /**
     * Returns a list of listeners of the given packet class.
     *
     * @param clazz the packet class
     * @return the list of listeners
     */
    List<PacketListener> getPacketListeners(Class<? extends Packet> clazz);

}
