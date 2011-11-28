package replicatorg.plugin.toolpath;

import java.awt.Frame;
import java.util.LinkedList;

import replicatorg.model.BuildCode;
import replicatorg.model.BuildModel;

/**
 * This is the abstract base class which describes a toolpath plugin.
 * @author phooky
 *
 */
public abstract class ToolpathGenerator {
	public interface GeneratorListener {
		public enum Completion {
			SUCCESS,
			FAILURE
		};
		public void updateGenerator(String message);
		public void generationComplete(Completion completion, Object details);
	}
	
	protected BuildModel model;
	protected LinkedList<GeneratorListener> listeners = new LinkedList<GeneratorListener>();
	
	public void addListener(GeneratorListener listener) {
		listeners.add(listener);
	}
	
	public void setModel(BuildModel model) {
		this.model = model;
	}
	
	/**
	 * Returns true if configuration successful; false if aborted.
	 */
	public boolean visualConfigure(Frame parent) {
		//TODO: Why does this exist? Test/refactor
		assert parent != null;
		assert model != null;
		return true;
	}
	
	/**
	 * Returns true if configuration successful; false if aborted.
	 */	
	public boolean visualConfigure(Frame parent, int x, int y, String name) {
		//TODO: Why does this exist? Test/refactor
		assert parent != null;
		assert model != null;
		return true;
	}
	
	/**
	 * asserts model exists, Returns true.
	 */
	public boolean nonvisualConfigure()
	{
		//TODO: Why does this exist? Test/refactor
		assert model != null;
		return true;
	}
	
	public void editProfiles(Frame parent) {
		assert parent != null;
	}
	
	public abstract BuildCode generateToolpath();
	
	public void emitUpdate(String message) {
		for (GeneratorListener listener : listeners) {
			listener.updateGenerator(message);
		}
	}
	
	public void emitCompletion(GeneratorListener.Completion completion, Object details) {
		for (GeneratorListener listener : listeners) {
			listener.generationComplete(completion, details);
		}
	}


}
