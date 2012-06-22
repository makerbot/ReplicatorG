package replicatorg.plugin.toolpath;

import replicatorg.app.Base;
import java.awt.Frame;
import java.util.EventObject;
import java.util.LinkedList;

import replicatorg.model.BuildCode;
import replicatorg.model.BuildModel;
import replicatorg.plugin.toolpath.ToolpathGenerator.GeneratorListener.Completion;

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
		public void updateGenerator(GeneratorEvent evt);
		public void generationComplete(GeneratorEvent evt);
	}
	
	public class GeneratorEvent extends EventObject {
		private Completion completion;
		private String message;
		
		public GeneratorEvent(Object source) {
			this(source, "", null);
		}
		public GeneratorEvent(Object source, String message, Completion completion) {
			super(source);
			this.message = message;
			this.completion = completion;
		}
		
		public Completion getCompletion() {
			return completion;
		}
		
		public String getMessage() {
			return message;
		}
		
	}
	public static String displayName = "A Toolpath Generator";
	
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
	//should return a cached toolpath
	public abstract BuildCode getGeneratedToolpath();
	
	public void emitUpdate(String message) {
		for (GeneratorListener listener : listeners) {
			listener.updateGenerator(new GeneratorEvent(this, message, null));
		}
	}
	
	public void emitCompletion(GeneratorListener.Completion completion) {
		for (GeneratorListener listener : listeners) {
			Base.logger.finest("emitCompletion! sent to " + listener.toString());
			listener.generationComplete(new GeneratorEvent(this, null, completion));
		}
	}


}
