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
        public static final short NODECOUNT = 0x01;

        /**
         * Requests a node's ip using multicast.
         */
        public static final short GETIP = 0x02;

        /**
         * Sent by a node using multicast to the naming server to notify it is quitting the network.
         */
        public static final short QUITNAMING = 0x03;

        /**
         * Sent by naming server as response to a {@link #GETIP} packet.
         */
        public static final short IPRESPONSE = 0x04;

    }

    private final int id;

    public Packet(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public abstract void receive(DataInputStream dis) throws IOException;

    public abstract void send(DataOutputStream dos) throws IOException;

}
