package be.ac.ua.dist.systemy.networking.packet;

import be.ac.ua.dist.systemy.networking.Client;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class HelloPacket extends Packet {

    private int hash;

    public HelloPacket(Client client) {
        super(0x00, client);
    }

    @Override
    public void receive(DataInputStream dis) throws IOException {
        hash = dis.readInt();
    }

    @Override
    public void send(DataOutputStream dos) throws IOException {
        dos.writeInt(hash);
    }

    public int getHash() {
        return hash;
    }

    public void setHash(int hash) {
        this.hash = hash;
    }

}
