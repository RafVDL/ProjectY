package be.ac.ua.dist.systemy.node;

import be.ac.ua.dist.systemy.Constants;

import java.io.*;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class FileHandle {

    private String fileName;
    private boolean local;
    private File logFile;
    private int downloads;
    private final Set<Integer> availableNodes = new LinkedHashSet<>();

    public FileHandle(String fileName, boolean local) {
        this.fileName = fileName;
        logFile = new File(Constants.LOGS_PATH + fileName);
    }

    public File getFile() {
        return new File((local ? Constants.LOCAL_FILES_PATH : Constants.REPLICATED_FILES_PATH) + fileName);
    }

    public void setLocal(boolean local) {
        this.local = local;
    }

    public File getLogFile() {
        return logFile;
    }

    public void setLogFile(File logFile) {
        this.logFile = logFile;
    }

    public int getDownloads() {
        return downloads;
    }

    public void increaseDownloads() {
        downloads++;
    }

    public void setDownloads(int downloads) {
        this.downloads = downloads;
    }

    public Set<Integer> getAvailableNodes() {
        return availableNodes;
    }

    public void readLog() {
        try {
            BufferedReader in = new BufferedReader(new FileReader(logFile));
            availableNodes.addAll(Arrays.stream(in.readLine().split(",")).map(Integer::parseInt).collect(Collectors.toList()));
            downloads = Integer.parseInt(in.readLine());
            in.close();
        } catch (FileNotFoundException e) {
            System.err.println("Log file of " + fileName + " does not exist");
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NumberFormatException e) {
            System.err.println("Failed to read log of " + fileName + "; unable to read numbers");
        }
    }

    public void writeLog() {
        try {
            PrintWriter writer = new PrintWriter(logFile);
            writer.println(availableNodes.stream().map(Object::toString).collect(Collectors.joining(",")));
            writer.println(downloads);
            writer.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof FileHandle && ((FileHandle) o).fileName.equals(this.fileName);
    }

}
