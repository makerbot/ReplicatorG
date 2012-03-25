package replicatorg.machine.model;

/**
 * This class represents a lookup system for toolhead info. IT can be used to 
 * convert variouis ID's from one to another, based on 
 * 
 * Should this be moved into ToolModel?
 *   -Ted 
 */
public enum ToolheadAlias
{
	RIGHT(0, "Right","A", "T0", "P1","G54"),
	LEFT(1, "Left", "B","T1", "P2", "G55"),
	SINGLE(0, "Center","A", "T0", "P1","G53");
	
	public final int number;	/// Toolhead ID number
	public final String tcodeName;	///Tcode for a toolhead (gcode commands)
	public final String pcodeName;	// Pcode for a toolhead (gcode commands)
	public final String recallOffsetCmd; // recall command (gcode command)
	
	// These can be overridden in some cases. Beware!
	public String guiName;		/// Toolhead GUI name
	public String axis;			/// If we use this to override an aixs, this indicates which one
	
	/// standard constructor. 
	private ToolheadAlias(int n, String guiName, String axis, String tcodeName, String pcodeName, 
			String recallOffsetCmd){
		this.number = n;
		this.guiName = guiName;
		this.axis = axis;
		this.tcodeName = tcodeName;
		this.pcodeName = pcodeName;
		this.recallOffsetCmd = recallOffsetCmd;
	}

	/** 
	 * Set this if we are overriding an axis.
	 * @param axis
	 */
	public void setAxis(String axis) {
		this.axis = axis;
	}
	public void setGuiName(String name) {
		this.guiName = name;
	}
	
	/// returns the Toolhead ID (T0 or T1) 
	public String getTcode(){
		return this.tcodeName;
	}
	
	/// returns the Toolhead name (Right or Left) 
	public String getName()
	{
		return this.guiName;
	}
	
	
	public String getPcode() {
		return this.pcodeName;
		/// returns default ID for setting the toolhead offset,
		/// which defines center of head to nozzle distance
		/// Z.b. : "G10 P1 X16.55 Y0 Z-0.3"
	}
	
	
	/// returns default gcode command for 'restoring/activating' the toolhead offset 
	/// (which must be specified earlier in the file with a P1/P2 command
	public String getRecallOffsetGcodeCommand()
	{
		// NOTE: G54 to G59 are valid offsets, set by P1 to P9,
		// G53 is 'Master' offset, which is 0,0,0 be definition
		return this.recallOffsetCmd;
	}
	
}
