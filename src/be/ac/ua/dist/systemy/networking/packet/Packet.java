package be.ac.ua.dist.systemy.networking.packet;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * An abstract class for packet handlers
 */
public abstract class Packet {

    /**
     * Contains ID's for packets.
     */
    public class ID {

        /**
         * Packet sent using multicast to join the network.
         */
        public static final short HELLO = 0x00;

        /**
         * Sent by the naming server as (TCP) response to a {@link #HELLO} packet.
         */
        public static final short NODE_COUNT = 0x01;

        /**
         * Requests a node's ip using multicast.
         */
        public static final short GET_IP = 0x02;

        /**
         * Sent by a node using multicast to the naming server to notify it is quitting the network.
         */
        public static final short QUIT_NAMING = 0x03;

        /**
         * Sent by naming server as response to a {@link #GET_IP} packet.
         */
        public static final short IP_RESPONSE = 0x04;

        /**
         * Updates the receiving node's neighbours.
         */
        public static final short UPDATE_NEIGHBOUR = 0x05;

        /**
         * Sent by a node to request a file.
         */
        public static final short FILE_REQUEST = 0x10;

        /**
         * A series of these packets are sent by a node as response to {@link #FILE_REQUEST}.
         */
        public static final short FILE_FRAGMENT = 0x11;

    }

    private int senderHash;

    /**
     * Instantiates a Packet with an unknown sender (-1).
     */
    protected Packet() {
        this(-1);
    }

    /**
     * Instantiates a Packet with a known sender.
     *
     * @param senderHash the sender of this packet
     */
    protected Packet(int senderHash) {
        this.senderHash = senderHash;
    }

    /**
     * @return The sender of this packet.
     */
    public int getSenderHash() {
        return senderHash;
    }

    /**
     * Sets the sender of this packet.
     *
     * @param senderHash the new sender of this packet
     */
    public void setSenderHash(int senderHash) {
        this.senderHash = senderHash;
    }

    /**
     * Invokes when a packet of the given type is received. The implementation needs to fill its data by reading from the given {@link java.io.DataInputStream}.
     *
     * @param dis the stream to read the data from
     * @throws IOException If an I/O exception occurs.
     */
    public abstract void receive(DataInputStream dis) throws IOException;

    /**
     * Invokes when a packet of the given type needs to be sent. The implementation needs to sent its data through the given {@link java.io.DataOutputStream}.
     *
     * @param dos the stream to read the data from
     * @throws IOException If an I/O exception occurs.
     */
    public abstract void send(DataOutputStream dos) throws IOException;

}
