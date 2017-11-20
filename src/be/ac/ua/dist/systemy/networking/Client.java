package be.ac.ua.dist.systemy.networking;

import be.ac.ua.dist.systemy.networking.packet.Packet;

public interface Client {

    void sendPacket(Packet packet);

    void close();

}
