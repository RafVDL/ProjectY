package be.ac.ua.dist.systemy.networking.udp;

import be.ac.ua.dist.systemy.networking.Client;
import be.ac.ua.dist.systemy.networking.packet.Packet;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

public class UDPClient implements Client {

    private final InetAddress address;
    private final DatagramSocket socket;
    private final int port;

    public UDPClient(InetAddress address, int port) throws SocketException {
        socket = new DatagramSocket();
        this.address = address;
        this.port = port;
    }

    @Override
    public void sendPacket(Packet packet) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream(1024);
            DataOutputStream dos = new DataOutputStream(baos);

            dos.writeShort(packet.getId());
            packet.send(dos);

            dos.close();

            DatagramPacket datagramPacket = new DatagramPacket(baos.toByteArray(), baos.size(), address, port);
            socket.send(datagramPacket);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void close() {
        socket.close();
    }

}
