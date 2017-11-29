package be.ac.ua.dist.systemy.networking.udp;

import be.ac.ua.dist.systemy.networking.PacketListener;
import be.ac.ua.dist.systemy.networking.Server;
import be.ac.ua.dist.systemy.networking.packet.Packet;

import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UnicastServer implements Server, Runnable {

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
    public void run() {
        try {
            DatagramSocket socket = new MulticastSocket(new InetSocketAddress(address, port));
            socket.setSoTimeout(2500);

            DatagramPacket packet;
            while (running) {
                try {
                    byte[] buf = new byte[1024];
                    packet = new DatagramPacket(buf, buf.length);
                    socket.receive(packet);

                    UnicastListenerRunnable runnable = new UnicastListenerRunnable(this, packet);
                    Thread thread = new Thread(runnable);
                    thread.start();
                } catch (SocketTimeoutException e) {
                    // don't do anything
                }
            }

            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
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

}
