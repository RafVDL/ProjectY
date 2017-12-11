package be.ac.ua.dist.systemy.node;

public class FailureAgent implements Runnable, Serializable {
    Node node;
    int failedNode;
    
    public FailureAgent(int failedNode, Node node){ //integer is hash of node that is downloading file
        this.node = node;
        this.failedNode = failedNode;
    }
}
