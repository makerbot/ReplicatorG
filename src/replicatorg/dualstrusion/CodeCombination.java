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

	/*
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
		
		
		ArrayList<String> tween = DualStrusionWorker.readFiletoArrayList(new File("DualStrusion_Snippets/rctween.gcode"));
		
		// Grab the code from each file
		for(File f : files)
		{
			ArrayList<String> lines = DualStrusionWorker.readFiletoArrayList(f);
			// removes whitespace, empty layers
			DualStrusionWorker.preprocessForCombine(lines);
			
			// leave the first file's start untouched, remove the others'
			// also, don't add stuff unless it's after the first
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
					lines.subList(start, end+1).clear();
				
				// now we can add our in-between stuff at the top of the file, below the temperature change
				output.addAll(tween);
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
					lines.subList(start, end+1).clear();
			}
			
			output.addAll(lines);
		}
		
		DualStrusionWorker.mayHaveWipeCrash(output);
		
		DualStrusionWorker.writeArrayListtoFile(output, dest);

		return dest;
	}

}
