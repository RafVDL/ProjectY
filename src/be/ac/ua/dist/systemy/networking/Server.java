package be.ac.ua.dist.systemy.networking;

import java.net.InetAddress;

public interface Server {

    boolean startServer(InetAddress address, int port);

    void stop();

}
