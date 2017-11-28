package be.ac.ua.dist.systemy.networking.multicast;

import be.ac.ua.dist.systemy.Constants;
import be.ac.ua.dist.systemy.networking.NetworkManager;
import be.ac.ua.dist.systemy.networking.packet.Packet;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.DatagramPacket;
import java.net.SocketException;

public class MulticastListenerRunnable implements Runnable {

    private final DatagramPacket datagramPacket;

    public MulticastListenerRunnable(DatagramPacket datagramPacket) {
        this.datagramPacket = datagramPacket;
    }

    @Override
    public void run() {
        try {
            ByteArrayInputStream bais = new ByteArrayInputStream(datagramPacket.getData());
            DataInputStream dis = new DataInputStream(bais);
            short packetId = dis.readShort();

            Class<? extends Packet> packetClazz = NetworkManager.getPacketById(packetId);

            Packet packet = packetClazz.getConstructor().newInstance();
            packet.receive(dis);

            NetworkManager.getPacketListeners(packetClazz).forEach(packetListener -> {
                try {
                    packetListener.receivePacket(packet, NetworkManager.getUDPClient(datagramPacket.getAddress(), Constants.UNICAST_PORT));
                } catch (SocketException e) {
                    e.printStackTrace();
                }
            });
        } catch (IOException | NoSuchMethodException | InstantiationException | InvocationTargetException | IllegalAccessException e) {
            e.printStackTrace();
        }
    }

}
