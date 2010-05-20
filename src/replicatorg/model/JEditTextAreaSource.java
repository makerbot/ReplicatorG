/**
 * 
 */
package replicatorg.model;

import java.util.Iterator;

import replicatorg.app.syntax.JEditTextArea;

/**
 * @author phooky
 *
 */
public class JEditTextAreaSource implements GCodeSource {
	
	private JEditTextArea textarea;
	
	public JEditTextAreaSource(JEditTextArea textarea) {
		this.textarea = textarea;
	}
	
	public Iterator<String> iterator() {
		final JEditTextArea ta = this.textarea;
		return new Iterator<String>() {
			int idx = 0;
			public boolean hasNext() { return idx < ta.getLineCount(); }
			public String next() { String s = ta.getLineText(idx); idx = idx + 1; return s; }
			public void remove() { throw new UnsupportedOperationException(); }
		};
	}
	
	public int getLineCount() {
		return textarea.getLineCount();
	}

	
}
