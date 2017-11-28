package be.ac.ua.dist.systemy.networking;

import be.ac.ua.dist.systemy.networking.packet.Packet;

import java.io.IOException;

public interface Connection {

    boolean isClosed();

    void close() throws IOException;

    void sendPacket(Packet packet) throws IOException;

}
