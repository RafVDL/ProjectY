package be.ac.ua.dist.systemy.networking.tcp;

import be.ac.ua.dist.systemy.networking.NetworkManager;
import be.ac.ua.dist.systemy.networking.packet.Packet;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.Socket;
import java.nio.ByteBuffer;

public class TCPListenerRunnable implements Runnable {

    private final Socket clientSocket;

    public TCPListenerRunnable(Socket clientSocket) {
        this.clientSocket = clientSocket;
    }

    @Override
    public void run() {
        try {
            InputStream is = clientSocket.getInputStream();
            byte[] buf = new byte[2]; // 2 byte (short) packet ID
            int byteRead = is.read(buf, 0, 2);
            if (byteRead < 2)
                return;
            ByteBuffer byteBuffer = ByteBuffer.wrap(buf);

            Class<? extends Packet> packetClazz = NetworkManager.getPacketById(byteBuffer.getShort());

            Packet packet = packetClazz.getConstructor().newInstance();
            DataInputStream dis = new DataInputStream(is);
            packet.receive(dis);
            dis.close();

            NetworkManager.getPacketListeners(packetClazz).forEach(packetListener -> packetListener.receivePacket(packet, new TCPClient(clientSocket)));
        } catch (IOException | IllegalAccessException | InstantiationException | NoSuchMethodException | InvocationTargetException e) {
            e.printStackTrace();
        }
    }

}
