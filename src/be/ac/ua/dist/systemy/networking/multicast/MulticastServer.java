package be.ac.ua.dist.systemy.networking.multicast;

import be.ac.ua.dist.systemy.networking.Server;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;

public class MulticastServer implements Server, Runnable {

    private boolean running = true;
    private InetAddress address;
    private int port;

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
        MulticastSocket multicastSocket;
        try {
            multicastSocket = new MulticastSocket(port);
            multicastSocket.setLoopbackMode(false);
            multicastSocket.joinGroup(address);

            DatagramPacket packet;
            while (running) {
                byte[] buf = new byte[1024];
                packet = new DatagramPacket(buf, buf.length);
                multicastSocket.receive(packet);

                MulticastListenerRunnable runnable = new MulticastListenerRunnable(packet);
                Thread thread = new Thread(runnable);
                thread.start();
            }

            multicastSocket.leaveGroup(address);
            multicastSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
