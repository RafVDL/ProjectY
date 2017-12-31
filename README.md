# SystemY
## Naming server
The naming server keeps track of the nodes in the network. It contains information about the hash of the node and its IP address.

Nodes can request information from the naming server using UDP or RMI. This includes the IP address of nodes, the owner of a file, and neighbours of a node.

### Starting the naming server
The naming server can be started by running the `be.ac.ua.dist.systemy.namingServer.NamingServer` class. An IP address will be asked for on which the naming server will bind to.

## Node
Every node can add and remove files to the network. At all times, files are replicated on at least two nodes.

### Starting the node
When starting the node, it will ask for a hostname for this node and the IP address to bind to.
#### Console mode
The console node can be started by running the `be.ac.ua.dist.systemy.node.Node` class.
#### GUI mode
The GUI node can be started by running the `be.ac.ua.dist.systemy.node.NodeMain` class.
