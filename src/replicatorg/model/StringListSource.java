/**
 * 
 */
package replicatorg.model;

import java.util.Iterator;
import java.util.List;

/**
 * @author phooky
 *
 */
public class StringListSource implements GCodeSource {
	
	private List<String> gcode;
	
	public StringListSource(List<String> codes) {
		gcode = codes;
	}
	
	public Iterator<String> iterator() {
		return gcode.iterator();
	}
	
	public List<String> asList() {
		return gcode;
	}

	public int getLineCount() { return gcode.size(); }
}
