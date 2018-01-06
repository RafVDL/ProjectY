# SystemY
The binaries for both the naming server and the node can be downloaded from [Releases](https://github.com/RafVDL/SystemY/releases).

> Before setting up the network, firewalls should be disabled on all the computers that are used.

## Naming server
The naming server keeps track of information of the nodes in the network (hash of the node and its IP address). On top of that, the naming server makes sure the FileAgent keeps running if a node would fail.

Nodes can request information from the naming server using UDP or RMI. This includes the IP address of nodes, the owner of a file, and neighbours of a node.

### Starting the naming server
The naming server can be started directly from an IDE or by using the binaries. Upon starting the naming server, it will ask for the IP address to bind to.

#### Using an IDE
The naming server can be started by running the `be.ac.ua.dist.systemy.namingServer.NamingServer` class.

#### Using the binaries
The naming server can be started by running the following command in the directory the jar is located in:

`java -jar NamingServer.jar`

## Node
Every node can add and remove files to the network. At all times, files are located on at least two nodes (except if there is only one node in the network).
### Starting the node
When starting the node, it will ask for a hostname and the IP address to bind to.

#### Using an IDE
##### Console mode
The console node can be started by running the `be.ac.ua.dist.systemy.node.Node` class.
##### GUI mode
The GUI node can be started by running the `be.ac.ua.dist.systemy.node.NodeMain` class.

#### Using the binaries
The binary can only run the GUI mode.

The node can be started by double clicking the jar or by running the following command in the directory the jar is located in:

`java -jar Node.jar`
