package be.ac.ua.dist.systemy.networking;

import be.ac.ua.dist.systemy.networking.packet.Packet;

import java.io.IOException;
import java.net.InetAddress;

public class Client {

    private final InetAddress address;
    private final int port;
    private final Connection connection;

    protected Client(InetAddress address, int port, Connection connection) {
        this.address = address;
        this.port = port;
        this.connection = connection;
    }

    public InetAddress getAddress() {
        return address;
    }

    public int getPort() {
        return port;
    }

    public Connection getConnection() {
        return connection;
    }

    public void sendPacket(Packet packet) throws IOException {
        connection.sendPacket(packet);
    }

    public void close() throws IOException {
        connection.close();
    }

}
