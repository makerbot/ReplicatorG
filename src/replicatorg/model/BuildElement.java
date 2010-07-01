package replicatorg.model;

import java.io.OutputStream;

public abstract class BuildElement {
	protected Build parent = null;
		
	public enum Type {
		MODEL("model"),
		GCODE("gcode");
		
		private String displayString;
		Type(String displayString) { this.displayString = displayString; }
		public String getDisplayString() { return displayString; }
	}

	public abstract Type getType();

	private boolean modified = false;
	
	/**
	 * Indicates if the element has been modified since the time it was
	 * loaded, or last saved.
	 * @return True if the element has been modified; false otherwise.
	 */
	public boolean isModified() {
		return modified;
	}

	/**
	 * Mark this element as modified.  All changes made in an editing window
	 * should mark the underlying element as modified.
	 * @param modified
	 */
	public void setModified(boolean modified) {
		this.modified = modified;
	}
	
	/**
	 * Write this build element to the given output stream.  Ordinarily this is
	 * called by Build during a save.
	 */
	abstract void writeToStream(OutputStream ostream);
}
