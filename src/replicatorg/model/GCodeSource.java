package replicatorg.model;

import java.util.Iterator;
import java.util.List;

public interface GCodeSource extends Iterable<String> {
	
	/**
	 * Returns an iterator starting at the beginning of this source's gcode.
	 * @return a string iterator over the gcode
	 */
	Iterator<String> iterator();
	
	List<String> asList();
	
	int getLineCount();
}
