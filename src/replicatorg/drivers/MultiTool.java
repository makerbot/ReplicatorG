/**
 * 
 */
package replicatorg.drivers;

/**
 * This class adds methods that machines which support multiple toolheads will find useful.
 * @author phooky
 *
 */
public interface MultiTool {

	/**
	 * Returns true if the driver is capable of explicitly setting the hardware index of an 
	 * attached tool.
	 * @see setConnectedToolIndex(int)
	 * @return true if tools can have their indexes set.
	 */
	public boolean toolsCanBeReindexed();
	
	/**
	 * If multiple identical hardware toolheads are available, it may be necessary to program a
	 * tool with its identifier so that multiple toolheads do not share a slot.  This is usually
	 * accomplished by disconnecting all other toolheads and explicitly setting the index of the 
	 * remaining tool.
	 * 
	 * If toolsCanBeReindexed() does not return true, this purpose of this function is undefined.
	 * 
	 * @param index The index to set the currently connected tool to.
	 * @return whether the tool's index was successfully set.
	 */
	public boolean setConnectedToolIndex(int index);
	
	/**
	 * If a machine supports multiple simultaneous toolheads, this function should return true.
	 * Such machines can have commands dispatched to a given toolhead simply by appending a "T"
	 * code to any M command.  It is expected that the last T code will be interpreted as the default
	 * tool from that point onwards.
	 */
	public boolean supportsSimultaneousTools();
}
