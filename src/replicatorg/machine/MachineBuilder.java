package replicatorg.machine;

/**
 * Helper objects that makes a build onto a target, such as simulating to screen, building to a machine, or building to file.
 * @author mattmets
 *
 */
public interface MachineBuilder {
	// True if the job has finished.
	public boolean finished();

	// Run the next 
	public void runNext();
}
