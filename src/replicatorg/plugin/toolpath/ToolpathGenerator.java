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
	private BuildModel model;
	
	public void setModel(BuildModel model) {
		this.model = model;
	}
	
	public void configure(JComponent component) {
		assert model != null;
	}
	
	public abstract BuildCode generateToolpath();
}
