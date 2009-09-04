package replicatorg.plugin;

import java.util.Iterator;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import replicatorg.model.GCodeSource;

public class PluginEngine implements GCodeSource {
	GCodeSource parent = null;
	
	void setParentSource(GCodeSource parent) {
		this.parent = parent;
	}
	
	Vector<PluginEntry> plugins;
	void setPlugins(Vector<PluginEntry> plugins) {
		this.plugins = plugins;
	}
	
	public int getLineCount() {
		return parent.getLineCount();
	}

	Pattern mPattern = Pattern.compile("(^M([0-9]+))");

	private void processCodes(String s) {
		s = s.trim();
		Matcher match = mPattern.matcher(s);
		if (match.matches()) {
			int code = Integer.parseInt(match.group(1));
			for (PluginEntry plugin : plugins) {
				if (plugin instanceof MCodePlugin) {
					MCodePlugin mcp = (MCodePlugin)plugin;
					int codes[] = mcp.getAcceptedMCodes();
					for (int acceptedCode : codes) {
						if (code == acceptedCode) {
							mcp.processMCode(s);
						}
					}
				}
			}
		}
	}
	
	class GCodeIterator implements Iterator<String> {
		private Iterator<String> parent;
		public GCodeIterator(Iterator<String> parent) {
			this.parent = parent;
		}
		public boolean hasNext() {
			return parent.hasNext();
		}

		public String next() {
			String next = parent.next();
			processCodes(next);
			return next;
		}

		public void remove() {
			parent.remove();
		}
	}
	
	public Iterator<String> iterator() {
		return new GCodeIterator(parent.iterator());
	}

}
