package be.ac.ua.dist.systemy.networking;

import be.ac.ua.dist.systemy.networking.packet.Packet;

import java.io.IOException;

/**
 * The listener interface for receiving {@link Packet}s. Classes that implement this interface can be passed to {@link Server#registerListener(Class, PacketListener)}.
 *
 * @param <E> the type of {@link Packet} the {@link PacketListener} will receive
 */
public interface PacketListener<E extends Packet> {

    /**
     * This method gets invoked by a {@link Server} instance when the given {@link Packet} is received.
     *
     * @param packet The packet that the server received
     * @param client The client that contacted the server
     * @throws IOException If an I/O exception occurs
     */
    void receivePacket(E packet, Client client) throws IOException;

}
