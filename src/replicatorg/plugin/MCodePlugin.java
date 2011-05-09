package replicatorg.plugin;

import replicatorg.app.GCode;

public interface MCodePlugin {
	/**
	 * Returns an array of integers indicating which M-codes this plugin is capable of processing.
	 * Every m-code with a value in this list will be sent to processMCode.
	 * @return the accepted m-code values
	 */
	public int[] getAcceptedMCodes();
	
	/**
	 * Handle one of the acceptable m-codes.  This function should expect to recieve the entire line
	 * on which the m-code was found.
	 * 
	 * In general, this function should return quickly, to avoid blocking replicatorG.
	 * @param mcode the full line of m-code
	 */
	public void processMCode(GCode mcode);
}
