package replicatorg.plugin.toolpath;

import javax.swing.JComponent;

import replicatorg.model.BuildCode;
import replicatorg.model.BuildModel;

/**
 * This is the abstract base class which describes a toolpath plugin.
 * @author phooky
 *
 */
public abstract class ToolpathGenerator {
	public interface GeneratorListener {
		public void updateGenerator(String message);
	}
	
	protected BuildModel model;
	protected GeneratorListener listener;
	
	public void setListener(GeneratorListener listener) {
		this.listener = listener;
	}
	
	public void setModel(BuildModel model) {
		this.model = model;
	}
	
	public void visualConfigure(JComponent parent) {
		assert parent != null;
		assert model != null;
	}
	
	public abstract BuildCode generateToolpath();
}
