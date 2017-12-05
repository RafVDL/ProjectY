package be.ac.ua.dist.systemy.networking;

import be.ac.ua.dist.systemy.networking.packet.Packet;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetAddress;

public class Client implements Closeable {

    private final InetAddress address;
    private final int port;
    private final Connection connection;

    protected Client(InetAddress address, int port, Connection connection) {
        this.address = address;
        this.port = port;
        this.connection = connection;
    }

    /**
     * Returns the address the client is connecting to.
     *
     * @return the address
     */
    public InetAddress getAddress() {
        return address;
    }

    /**
     * Returns the port the client is connecting to.
     *
     * @return the port
     */
    public int getPort() {
        return port;
    }

    /**
     * Returns the {@link Connection} used to send packets.
     *
     * @return the connection
     * @see be.ac.ua.dist.systemy.networking.udp.UDPConnection
     * @see be.ac.ua.dist.systemy.networking.tcp.TCPConnection
     */
    public Connection getConnection() {
        return connection;
    }

    /**
     * Sends a packet to this Client.
     *
     * @param packet the packet to send
     * @throws IOException if an I/O error occurs.
     */
    public void sendPacket(Packet packet) throws IOException {
        connection.sendPacket(packet);
    }

    /**
     * Blocks until a packet of the given type is received, and if the backing connection supports it.
     *
     * @param clazz the packet class to wait for
     * @return The packet once it is received
     * @throws IOException if an I/O exception occurs.
     */
    public <E extends Packet> E waitForPacket(Class<E> clazz) throws IOException {
        return connection.waitForPacket(clazz);
    }

    /**
     * Blocks until a packet is received, and if the backing connection supports it.
     *
     * @return The packet once it is received
     * @throws IOException if an I/O exception occurs.
     */
    public Packet waitForPacket() throws IOException {
        return connection.waitForPacket();
    }

    @Override
    public void close() throws IOException {
        connection.close();
    }

}
