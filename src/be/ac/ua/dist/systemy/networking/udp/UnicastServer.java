package be.ac.ua.dist.systemy.networking.udp;

import be.ac.ua.dist.systemy.networking.Server;

import java.io.IOException;
import java.net.*;

public class UnicastServer implements Server, Runnable {

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
        try {
            DatagramSocket socket = new MulticastSocket(new InetSocketAddress(address, port));
            socket.setSoTimeout(2500);

            DatagramPacket packet;
            while (running) {
                try {
                    byte[] buf = new byte[1024];
                    packet = new DatagramPacket(buf, buf.length);
                    socket.receive(packet);

                    UnicastListenerRunnable runnable = new UnicastListenerRunnable(packet);
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

}
