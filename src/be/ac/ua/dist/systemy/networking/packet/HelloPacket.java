package be.ac.ua.dist.systemy.networking.packet;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class HelloPacket extends Packet {

    public HelloPacket() {
    }

    @Override
    public void receive(DataInputStream dis) throws IOException {
        // empty
    }

    @Override
    public void send(DataOutputStream dos) throws IOException {
        // empty
    }

}
