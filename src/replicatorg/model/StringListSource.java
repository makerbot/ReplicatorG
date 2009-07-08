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
	
	public Iterator<String> iterator() {
		return gcode.iterator();
	}
	
}
