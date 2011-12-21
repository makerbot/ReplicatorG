package replicatorg.app.gcode;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;
import java.util.Vector;

import replicatorg.machine.model.ToolheadAlias;
import replicatorg.model.GCodeSource;
import replicatorg.model.StringListSource;

/**
 * What a useless class name. 
 * This class is where we're going to stash the various functions for manipulating GCode source
 * I suspect that anything that ends up in here really belongs somewhere else
 * @author Ted
 *
 */
public class GCodeHelper {

	/**
	 * Takes gcode, and changes all toolhead references(T0 -> T1 or similar)
	 * based on user input.
	 * 
	 * G54 also must be used for T0, and G55 for T1 
	 * 
	 * This is not used by DualstrusionWorker, 
	 * it is only meant for single headed files
	 * 
	 * oldGCode should not change
	 * 
	 * @param source
	 *            gcode to alter the toolhead info on
	 * @param tool
	 *            string name of the toolhead, 'right' or 'left'
	 * @return A new GCodeSource with the new code
	 */
	public static GCodeSource changeToolHead(GCodeSource oldGCode, ToolheadAlias tool) {

		Vector<String> newGCode = new Vector<String>();
		
		// T only needs to be called once in a file, so we find the first,
		// make sure it's correct, and then remove every T after it
		// (unless it's a comment, we can skip comments)
		boolean foundT = false;
		// same goes for G54&55
		boolean foundG = false;
		
		for(String line : oldGCode)
		{
			// clone
			String newLine = new String(line);
			GCodeCommand g = new GCodeCommand(newLine);

			// replace the first toolhead call with the correct one
			// removes every following toolhead call
			if(!foundT)
			{
//				if(g.removeCode('T') != null)
				if(g.hasCode('T'))
				{
					foundT = true;
//					g.addCode('T', tool.number);

					// replace T* with the correct T code
					newLine = newLine.replace("T0", tool.getTcode());
					newLine = newLine.replace("T1", tool.getTcode());
				}
			}
			else
			{
//				g.removeCode('T');
				newLine = newLine.replace("T0", "");
				newLine = newLine.replace("T1", "");
			}
			
			// replaces the first G54/55 with the correct one
			// removes every following G54/55
			if(!foundG)
			{
				if(g.getCodeValue('G') == 55)
				{
					foundG = true;
					if(tool == ToolheadAlias.LEFT)
					{
//						g.removeCode('G');
//						g.addCode('G', 54);
						newLine = newLine.replace("G55", "G54");
					}
				}
				else if(g.getCodeValue('G') == 54)
				{
					foundG = true;
					if(tool == ToolheadAlias.RIGHT)
					{
						newLine = newLine.replace("G54", "G55");
					}
				}
			}
			else
			{
				if (g.getCodeValue('G') == 55 || g.getCodeValue('G') == 54)
				{
//					g.removeCode('G');
					newLine = newLine.replace("G54", "");
					newLine = newLine.replace("G55", "");
				}
			}
			
			newGCode.add(newLine);
		}

		return new StringListSource(newGCode);
	}
	// TODO: remove this -- "reverse compatibility"
	public static void changeToolHead(File source, ToolheadAlias tool) {
		writeGCodeSourcetoFile(changeToolHead(readFiletoGCodeSource(source), tool), source);
	}

	/**
	 * Weird things about this function:
	 * It actually just looks for the first line of the 'oldSection' and rips out an amount equal to the
	 * length of that section. We do this because it's possible that something else has modified the code
	 * in there somewhat.
	 * Another way of doing that might be to check the first and last lines, but I'm not sure they'll stay the same
	 *   in which case why am I even relying on the first line? 
	 * @param source
	 * @param oldSection
	 * @param newSection
	 * @return
	 */
	public static GCodeSource replaceStartOrEndGCode(GCodeSource source, GCodeSource oldSection, GCodeSource newSection)
	{
		Vector<String> newGCode = new Vector<String>();

		Iterator<String> oldIt = oldSection.iterator();
		String firstOldLine = oldIt.next();
		for(Iterator<String> sourceIt = source.iterator(); sourceIt.hasNext();)
		{
			String line = sourceIt.next();
			if(line.equals(firstOldLine))
			{
				// DANGER: we just assume that start is the same length as it was,
				// but we don't check for exact matches, because some other modification may have been done
				for(; oldIt.hasNext();)
				{
					sourceIt.next();
					oldIt.next();
				}
				//now that we've removed the old code
				for(String newLine : newSection)
					newGCode.add(newLine);
			}
			else
			{
				newGCode.add(line);
			}
		}

		return new StringListSource(newGCode);
	}
	
	public static GCodeSource addCodeToSource(GCodeSource source, GCodeSource newSection, long location)
	{
		Vector<String> newGCode = new Vector<String>();

//		newGCode.addAll(source.)

		return new StringListSource(newGCode);
	}
	
	/**
	 * This method is used to write finished combinedGcode to a file
	 * 
	 * @param t
	 *            writeThis arrayList
	 * @param f
	 *            to this Destination
	 */
	public static void writeGCodeSourcetoFile(GCodeSource source, File f) {
		try {
			FileWriter bwr = new FileWriter(f);

			for (String s : source) {
				bwr.write(s + "\n");
			}
			bwr.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Reads a file f into an ArrayList to use for combination later
	 * 
	 * @param f
	 * @return an ArrayList<String> with one line per array item
	 */
	public static GCodeSource readFiletoGCodeSource(File f) {
		Vector<String> vect = new Vector<String>();
		String curline;
		try {
			BufferedReader bir = new BufferedReader(new FileReader(f));
			curline = bir.readLine();
			while (curline != null) {
				vect.add(curline);
				curline = bir.readLine();
			}
			bir.close();
		} catch (IOException e) {
			System.err.println("couldnt read file " + f.getAbsolutePath());
			return null;
		}
		return new StringListSource(vect);
	}
}
