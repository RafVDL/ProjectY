package be.ac.ua.dist.systemy.networking.tcp;

import be.ac.ua.dist.systemy.networking.Client;
import be.ac.ua.dist.systemy.networking.packet.Packet;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;

public class TCPClient implements Client {

    private Socket clientSocket;
    private DataOutputStream dos;

    public TCPClient(InetAddress address, int port) {
        try {
            clientSocket = new Socket();
            clientSocket.setSoLinger(true, 5);
            clientSocket.connect(new InetSocketAddress(address, port));
            dos = new DataOutputStream(clientSocket.getOutputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public TCPClient(Socket clientSocket) {
        this.clientSocket = clientSocket;
        try {
            dos = new DataOutputStream(clientSocket.getOutputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void sendPacket(Packet packet) {
        try {
            dos.writeShort(packet.getId());
            packet.send(dos);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void close() {
        try {
            dos.close();
            clientSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
