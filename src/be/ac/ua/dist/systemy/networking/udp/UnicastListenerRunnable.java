package be.ac.ua.dist.systemy.networking.udp;

import be.ac.ua.dist.systemy.Constants;
import be.ac.ua.dist.systemy.networking.Communications;
import be.ac.ua.dist.systemy.networking.packet.Packet;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.DatagramPacket;
import java.net.SocketException;

/**
 * This class is instantiated and ran as a new Thread when a known packet is received by the {@link UnicastServer}.
 */
public class UnicastListenerRunnable implements Runnable {

    private final UnicastServer unicastServer;
    private final DatagramPacket datagramPacket;

    UnicastListenerRunnable(UnicastServer unicastServer, DatagramPacket datagramPacket) {
        this.unicastServer = unicastServer;
        this.datagramPacket = datagramPacket;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void run() {
        try {
            ByteArrayInputStream bais = new ByteArrayInputStream(datagramPacket.getData());
            DataInputStream dis = new DataInputStream(bais);
            short packetId = dis.readShort();

            Class<? extends Packet> packetClazz = Communications.getPacketById(packetId);

            if (packetClazz == null) {
                System.err.println("[Unicast] Received unknown packet id " + packetId + " from " + datagramPacket.getAddress().getHostAddress());
                dis.close();
                return;
            }

            if (Communications.DEBUG())
                System.out.println("[Unicast] Received packet " + packetId + " from " + datagramPacket.getAddress().getHostAddress());

            int senderHash = dis.readInt();

            Packet packet = packetClazz.getConstructor().newInstance();
            packet.setSenderHash(senderHash);
            packet.receive(dis);

            unicastServer.getPacketListeners(packetClazz).forEach(packetListener -> {
                try {
                    packetListener.receivePacket(packet, Communications.getUDPClient(datagramPacket.getAddress(), Constants.UNICAST_PORT));
                } catch (SocketException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    System.err.println("[Unicast] Error handling packet id " + packetId + ":");
                    e.printStackTrace();
                }
            });

            dis.close();
        } catch (IOException | NoSuchMethodException | InstantiationException | InvocationTargetException | IllegalAccessException e) {
            e.printStackTrace();
        }
    }

}
