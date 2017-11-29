package be.ac.ua.dist.systemy.networking.udp;

import be.ac.ua.dist.systemy.Constants;
import be.ac.ua.dist.systemy.networking.NetworkManager;
import be.ac.ua.dist.systemy.networking.packet.Packet;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.DatagramPacket;
import java.net.SocketException;

public class UnicastListenerRunnable implements Runnable {

    private final DatagramPacket datagramPacket;

    public UnicastListenerRunnable(DatagramPacket datagramPacket) {
        this.datagramPacket = datagramPacket;
    }

    @Override
    public void run() {
        try {
            ByteArrayInputStream bais = new ByteArrayInputStream(datagramPacket.getData());
            DataInputStream dis = new DataInputStream(bais);
            short packetId = dis.readShort();

            Class<? extends Packet> packetClazz = NetworkManager.getPacketById(packetId);

            if (NetworkManager.DEBUG())
                System.out.println("[Unicast] Received packet " + packetId + " from " + datagramPacket.getAddress().getHostAddress());

            if (packetClazz == null) {
                System.err.println("[Unicast] Received unknown packet id " + packetId + " from " + datagramPacket.getAddress().getHostAddress());
                dis.close();
                return;
            }

            int senderHash = dis.readInt();

            Packet packet = packetClazz.getConstructor().newInstance();
            packet.setSenderHash(senderHash);
            packet.receive(dis);

            NetworkManager.getPacketListeners(packetClazz).forEach(packetListener -> {
                try {
                    packetListener.receivePacket(packet, NetworkManager.getUDPClient(datagramPacket.getAddress(), Constants.UNICAST_PORT));
                } catch (SocketException e) {
                    e.printStackTrace();
                }
            });

            dis.close();
        } catch (IOException | NoSuchMethodException | InstantiationException | InvocationTargetException | IllegalAccessException e) {
            e.printStackTrace();
        }
    }

}
