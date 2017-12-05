package be.ac.ua.dist.systemy.node;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.List;

public class FileUpdateWatcher implements Runnable {
    private final Path dirPath;
    private final Node node;

    public FileUpdateWatcher(Node node, String directory) {
        this.dirPath = Paths.get(directory);
        this.node = node;
    }

    @Override
    public void run() {
        System.out.println("Started FileUpdateWatcher in dir: " + dirPath.getFileName());

        try {
            // Create Watcher object for the directory
            WatchService watcher = dirPath.getFileSystem().newWatchService();
            // Register the watcher for ENTRY_CREATE events
            dirPath.register(watcher, StandardWatchEventKinds.ENTRY_CREATE);
            WatchKey watchKey = watcher.take();

            // Put all events in a list and process them
            while (node.isRunning()) {
                List<WatchEvent<?>> events = watchKey.pollEvents();
                for (WatchEvent event : events) {
                    File file = new File(dirPath.getFileName() + "/" + event.context().toString());

                    // If a file is replicated onto this Node, the UpdateWatcher should not process it again
                    if (node.getDownloadingFiles().contains(file.getName())) {
                        System.out.println(file.getName() + " is still downloading, skipping update checker");
                        continue;
                    }

                    System.out.println("Watcher detected: [NEW] - " + event.context() + " ... waiting for file finnish copying");
                    // Create watcher object that waits for the file to be done copying
                    FileSizeWatcher fileSizeWatcher = new FileSizeWatcher(node, file);
                    Thread thread = new Thread(fileSizeWatcher);
                    thread.start();
                }
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}
