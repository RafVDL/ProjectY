package be.ac.ua.dist.systemy.networking.tcp;

import be.ac.ua.dist.systemy.networking.NetworkManager;
import be.ac.ua.dist.systemy.networking.packet.Packet;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.Socket;

public class TCPListenerRunnable implements Runnable {

    private final Socket clientSocket;

    public TCPListenerRunnable(Socket clientSocket) {
        this.clientSocket = clientSocket;
    }

    @Override
    public void run() {
        try {
            InputStream is = clientSocket.getInputStream();
            DataInputStream dis = new DataInputStream(is);
            short packetId = dis.readShort();

            Class<? extends Packet> packetClazz = NetworkManager.getPacketById(packetId);

            Packet packet = packetClazz.getConstructor().newInstance();
            packet.receive(dis);

            NetworkManager.getPacketListeners(packetClazz).forEach(packetListener -> packetListener.receivePacket(packet, new TCPClient(clientSocket)));
        } catch (IOException | IllegalAccessException | InstantiationException | NoSuchMethodException | InvocationTargetException e) {
            e.printStackTrace();
        }
    }

}
