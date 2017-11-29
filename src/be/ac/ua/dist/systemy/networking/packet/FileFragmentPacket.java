package be.ac.ua.dist.systemy.networking.packet;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class FileFragmentPacket extends Packet {

    private int fileHash;
    private int remainingBytes;
    private byte[] data = new byte[4096];
    private int length;

    public FileFragmentPacket() {
    }

    public FileFragmentPacket(int fileHash, int remainingBytes, byte[] data) {
        this.fileHash = fileHash;
        this.remainingBytes = remainingBytes;
        this.data = data;
        length = data.length;
    }

    @Override
    public void receive(DataInputStream dis) throws IOException {
        fileHash = dis.readInt();
        remainingBytes = dis.readInt();
        length = dis.read(data, 0, Math.max(remainingBytes, 1024));
    }

    @Override
    public void send(DataOutputStream dos) throws IOException {
        dos.writeInt(fileHash);
        dos.writeInt(remainingBytes);
        dos.write(data, 0, length);
    }

    public int getFileHash() {
        return fileHash;
    }

    public void setFileHash(int fileHash) {
        this.fileHash = fileHash;
    }

    public int getRemainingBytes() {
        return remainingBytes;
    }

    public void setRemainingBytes(int remainingBytes) {
        this.remainingBytes = remainingBytes;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    public int getLength() {
        return length;
    }

    public void setLength(int length) {
        this.length = length;
    }

}
