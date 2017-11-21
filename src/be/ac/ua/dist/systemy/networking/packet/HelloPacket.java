package be.ac.ua.dist.systemy.networking.packet;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class HelloPacket extends Packet {

    private int hash;

    public HelloPacket() {
        super(0x00);
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
