package replicatorg.model;

import java.util.Iterator;

public interface GCodeSource extends Iterable<String> {
	
	/**
	 * Returns an iterator starting at the beginning of this source's gcode.
	 * @return a string iterator over the gcode
	 */
	Iterator<String> iterator();
	
	int getLineCount();
}
