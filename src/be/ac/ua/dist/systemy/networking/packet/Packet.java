package be.ac.ua.dist.systemy.networking.packet;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public abstract class Packet {

    private final int id;

    public Packet(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public abstract void receive(DataInputStream dis) throws IOException;

    public abstract void send(DataOutputStream dos) throws IOException;

}
