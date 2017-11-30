package be.ac.ua.dist.systemy.networking;

import be.ac.ua.dist.systemy.networking.packet.Packet;

import java.io.*;

public interface Connection extends Flushable, Closeable {

    /**
     * Returns if the Connection is closed.
     *
     * @return true is the Connection is closed
     */
    boolean isClosed();

    /**
     * Sends a packet using this Connection.
     *
     * @param packet The packet to send
     * @throws IOException if an I/O exception occurs.
     */
    void sendPacket(Packet packet) throws IOException;

    DataInputStream getDataInputStream();

    /**
     * Returns the {@link DataOutputStream} used in this Connection.
     *
     * @return the DataOutputStream
     */
    DataOutputStream getDataOutputStream();

    /**
     * Blocks until a packet of the given type is received, and if the backing connection supports it.
     *
     * @param clazz the packet class to wait for
     * @return The packet once it is received
     */
    <E extends Packet> E waitForPacket(Class<E> clazz) throws IOException;

}
