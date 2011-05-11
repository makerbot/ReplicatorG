package replicatorg.machine.builder;

import replicatorg.machine.Machine.JobTarget;

/**
 * Object responsible for managing a build on a machine.
 * @author mattmets
 *
 */
public interface MachineBuilder {
	// True if the build has finished.
	public boolean finished();
	
	public int getLinesTotal();
	public int getLinesProcessed();

	// Run the next command on the machine, if possible.
	public void runNext();
	
	/**
	 * True if this builder is managing a build on a live machine (i.e., not a file or buffer)
	 * @return
	 */
	public boolean isInteractive();
	
	/**
	 * Get the type of target that this builder is building to.
	 * @return
	 */
	public JobTarget getTarget();
}
