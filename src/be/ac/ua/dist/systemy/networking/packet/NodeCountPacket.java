package be.ac.ua.dist.systemy.networking.packet;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class NodeCountPacket extends Packet {

    private int nodeCount;

    public NodeCountPacket() {
    }

    public NodeCountPacket(int nodeCount) {
        this.nodeCount = nodeCount;
    }

    @Override
    public void receive(DataInputStream dis) throws IOException {
        nodeCount = dis.readInt();
    }

    @Override
    public void send(DataOutputStream dos) throws IOException {
        dos.writeInt(nodeCount);
    }

    public int getNodeCount() {
        return nodeCount;
    }

    public void setNodeCount(int nodeCount) {
        this.nodeCount = nodeCount;
    }

}
