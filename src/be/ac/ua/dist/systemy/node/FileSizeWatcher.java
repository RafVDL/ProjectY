package be.ac.ua.dist.systemy.node;

import java.io.File;

/**
 *  When a file is copied (on Windows 10), an empty file is first created. Then the actual data is copied into that file.
 *  This class waits for the copying to finnish before trying to introduce it into the network.
 */
public class FileSizeWatcher implements Runnable {
    private final Node node;
    private final File file;

    public FileSizeWatcher(Node node, File file) {
        this.node = node;
        this.file = file;
    }

    @Override
    public void run() {
        long oldFileSize = file.length();
        while (true) {
            long newFileSize = file.length();
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            if (newFileSize == oldFileSize) {
                node.addFileToNetwork(file.getParent() + "/", file.getName());
                break;
            }
        }
    }
}
