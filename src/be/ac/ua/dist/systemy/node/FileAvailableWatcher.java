package be.ac.ua.dist.systemy.node;

import be.ac.ua.dist.systemy.Constants;

import java.io.File;
import java.nio.file.Files;

/**
 * When a file is copied (on Windows 10), an empty file is first created. Then the file size is set and allocated on disk. The last step is to write the actual data into that file.
 * This class waits for the copying to finnish before trying to introduce it into the network.
 */
public class FileAvailableWatcher implements Runnable {
    private final Node node;
    private final File file;

    public FileAvailableWatcher(Node node, File file) {
        this.node = node;
        this.file = file;
    }

    @Override
    public void run() {
        do {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } while (!Files.isReadable(file.toPath()));
        FileHandle fileHandle = new FileHandle(file.getName(), file.getParent().equals(Constants.LOCAL_FILES_PATH.substring(0, Constants.LOCAL_FILES_PATH.length() - 1)));
        fileHandle.setLocalAddress(node.getOwnAddress());
        node.addFileToNetwork(fileHandle);
    }
}
