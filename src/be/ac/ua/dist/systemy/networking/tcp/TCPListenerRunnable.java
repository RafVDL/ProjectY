package be.ac.ua.dist.systemy.networking.tcp;

import be.ac.ua.dist.systemy.networking.Communications;
import be.ac.ua.dist.systemy.networking.packet.Packet;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.Socket;

public class TCPListenerRunnable implements Runnable {

    private final TCPServer tcpServer;
    private final Socket socket;

    public TCPListenerRunnable(TCPServer tcpServer, Socket socket) {
        this.tcpServer = tcpServer;
        this.socket = socket;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void run() {
        try {
            InputStream is = socket.getInputStream();
            DataInputStream dis = new DataInputStream(is);
            while (!socket.isClosed()) {
                Packet packet = TCPUtil.readPacket(socket, dis);

                if (packet == null)
                    continue;

                tcpServer.getPacketListeners(packet.getClass()).forEach(packetListener -> {
                    try {
                        packetListener.receivePacket(packet, Communications.getTCPClient(socket.getInetAddress(), socket.getPort(), socket));
                    } catch (IOException e) {
                        System.err.println("[Unicast] Error handling packet " + packet.getClass().getSimpleName() + ":");
                        e.printStackTrace();
                        try {
                            socket.close();
                        } catch (IOException e1) {
                            System.err.println("[Unicast] Error closing socket after error handling packet " + packet.getClass().getSimpleName() + ":"); // yes.
                            e1.printStackTrace();
                        }
                    }
                });
            }
        } catch (EOFException e) {
            System.out.println("[TCP] Peer closed connection");
            try {
                socket.close();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        } catch (IOException | IllegalAccessException | InstantiationException | NoSuchMethodException | InvocationTargetException e) {
            e.printStackTrace();
        }
    }

}
