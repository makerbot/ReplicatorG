/**
 * 
 */
package replicatorg.machine.model;

/**
 * @author rob
 *
 */
public enum Endstops {
	NONE(false, false),
	MIN(true, false),
	MAX(false, true),
	BOTH(true, true);
	
	public final boolean hasMin, hasMax;
	
	private Endstops(boolean hasMin, boolean hasMax)
	{
		this.hasMin = hasMin;
		this.hasMax = hasMax;
	}
}
