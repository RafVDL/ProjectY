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

### Commands
The console can be used to print information of the naming server.

`debug`

Enables debug of network communication (TCP/UDP)

`undebug`

Disables debug of network communication (TCP/UDP)

`clear`

Clears the ip table

`shutdown`

Stops the naming server

`table`

Prints the ip table

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

### Commands
The console can be used to interact with the node. This is only possible if the node is running from an IDE or using the command line when running with the binaries.

`debug`

Enables debug of network communication (TCP/UDP)

`undebug`

Disables debug of network communication (TCP/UDP)

`shutdown+`

Performs a shutdown of the node. This includes moves files to other nodes if necessary.

`shutdown`

Performs an immediate shutdown without moving any files to other nodes.

`neighbours`

Prints the neighbours of the node

`localFiles`

Prints the local files list.

`replicatedFiles`

Prints the replicated files list.

`ownerFiles`

Prints the owner files list.

`allfiles`

Prints a list of all files in the network.

`dl`

Downloads a file of the network. The node will ask for the file name to download.

`delnetw`

Deletes a file in the network. The node will ask for the file name to delete.
