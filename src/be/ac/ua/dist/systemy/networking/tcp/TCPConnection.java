package be.ac.ua.dist.systemy.networking.tcp;

import be.ac.ua.dist.systemy.networking.Connection;
import be.ac.ua.dist.systemy.networking.NetworkManager;
import be.ac.ua.dist.systemy.networking.packet.Packet;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;

public class TCPConnection implements Connection {

    private final Socket socket;
    private final DataInputStream dis;
    private final DataOutputStream dos;

    public TCPConnection(InetAddress address, int port) throws IOException {
        socket = new Socket();
        socket.setSoLinger(true, 5);
        socket.connect(new InetSocketAddress(address, port));
        dis = new DataInputStream(socket.getInputStream());
        dos = new DataOutputStream(socket.getOutputStream());
    }

    public TCPConnection(Socket socket) throws IOException {
        this.socket = socket;
        dis = new DataInputStream(socket.getInputStream());
        dos = new DataOutputStream(this.socket.getOutputStream());
    }

    @Override
    public boolean isClosed() {
        return socket.isClosed();
    }

    @Override
    public void close() throws IOException {
        if (NetworkManager.DEBUG())
            System.out.println("[TCP] Closing socket to " + socket.getInetAddress().getHostAddress());

        dos.close();
        socket.close();
    }

    @Override
    public void sendPacket(Packet packet) throws IOException {
        if (NetworkManager.DEBUG())
            System.out.println("[TCP] Sending packet with id " + NetworkManager.getPacketIdByObject(packet) + " to " + socket.getInetAddress().getHostAddress());

        dos.writeShort(NetworkManager.getPacketIdByObject(packet));
        dos.writeInt(NetworkManager.getSenderHash());
        packet.send(dos);
    }

    @Override
    public DataInputStream getDataInputStream() {
        return dis;
    }

    @Override
    public DataOutputStream getDataOutputStream() {
        return dos;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <E extends Packet> E waitForPacket(Class<E> clazz) throws IOException {
        try {
            Packet packet = TCPUtil.readPacket(socket, dis);

            if (packet == null)
                return null;

            if (clazz.isInstance(packet)) {
                return (E) packet;
            }

            System.err.println("Received packet " + packet.getClass().getSimpleName() + "; expected " + clazz.getSimpleName() + ". State error of other peer?");
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | InstantiationException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public void flush() throws IOException {
        dos.flush();
    }

}
