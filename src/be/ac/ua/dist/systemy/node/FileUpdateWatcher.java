package be.ac.ua.dist.systemy.node;

import java.io.IOException;
import java.nio.file.*;
import java.util.List;

public class FileUpdateWatcher extends Thread {
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
            // Create watcher object for the directory
            WatchService watcher = dirPath.getFileSystem().newWatchService();
            // Register the watcher for all possible events
            dirPath.register(watcher, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_DELETE, StandardWatchEventKinds.ENTRY_MODIFY);
            WatchKey watchKey = watcher.take();

            // Put all events in a list
            List<WatchEvent<?>> events = watchKey.pollEvents();
            for (WatchEvent event : events) {
                if (event.kind() == StandardWatchEventKinds.ENTRY_CREATE) {
                    System.out.println("Watcher detected: [NEW] - " + event.context());
                } else if (event.kind() == StandardWatchEventKinds.ENTRY_DELETE) {
                    System.out.println("Watcher detected: [DEL] - " + event.context());
                } else if (event.kind() == StandardWatchEventKinds.ENTRY_MODIFY) {
                    System.out.println("Watcher detected: [MOD] - " + event.context());
                }
            }


        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}
