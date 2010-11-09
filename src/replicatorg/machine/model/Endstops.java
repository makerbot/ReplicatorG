/**
 * 
 */
package replicatorg.machine.model;

/**
 * @author rob
 *
 */
public enum Endstops {
	none(false, false),
	min(true, false),
	max(false, true),
	both(true, true);
	
	public final boolean hasMin, hasMax;
	
	private Endstops(boolean hasMin, boolean hasMax)
	{
		this.hasMin = hasMin;
		this.hasMax = hasMax;
	}
}
