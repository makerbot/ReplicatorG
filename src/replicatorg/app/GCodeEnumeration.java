package replicatorg.app;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public enum GCodeEnumeration {
	M0("M", 0, "Unconditional Halt"),
	M1("M", 1, "Optional Halt"),
	M2("M", 2, "Program End"),
	M3("M", 3, "Spindle On - Clockwise"),
	M4("M", 4, "Spindle On - Counter Clockwise"),
	M5("M", 5, "Spindle Off"),
	M6("M", 6, "Tool Change"),
	M7("M", 7, "Coolant A on (flood coolant)"),
	M8("M", 8, "Coolant B on (mist coolant)"),
	M9("M", 9, "All Coolant Off"),
	M10("M", 10, "Close Clamp"),
	M11("M", 11, "Open Clamp"),
	M13("M", 13, "Spindle CW and Coolant A On"),
	M14("M", 14, "Spindle CCW and Coolant A On"),
	M17("M", 17, "Enable Drives"),
	M18("M", 18, "Disable Drives"),
	M21("M", 21, "Open Collet"),
	M22("M", 22, "Close Collet"),
	M30("M", 30, "Program Rewind"),
	M40("M", 40, "Change Gear Ratio to 0"),
	M41("M", 41, "Change Gear Ratio to 1"),
	M42("M", 42, "Change Gear Ratio to 2"),
	M43("M", 43, "Change Gear Ratio to 3"),
	M44("M", 44, "Change Gear Ratio to 4"),
	M45("M", 45, "Change Gear Ratio to 5"),
	M46("M", 46, "Change Gear Ratio to 6"),
	M50("M", 50, "Read Spindle Speed"),
	M101("M", 101, "Turn Extruder On, Forward"),
	M102("M", 102, "Turn Extruder On, Reverse"),
	M103("M", 103, "Turn Extruder Off"),
	M104("M", 104, "Custom Code for Temperature Control"),
	M105("M", 105, "Custom Code for Temperature Reading"),
	M106("M", 106, "Turn Fan On"),
	M107("M", 107, "Turn Fan Off"),
	M108("M", 108, "Set Extruder's Max Speed (RPM)"),
	M109("M", 109, "Set Build Platform Temperature"),
	M110("M", 110, "Set Build Chamber Temperature"),
	M126("M", 126, "Valve Open"),
	M127("M", 127, "Valve Close"),
	M128("M", 128, "Get Position"),
	M131("M", 131, "Store Current Position to EEPROM"),
	M132("M", 132, "Load Current Position from EEPROM"),
	M200("M", 200, "Initialize to Default State"),
	M300("M", 300, "Set Servo 1 Position"),
	M301("M", 301, "Set Servo 1 Position"),
	G0("G", 0, "Linear Interpolation"),
	G1("G", 1, "Rapid Positioning"),
	G2("G", 2, "Clockwise Arc"),
	G3("G", 3, "Counter Clockwise Arc"),
	G4("G", 4, "Dwell"),
	G10("G", 10, "Set Axis Offset"),
	G20("G", 20, "Use Inches as Units"),
	G70("G", 70, "Use Inches as Units"),
	G21("G", 21, "Use Milimeters as Units"),
	G71("G", 71, "Use Milimeters as Units"),
	G28("G", 28, "This should be \"return to home\".  We need to introduce new GCodes for homing."),
	G161("G", 161, "Home Negative"),
	G162("G", 162, "Home Positive"),
	G53("G", 53, "Master Offset"),
	G54("G", 54, "Fixture Offset 1"),
	G55("G", 55, "Fixture Offset 2"),
	G56("G", 56, "Fixture Offset 3"),
	G57("G", 57, "Fixture Offset 4"),
	G58("G", 58, "Fixture Offset 5"),
	G59("G", 59, "Fixture Offset 6"),
	G90("G", 90, "Absolute Positioning"),
	G91("G", 91, "Incremental Positioning"),
	G92("G", 92, "Set Position"),
	G97("G", 97, "Spindle speed rate");
	
	private static final Map<String, GCodeEnumeration> lookup = new HashMap<String, GCodeEnumeration>(100);
	static {
		for(GCodeEnumeration e : EnumSet.allOf(GCodeEnumeration.class))
			lookup.put(e.letter + e.number, e);
	}
	
	public final String documentation;
	public final String letter;
	public final Integer number;
	
	private GCodeEnumeration(String letter, Integer number, String documentation)
	{
		this.letter = letter;
		this.number = number;
		this.documentation = documentation;
	}
	
	static Set<String> supportedCodes()
	{
		return lookup.keySet();
	}
	
	static Collection<String> getDocumentation()
	{
		ArrayList<String> result = new ArrayList<String>();
		for(GCodeEnumeration e : lookup.values())
			result.add(e.letter + e.number + " " + e.documentation);
		return result;
	}

	static GCodeEnumeration getGCode(String name)
	{
		return lookup.get(name);
	}
	
	static GCodeEnumeration getGCode(String letter, Integer number)
	{
		return lookup.get(letter + number);
	}

	public static void main(String[] args)
	{
		File docFile = new File("docs/GCodeDocumentation.txt");
		
		try
		{
			// Clear the file. I feel like there's a better way to do this
			if(docFile.exists())
				docFile.delete();
			docFile.createNewFile();
			
			BufferedWriter docs = new BufferedWriter(new FileWriter(docFile));
			
			for(String d : getDocumentation())
				docs.write(d);
		}
		catch (IOException ioe)
		{
			System.out.println("Could not write to file!");
		}
		
	}
}
