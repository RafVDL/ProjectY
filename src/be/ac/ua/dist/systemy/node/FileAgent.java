package be.ac.ua.dist.systemy.node;
import java.io.Serializable;
import java.util.Set;
import java.util.TreeMap;

/*https://stackoverflow.com/questions/3429921/what-does-serializable-mean*/

public class FileAgent implements Runnable, Serializable{
    // to run: (new Thread(new FileAgent())).start();
    TreeMap<String, Integer> files = new TreeMap<>();
    Node node;
    private Set<String> localFiles;
    private String lockRequest;



    public FileAgent(TreeMap<String, Integer> files, Node node){ //integer is hash of node that is downloading file
        this.files = files;
        this.node = node;
        }

    public void run(){
        localFiles = node.getLocalFiles();
        //Stap 1: voeg localFiles toe aan map met files
        if(localFiles != null) {
            for (String s : localFiles) {
                if (!files.containsKey(s)) {
                    files.put(s, 0);
                }
            }
        }
        //Stap 2: update de lijst van bestanden
        node.emptyAllFileList();
        if(files != null) {
            files.forEach((key, value) -> {
                node.addAllFileList(key);
            });

            //Stap 3: checken naar lock request
            lockRequest = node.getFileLockRequest();
            if(!lockRequest.equals("null")) {
                if (files.get(lockRequest) == 0) {
                    node.setDownloadFileGranted(lockRequest);
                    files.put(lockRequest, node.getOwnHash());
                    node.setFileLockRequest("null");
                }
            }
            if(!node.getDownloadFileGranted().equals("null")){
                if (!node.getDownloadingFiles().contains(node.getDownloadFileGranted())) { //file downloaded
                    node.setDownloadFileGranted("null");
                    files.put(lockRequest, 0);
                }
            }


            node.setFiles(this.files);
        }
        return;


    }



}
