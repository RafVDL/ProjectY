package be.ac.ua.dist.systemy.networking.tcp;

import be.ac.ua.dist.systemy.networking.Server;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;

public class TCPServer implements Server, Runnable {

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
    public void setRunning(boolean running) {
        this.running = running;
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
                TCPListenerRunnable runnable = new TCPListenerRunnable(clientSocket);
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
