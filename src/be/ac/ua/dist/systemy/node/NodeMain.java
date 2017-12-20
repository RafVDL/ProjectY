package be.ac.ua.dist.systemy.node;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Scanner;

public class NodeMain {
    public static void main(String[] args){
        try {
            // Get IP and hostname
            Scanner sc = new Scanner(System.in);
            System.out.println("(Detected localHostName is: " + InetAddress.getLocalHost() + ")");
            System.out.print("Enter hostname: ");
            String hostname = sc.nextLine();
            if (hostname.isEmpty()) {
                hostname = InetAddress.getLocalHost().getHostName();
            }
            System.out.println("(Detected localHostAddress is: " + InetAddress.getLocalHost() + ")");
            System.out.print("Enter IP: ");
            String ip = sc.nextLine();
            if (ip.isEmpty()) {
                ip = InetAddress.getLocalHost().getHostAddress();
            }


            // Create Node object and initialize
            Node node = Node.startNode(hostname, InetAddress.getByName(ip));
        } catch (UnknownHostException e) {
            e.printStackTrace();
            System.err.println("Unknown host detected, failed to start the Node.");
            return;
        }
    }
}
