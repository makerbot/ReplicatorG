package replicatorg.machine;

/**
 * Object responsible for managing a build on a machine.
 * @author mattmets
 *
 */
public interface MachineBuilder {
	// True if the build has finished.
	public boolean finished();

	// Run the next command on the machine, if possible.
	public void runNext();
}
