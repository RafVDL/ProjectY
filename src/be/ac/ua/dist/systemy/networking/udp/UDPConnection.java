package be.ac.ua.dist.systemy.networking.udp;

import be.ac.ua.dist.systemy.networking.Connection;
import be.ac.ua.dist.systemy.networking.packet.Packet;

import java.io.ByteArrayOutputStream;
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
        socket.close();
    }

    @Override
    public void sendPacket(Packet packet) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream(1024);
        DataOutputStream dos = new DataOutputStream(baos);

        dos.writeShort(packet.getId());
        packet.send(dos);

        dos.close();

        DatagramPacket datagramPacket = new DatagramPacket(baos.toByteArray(), baos.size(), address, port);
        socket.send(datagramPacket);
    }

}
