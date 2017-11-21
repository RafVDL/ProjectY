package be.ac.ua.dist.systemy.networking;

import be.ac.ua.dist.systemy.networking.packet.HelloPacket;
import be.ac.ua.dist.systemy.networking.packet.NodeCountPacket;
import be.ac.ua.dist.systemy.networking.packet.Packet;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NetworkManager {

    private static Map<Integer, Class<? extends Packet>> packets = new HashMap<>();
    private static Map<Class<? extends Packet>, List<PacketListener>> packetListeners = new HashMap<>();

    static {
        packets.put(0x00, HelloPacket.class);
        packets.put(0x01, NodeCountPacket.class);
    }

    private NetworkManager() {

    }

    public static Class<? extends Packet> getPacketById(int id) {
        return packets.get(id);
    }

    public static void registerListener(Class<? extends Packet> clazz, PacketListener listener) {
        packetListeners.computeIfAbsent(clazz, k -> new ArrayList<>());
        packetListeners.get(clazz).add(listener);
    }

    public static List<PacketListener> getPacketListeners(Class<? extends Packet> clazz) {
        return packetListeners.getOrDefault(clazz, new ArrayList<>());
    }

}