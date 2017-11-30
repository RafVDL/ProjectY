package be.ac.ua.dist.systemy.networking;

import be.ac.ua.dist.systemy.networking.packet.Packet;

import java.io.IOException;

public interface PacketListener<E extends Packet> {

    void receivePacket(E packet, Client client) throws IOException;

}
