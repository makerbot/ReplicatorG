package fabman.manager;

/**
 * The FabConnection object represents an open connection to a fabricator
 * queue.  Fabricator queues run in their own threads.  Communication is
 * established over local or IP sockets; this should allow some degree of
 * transparency in connecting to a remote machine.
 * 
 * FabConnections manage all message marshalling and sending.
 * 
 * TODO: FabConnections implement an interface which can be used to interface
 * to a local in-process instance of a Fab.  (For example, for simulation,
 * or for file output.) 
 * 
 * @author phooky
 *
 */
public class FabConnection {

}
