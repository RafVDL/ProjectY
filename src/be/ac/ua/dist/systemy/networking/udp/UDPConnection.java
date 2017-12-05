package be.ac.ua.dist.systemy.networking.udp;

import be.ac.ua.dist.systemy.networking.Connection;
import be.ac.ua.dist.systemy.networking.NetworkManager;
import be.ac.ua.dist.systemy.networking.packet.Packet;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

public class UDPConnection implements Connection {

    private final DatagramSocket socket;
    private final InetAddress address;
    private final int port;

    private ByteArrayOutputStream baos = new ByteArrayOutputStream(1024);
    private DataOutputStream dos = new DataOutputStream(baos);

    public UDPConnection(InetAddress address, int port) throws SocketException {
        socket = new DatagramSocket();
        this.address = address;
        this.port = port;
    }

    @Override
    public boolean isClosed() {
        return socket.isClosed();
    }

    @Override
    public void close() {
        if (NetworkManager.DEBUG())
            System.out.println("[UDP] Closing socket to " + address.getHostAddress());

        socket.close();
    }

    @Override
    public void sendPacket(Packet packet) throws IOException {
        if (NetworkManager.DEBUG())
            System.out.println("[UDP] Sending packet with id " + NetworkManager.getPacketIdByObject(packet) + " to " + address.getHostAddress());

        flush();

        dos.writeShort(NetworkManager.getPacketIdByObject(packet));
        dos.writeInt(NetworkManager.getSenderHash());
        packet.send(dos);

        flush();
    }

    @Override
    public DataInputStream getDataInputStream() {
        throw new UnsupportedOperationException("DataInputStream not yet supported in UDP");
    }

    @Override
    public DataOutputStream getDataOutputStream() {
        return dos;
    }

    @Override
    public <E extends Packet> E waitForPacket(Class<E> clazz) {
        throw new UnsupportedOperationException("Waiting is not yet supported in UDP");
    }

    @Override
    public Packet waitForPacket() throws IOException {
        throw new UnsupportedOperationException("Waiting is not yet supported in UDP");
    }

    @Override
    public void flush() throws IOException {
        if (baos.size() == 0)
            return;

        DatagramPacket datagramPacket = new DatagramPacket(baos.toByteArray(), baos.size(), address, port);
        socket.send(datagramPacket);
        baos.reset();
    }
}
