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
 * 
 * So, most of these functions take a GCodeSource and return a different, modified, GCodeSource.
 *   would it make sense to pass them a MutableGCodeSource or something like that?
 * @author Ted
 *
 */
public class GCodeHelper {

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
	public static MutableGCodeSource replaceStartOrEndGCode(GCodeSource source, GCodeSource oldSection, GCodeSource newSection)
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

		return new MutableGCodeSource(newGCode);
	}

}
