package fabman.manager;

/**
 * FabManager is a top-level fab connection manager.  FabManager is responsible for discovering
 * connected fabrication queues, enumerating connectable queues, and starting servers for queues
 * that are connectable but not yet running.
 * 
 * FabManager should ordinarily be obtained by using the static factory call getFabManager().
 * Other factory calls exist for testing purposes only.
 * 
 * Concurrency: all calls need to assert a global mutex to avoid multiple instances attempting to
 * create connections against the same device simultaneously.
 * 
 * @author phooky
 *
 */
public class FabManager {

	// Factory calls
}
