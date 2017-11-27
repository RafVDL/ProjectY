package be.ac.ua.dist.systemy.node;

import java.io.File;
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

            // Put all events in a list and process them
            while (node.isRunning()) {
                List<WatchEvent<?>> events = watchKey.pollEvents();
                for (WatchEvent event : events) {
                    File file = new File(dirPath.getFileName() + "/" + event.context().toString());

                    if (event.kind() == StandardWatchEventKinds.ENTRY_CREATE) {
                        System.out.println("Watcher detected: [NEW] - " + event.context());
                    } else if (event.kind() == StandardWatchEventKinds.ENTRY_DELETE) {
                        System.out.println("Watcher detected: [DEL] - " + event.context());

                    /* When file is modified, file system first creates it with 0 bytes and fires a modify event
                    and then writes data on it. Then it fires the modify event again.
                    => check for fileSize > 0 */
                    } else if (event.kind() == StandardWatchEventKinds.ENTRY_MODIFY && file.length() > 0) {
                        System.out.println("Watcher detected: [MOD] - " + event.context());
                    }
                }
            }


        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}
