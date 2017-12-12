package be.ac.ua.dist.systemy.networking.packet;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;

public class IPResponsePacket extends Packet {

    private InetAddress address;

    public IPResponsePacket() {
    }

    public IPResponsePacket(InetAddress address) {
        this.address = address;
    }

    @Override
    public void receive(DataInputStream dis) throws IOException {
        byte[] buf = new byte[4];
        if (dis.read(buf, 0, 4) == 4) {
            this.address = (buf[0] == 0 && buf[1] == 0 && buf[2] == 0 && buf[3] == 0) ? null : InetAddress.getByAddress(buf);
        } else {
            throw new IOException("Incorrect number of data bytes sent; expected 4 bytes");
        }
    }

    @Override
    public void send(DataOutputStream dos) throws IOException {
        byte[] buf = {0, 0, 0, 0};
        if (address != null)
            buf = address.getAddress();
        dos.write(buf);
    }

    public InetAddress getAddress() {
        return address;
    }

    public void setAddress(InetAddress address) {
        this.address = address;
    }

}
