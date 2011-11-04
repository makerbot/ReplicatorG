package replicatorg.dualstrusion;

/**
 * 
 * @author Noah Levy
 * This enum is a replacement for using an int or string to represent Tooheads
 * 
 */
public enum Toolheads
{
	SECONDARY(0),
	PRIMARY(1);
	
	public final int number;
	
	private Toolheads(int n)
	{
		number = n;
	}
}