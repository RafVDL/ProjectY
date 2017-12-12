package be.ac.ua.dist.systemy.networking.packet;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class GetIPPacket extends Packet {

    private int hash;

    public GetIPPacket() {
    }

    public GetIPPacket(int hash) {
        this.hash = hash;
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
