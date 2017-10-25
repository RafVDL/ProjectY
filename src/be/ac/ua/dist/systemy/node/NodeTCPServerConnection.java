package be.ac.ua.dist.systemy.node;

import java.io.*;
import java.net.Socket;

public class NodeTCPServerConnection extends Thread {
    private Socket clientSocket;

    public NodeTCPServerConnection(Socket clientSocket) {
        this.clientSocket = clientSocket;
    }

    /**
     * Listens for the first input line (ie. REQUESTFILE) followed by a filename as second parameter. The switch statement
     * is already implemented for expansion reasons
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
