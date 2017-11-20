package be.ac.ua.dist.systemy.networking.packet;

import be.ac.ua.dist.systemy.networking.Client;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public abstract class Packet {

    private final int id;
    protected Client client;

    public Packet(int id, Client client) {
        this.id = id;
        this.client = client;
    }

    public int getId() {
        return id;
    }

    public abstract void receive(DataInputStream dis) throws IOException;

    public abstract void send(DataOutputStream dos) throws IOException;

}
