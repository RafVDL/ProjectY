package be.ac.ua.dist.systemy.networking.packet;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class UpdateNeighboursPacket extends Packet {

    private int previousNeighbour = -1;
    private int nextNeighbour = -1;

    public UpdateNeighboursPacket() {
    }

    public UpdateNeighboursPacket(int previousNeighbour, int nextNeighbour) {
        this.previousNeighbour = previousNeighbour;
        this.nextNeighbour = nextNeighbour;
    }

    @Override
    public void receive(DataInputStream dis) throws IOException {
        previousNeighbour = dis.readInt();
        nextNeighbour = dis.readInt();
    }

    @Override
    public void send(DataOutputStream dos) throws IOException {
        dos.writeInt(previousNeighbour);
        dos.writeInt(nextNeighbour);
    }

    public int getPreviousNeighbour() {
        return previousNeighbour;
    }

    public int getNextNeighbour() {
        return nextNeighbour;
    }

}
