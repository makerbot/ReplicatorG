package replicatorg.dualstrusion;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
/**
 * 
 * This is a tool intended for Makerbot's R&D team to do materials testing, it may have uses or hazards 
 * that are documented only verbally. Use at your own risk.
 * 
 * @author thbrandston
 *
 */
public class CodeCombination{

	/* so, what's the plan right now?
	 * we've got to take some number of gcode files with different skeinforge settings and put them one after another
	 * but the temperature from one to the next may be different, and we need to make sure that the toolhead doesn't 
	 * hit something that's already been printed.
	 * 
	 * So, we'll keep the initial start code, but for each file after the first we'll take out the starting gcode
	 * leaving behind only the temperature and some code to bring the head all the way back and spit
	 * 
	 * 
	 * See, now I've got a single static method in a class, and the method relies heavily on another DualStrusionWorker. 
	 * Shouldn't this method just be in dsw?
	 */
	/**
	 * 
	 * @param dest
	 * @param files The list of gcode files to be combined. Must be ordered from front to back! otherwise there'll be a lot of collisions.
	 * @return
	 */
	public static File mergeGCodes(File dest, List<File> files)
	{
		ArrayList<String> output = new ArrayList<String>();
		
		// Grab the code from each file
		for(File f : files)
		{
			ArrayList<String> lines = DualStrusionWorker.readFiletoArrayList(f);
			// removes whitespace, empty layers
			DualStrusionWorker.prepGcode(lines);
			
			// leave the first file's start untouched, remove the others'
			if(f != files.get(0))
			{
				// This is a modified version of DualStrusionWorker's stripStartGcode()
				int start = -1, end = -1;
				for(int i = 0; i < lines.size(); i++)
				{
					String l = lines.get(i);
					
					// grab the start of the start code
					if(l.equalsIgnoreCase("(**** beginning of start.gcode ****)"))
						start = i;
					// While we're in the start code
					if(start != -1)
					{
						// if this is the temperature setting, copy it into our collected file
						if(l.matches("M104.*"))
							output.add(l.substring(0));
						
					}
					// break out at the end of the start code
					if(l.equalsIgnoreCase("(**** end of start.gcode ****)"))
					{
						end = i;
						break;
					}
				}
				
				// see if we found anything and remove it. 
				// This check is not, I think, strictly necessary. A sublist from -1 of length 0 shouldn't be anything.
				if(start != -1)
					lines.subList(start, end).clear();
			}
			
			//leave the last file's end untouched
			if(f != files.get(files.size()-1))
			{
				int start = -1, end = -1;
				for(int i = 0; i < lines.size(); i++)
				{
					String l = lines.get(i);
					
					// grab the start of the end code
					if(l.equalsIgnoreCase("(**** beginning of end.gcode ****)"))
						start = i;
					// break out at the end of the end code
					if(l.equalsIgnoreCase("(**** end of end.gcode ****)"))
					{
						end = i;
						break;
					}
				}
				// see if we found anything and remove it. 
				// Again, I don't know that this needs to be conditional
				if(start != -1)
					lines.subList(start, end).clear();
			}

			// now we can add our in-between stuff at the top of the file
			output.add("G162 Z F100 (home Z axis maximum)");
			output.add("G161 X Y F2500 (home XY axes minimum)");
			output.add("M132 X Y Z A B (Recall stored home offsets for XYZAB axis)");
			output.add("M6 T0 (wait for toolhead parts, nozzle, HBP, etc., to reach temperature)");
			output.add("M101 T0 (Extruder on, forward)");
			output.add("G04 P3000 (Wait 3 seconds)");
			output.add("M103 T0 (Extruder off)");
			
			output.addAll(lines);
		}
		
		DualStrusionWorker.checkCrashes(output);
		
		DualStrusionWorker.writeArrayListtoFile(output, dest);

		return dest;
	}

}
