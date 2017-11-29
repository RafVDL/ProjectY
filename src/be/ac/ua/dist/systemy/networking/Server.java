package be.ac.ua.dist.systemy.networking;

import be.ac.ua.dist.systemy.networking.packet.Packet;

import java.net.InetAddress;
import java.util.List;

public interface Server {

    boolean startServer(InetAddress address, int port);

    void stop();

    /**
     * Adds the given listener to the listeners of the given packet class.
     *
     * @param clazz
     * @param listener
     * @param <T>
     */
    <T extends Packet> void registerListener(Class<T> clazz, PacketListener<T> listener);

    /**
     * Returns a list of listeners of the given packet class.
     *
     * @param clazz the packet class
     * @return a list of listeners
     */
    List<PacketListener> getPacketListeners(Class<? extends Packet> clazz);

}
