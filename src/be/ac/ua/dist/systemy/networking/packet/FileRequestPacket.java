package be.ac.ua.dist.systemy.networking.packet;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class FileRequestPacket extends Packet {

    private String fileName;

    public FileRequestPacket() {
    }

    public FileRequestPacket(String fileName) {
        this.fileName = fileName;
    }

    @Override
    public void receive(DataInputStream dis) throws IOException {

    }

    @Override
    public void send(DataOutputStream dos) throws IOException {

    }

}
