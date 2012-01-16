package replicatorg.app;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * After changing this, make sure to run it to re-generate the documentation file.
 * @author Ted
 *
 *
 * replicat.org claims that we support:
* M120, M121, M122 Snnn set the PID gain for the temperature regulator
* M123, M124 Snnn set iMax and iMin windup guard for the PID controller
* M129 get range
* M130 set range
* M201 get buffer size
* M202 clear buffer
* M203 abort
* M204 pause
 */
public enum GCodeEnumeration {
	M0("M", 0, "Unconditional Halt, not supported on SD?"),
	M1("M", 1, "Optional Halt, not supported on SD?"),
	M2("M", 2, "End program"),
	M3("M", 3, "Spindle On - Clockwise"),
	M4("M", 4, "Spindle On - Counter Clockwise"),
	M5("M", 5, "Spindle Off"),
	M6("M", 6, "Wait for toolhead to come up to reach (or exceed) temperature"),
	M7("M", 7, "Coolant A on (flood coolant)"),
	M8("M", 8, "Coolant B on (mist coolant)"),
	M9("M", 9, "All Coolant Off"),
	M10("M", 10, "Close Clamp"),
	M11("M", 11, "Open Clamp"),
	M13("M", 13, "Spindle CW and Coolant A On"),
	M14("M", 14, "Spindle CCW and Coolant A On"),
	M17("M", 17, "Enable Motor(s)"),
	M18("M", 18, "Disable Motor(s)"),
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
	M104("M", 104, "Set Temperature"),
	M105("M", 105, "Get Temperature"),
	M106("M", 106, "Turn Automated Build Platform (or the Fan, on older models) On"),
	M107("M", 107, "Turn Automated Build Platform (or the Fan, on older models) Off"),
	M108("M", 108, "Set Extruder's Max Speed (R = RPM, P = PWM)"),
	M109("M", 109, "Set Build Platform Temperature"),
	M110("M", 110, "Set Build Chamber Temperature"),
	M126("M", 126, "Valve Open"),
	M127("M", 127, "Valve Close"),
	M128("M", 128, "Get Position"),
	M131("M", 131, "Store Current Position to EEPROM"),
	M132("M", 132, "Load Current Position from EEPROM"),
	M140("M", 140, "Set Build Platform Temperature"),
	M141("M", 141, "Set Chamber Temperature (Ignored)"),
	M142("M", 142, "Set Chamber Holding Pressure (Ignored)"),
	M200("M", 200, "Reset driver"),
	M300("M", 300, "Set Servo 1 Position"),
	M301("M", 301, "Set Servo 2 Position"),
	M310("M", 310, "Start data capture"),
	M311("M", 311, "Stop data capture"),
	M312("M", 312, "Log a note to the data capture store"),
	G0("G", 0, "Rapid Positioning"),
	G1("G", 1, "Coordinated Motion"),
	G2("G", 2, "Clockwise Arc"),
	G3("G", 3, "Counter Clockwise Arc"),
	G4("G", 4, "Dwell"),
	G10("G", 10, "Create Coordinate System Offset from the Absolute one"),
	G20("G", 20, "Use Inches as Units"),
	G70("G", 70, "Use Inches as Units"),
	G21("G", 21, "Use Milimeters as Units"),
	G71("G", 71, "Use Milimeters as Units"),
	G28("G", 28, "Home given axes to maximum"),
	G53("G", 53, "Set absolute coordinate system"),
	G54("G", 54, "Use coordinate system from G10 P0"),
	G55("G", 55, "Use coordinate system from G10 P1"),
	G56("G", 56, "Use coordinate system from G10 P2"),
	G57("G", 57, "Use coordinate system from G10 P3"),
	G58("G", 58, "Use coordinate system from G10 P4"),
	G59("G", 59, "Use coordinate system from G10 P5"),
	G90("G", 90, "Absolute Positioning"),
	G91("G", 91, "Relative Positioning"),
	G92("G", 92, "Define current position on axes"),
	G97("G", 97, "Spindle speed rate"),
	G161("G", 161, "Home given axes to minimum"),
	G162("G", 162, "Home given axes to maximum");
	
	private static final Map<String, GCodeEnumeration> lookup = new TreeMap<String, GCodeEnumeration>(
			//providing this comparator makes sure that the ordering of the codes is what we'd expect it to be
			new Comparator<String>(){
				@Override
				public int compare(String s, String t) {
					Character l1 = s.charAt(0);
					Character l2 = t.charAt(0);
					Integer i1 = Integer.parseInt(s.substring(1));
					Integer i2 = Integer.parseInt(t.substring(1));
					
					int cmp = l1.compareTo(l2);
					
					return cmp == 0 ? i1.compareTo(i2) : cmp;
				}
				
			});
	
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
	
	public static Set<String> supportedCodes()
	{
		return lookup.keySet();
	}
	
	public static Collection<String> getDocumentation()
	{
		ArrayList<String> result = new ArrayList<String>();
		for(GCodeEnumeration e : lookup.values())
			result.add(e.letter + e.number + ": " + e.documentation);
		return result;
	}

	public static GCodeEnumeration getGCode(String name)
	{
		return lookup.get(name);
	}
	
	public static GCodeEnumeration getGCode(String letter, Integer number)
	{
		return lookup.get(letter + number);
	}
}
