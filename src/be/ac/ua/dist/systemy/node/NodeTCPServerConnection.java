package be.ac.ua.dist.systemy.node;

import java.io.*;
import java.net.Socket;

public class NodeTCPServerConnection extends Thread {

    private Node node;
    private Socket clientSocket;

    public NodeTCPServerConnection(Node node, Socket clientSocket) {
        this.node = node;
        this.clientSocket = clientSocket;
    }

    /**
     * Listens for commands from other nodes.
     */
    @Override
    public void run() {
        System.out.println("Started new connection.");
        try {
            DataInputStream dis = new DataInputStream(clientSocket.getInputStream());
            DataOutputStream dos = new DataOutputStream(clientSocket.getOutputStream());
            BufferedReader in = new BufferedReader(new InputStreamReader(dis));

            switch (in.readLine()) {
                case "REQUESTFILE":
                    String fileName = in.readLine();
                    sendFile(dos, "server_" + fileName);
                    break;

                case "NODECOUNT":
                    int nodecount = dis.readInt();
                    node.setNamingServerAddress(clientSocket.getInetAddress());
                    if (nodecount < 1) {
                        node.updatePrev(node.getOwnAddress(), node.getOwnHash());
                        node.updateNext(node.getOwnAddress(), node.getOwnHash());
                    }
                    break;

                case "PREV_NEXT_NEIGHBOUR":
                    int prevHash = dis.readInt();
                    int nextHash = dis.readInt();


                    node.updatePrev(clientSocket.getInetAddress(), prevHash);
                    node.updateNext(null, nextHash);
//                    node.updateNeighbours(clientSocket.getInetAddress(), prevName);
//                    node.updateNeighbours(null, nextName);
                    break;

                case "PREV_NEIGHBOUR":
                    prevHash = dis.readInt();
                    node.updatePrev(clientSocket.getInetAddress(), prevHash);
                    break;

                case "NEXT_NEIGHBOUR":
                    nextHash = dis.readInt();
                    node.updateNext(clientSocket.getInetAddress(), nextHash);
                    break;

                case "QUIT":
                    int oldNeighbour = dis.readInt();
                    if (oldNeighbour == node.getOwnHash())
                        break;
                    int newNeighbour = dis.readInt();

                    if (node.getPrevHash() == oldNeighbour) {
                        node.updatePrev(null, newNeighbour);
                    }

                    if (node.getNextHash() == oldNeighbour) {
                        node.updateNext(null, newNeighbour);
                    }
                    break;
            }

            dis.close();
            dos.close();
            in.close();
            clientSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Sends a file via a DataOutputStream
     *
     * @param dos      the DataOutputStream to utilize
     * @param fileName the name of the file to send
     */
    private void sendFile(DataOutputStream dos, String fileName) {
        try {
            int fileSize = (int) new File(fileName).length();
            FileInputStream fis = new FileInputStream(fileName);
            byte[] buffer = new byte[4096];

            dos.writeInt(fileSize);
            while (fis.read(buffer) > 0) {
                dos.write(buffer);
            }

            fis.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
