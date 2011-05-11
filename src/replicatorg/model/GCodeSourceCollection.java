package replicatorg.model;

import java.util.Iterator;
import java.util.Vector;


/**
 * A collection of GCode sources that act as a single source
 * @author mattmets
 *
 */
public class GCodeSourceCollection implements GCodeSource {

	final Vector<GCodeSource> sources; 
	final int lineCount;
	
	public class GCodeSourceCollectionIterator implements Iterator<String> {
		Vector<Iterator<String>> iterators;
		
		public GCodeSourceCollectionIterator(Vector<GCodeSource> sources) {
			iterators = new Vector<Iterator<String>>();
			
			for (GCodeSource source : sources) {
				iterators.add(source.iterator());
			}
		}
		
		@Override
		public boolean hasNext() {
			return (!iterators.isEmpty());
		}

		@Override
		public String next() {
			String next = null;
			
			if (hasNext()) {
				next = iterators.firstElement().next();
				
				if (!iterators.firstElement().hasNext()) {
					iterators.remove(0);
				}
			}
			return next;
		}

		@Override
		public void remove() {
			
		}
	}
	
	public GCodeSourceCollection(Vector<GCodeSource> sources) {
		this.sources = sources;
		
		// Count the total number of lines
		int lineCount = 0;
		for(GCodeSource source: this.sources) {
			lineCount += source.getLineCount();
		}
		this.lineCount = lineCount;
	}
	
	@Override
	public Iterator<String> iterator() {
		return new GCodeSourceCollectionIterator(sources);
	}

	@Override
	public int getLineCount() {
		return lineCount;
	}

}
