/**
 * 
 */
package replicatorg.model;

import java.util.Iterator;
import java.util.Vector;

/**
 * @author phooky
 *
 */
public class StringListSource implements GCodeSource {
	
	private Vector<String> gcode;
	
	public StringListSource(Vector<String> codes) {
		gcode = codes;
	}
	
	public Iterator<String> iterator() {
		return gcode.iterator();
	}

	public int getLineCount() { return gcode.size(); }
}
