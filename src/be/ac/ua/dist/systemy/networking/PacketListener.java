package be.ac.ua.dist.systemy.networking;

import be.ac.ua.dist.systemy.networking.packet.Packet;

public interface PacketListener {

    void receivePacket(Packet packet);

}
