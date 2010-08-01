package replicatorg.model;

import java.io.OutputStream;
import java.util.LinkedList;
import java.util.List;

import javax.swing.undo.UndoManager;

public abstract class BuildElement {
	protected UndoManager undo = new UndoManager();
	
	/**
	 * Every editable element has its own undo/redo manager.
	 */
	public UndoManager getUndoManager() { return undo; }
	
	/** 
	 * Classes which implement BuildElement.Listener can listen to
	 * a build element and recieve updates when it changes.
	 */
	public interface Listener {
		/**
		 * When a build element is modified, it will call buildElementUpdate
		 * on any listeners it may have.
		 * @param element The build element which has been updated.
		 */
		public void buildElementUpdate(BuildElement element);
	}
	
	private List<Listener> listeners = new LinkedList<Listener>();
	
	/**
	 * Add a listener to this build element.
	 * @param l The object that wishes to recieve updates.
	 */
	public void addListener(Listener l) {
		listeners.add(l);
	}
	
	/**
	 * Tell all our listeners that we've been updated.
	 */
	protected void emitUpdate() {
		for (Listener l : listeners) {
			l.buildElementUpdate(this);
		}
	}
	
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
	 * should mark the underlying element as modified.  If the state changes,
	 * an update event will be emitted.
	 * @param modified Whether the element is now modified or not.
	 */
	public void setModified(boolean modified) {
		if (this.modified == modified) return;
		this.modified = modified;
		emitUpdate();
	}
	
	/**
	 * Write this build element to the given output stream.  Ordinarily this is
	 * called by Build during a save.
	 */
	abstract void writeToStream(OutputStream ostream);
}
