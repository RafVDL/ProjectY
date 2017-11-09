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
                    if (nodecount < 1) {
                        node.updatePrev(node.getOwnAddress(), node.getOwnName());
                        node.updateNext(node.getOwnAddress(), node.getOwnName());
                    }
                    break;

                case "PREV_NEXT_NEIGHBOUR":
                    String prevName = in.readLine();
                    String nextName = in.readLine();

                    node.updatePrev(clientSocket.getInetAddress(), prevName);
                    node.updateNext(null, nextName);
//                    node.updateNeighbours(clientSocket.getInetAddress(), prevName);
//                    node.updateNeighbours(null, nextName);
                    break;

                case "QUIT":
                    String oldNeighbour = in.readLine();
                    if (oldNeighbour.equals(node.getOwnName()))
                        break;
                    String newNeighbour = in.readLine();

                    if (node.getPrevName().equals(oldNeighbour)) {
                        node.updatePrev(null, newNeighbour);
                    }

                    if (node.getNextName().equals(oldNeighbour)) {
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
