package be.ac.ua.dist.systemy.networking.tcp;

import be.ac.ua.dist.systemy.networking.PacketListener;
import be.ac.ua.dist.systemy.networking.Server;
import be.ac.ua.dist.systemy.networking.packet.Packet;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Handles incoming TCP packets.
 */
public class TCPServer implements Server, Runnable {

    private volatile boolean running = true;
    private InetAddress address;
    private int port;

    private static Map<Class<? extends Packet>, List<PacketListener>> packetListeners = new HashMap<>();

    @Override
    public boolean startServer(InetAddress address, int port) {
        this.address = address;
        this.port = port;
        Thread thread = new Thread(this);
        thread.start();
        return true;
    }

    @Override
    public void stop() {
        this.running = false;
    }

    @Override
    public <T extends Packet> void registerListener(Class<T> clazz, PacketListener<T> listener) {
        packetListeners.computeIfAbsent(clazz, k -> new ArrayList<>());
        packetListeners.get(clazz).add(listener);
    }

    @Override
    public List<PacketListener> getPacketListeners(Class<? extends Packet> clazz) {
        return packetListeners.getOrDefault(clazz, new ArrayList<>());
    }

    @Override
    public void run() {
        ServerSocket serverSocket;
        try {
            serverSocket = new ServerSocket(port, 0, address);
            serverSocket.setSoTimeout(2000);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        System.out.println("Started TCPServer");
        while (running) {
            try {
                Socket clientSocket = serverSocket.accept();
                TCPListenerRunnable runnable = new TCPListenerRunnable(this, clientSocket);
                Thread thread = new Thread(runnable);
                thread.start();
            } catch (SocketTimeoutException e) {
                // nothing
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}
