package be.ac.ua.dist.systemy.networking;

import be.ac.ua.dist.systemy.networking.packet.Packet;

import java.io.Closeable;
import java.io.DataOutputStream;
import java.io.Flushable;
import java.io.IOException;

public interface Connection extends Flushable, Closeable {

    boolean isClosed();

    void sendPacket(Packet packet) throws IOException;

    DataOutputStream getDataOutputStream();

}
