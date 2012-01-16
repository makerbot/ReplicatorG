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
	
	/// standard constructor. 
	private Toolheads(int n){
		number = n;
	}
	
	/// returns the Toolhead ID (T0 or T1) 
	public String getTid(){
		if(this == SECONDARY)		return "T0";
		return "T1";
	}
	
	/// returns the Toolhead name (Right or Left) 
	public String getName()
	{
		if(this == SECONDARY)	return "Right";
		return "Left";
	}	
	
	
	/// returns default ID for setting the toolhead offset 
	///, which defines center of head to nozzle distance
	/// Z.b. : "G10 P1 X16.55 Y0 Z-0.3"
	public String getPcode() {
		if(this == SECONDARY)	return "P1";
		return "P2";
	}
	
	/// returns default gcode command for 'restoring/activating' the toolhead offset 
	/// (which must be specified earlier in the file with a P1/P2 command
	public String getRecallOffsetGcodeCommand()
	{
		if(this == SECONDARY)
			return "G55";
		else return "G54";
		// NOTE: G54 to G59 are valid offsets, set by P1 to P9,
		// G53 is 'Master' offset, which is 0,0,0 be definition
	}
	
}
