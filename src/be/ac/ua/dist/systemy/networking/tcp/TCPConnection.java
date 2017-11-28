package be.ac.ua.dist.systemy.networking.tcp;

import be.ac.ua.dist.systemy.networking.Connection;
import be.ac.ua.dist.systemy.networking.packet.Packet;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;

public class TCPConnection implements Connection {

    private Socket clientSocket;
    private DataOutputStream dos;

    public TCPConnection(InetAddress address, int port) throws IOException {
        clientSocket = new Socket();
        clientSocket.setSoLinger(true, 5);
        clientSocket.connect(new InetSocketAddress(address, port));
    }

    public TCPConnection(Socket socket) throws IOException {
        clientSocket = socket;
        dos = new DataOutputStream(clientSocket.getOutputStream());
    }

    @Override
    public boolean isClosed() {
        return clientSocket.isClosed();
    }

    @Override
    public void close() throws IOException {
        dos.close();
        clientSocket.close();
    }

    @Override
    public void sendPacket(Packet packet) throws IOException {
        dos.writeShort(packet.getId());
        packet.send(dos);
    }

}
