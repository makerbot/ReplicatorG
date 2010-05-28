package replicatorg.plugin.toolpath;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import net.miginfocom.swing.MigLayout;
import replicatorg.app.Base;
import replicatorg.model.Build;
import replicatorg.model.BuildCode;

public class ToolpathGeneratorThread extends Thread {
	private JComponent parent;
	private ToolpathGenerator generator;
	private Build build;
	
	private class ProgressDialog extends JDialog implements ToolpathGenerator.GeneratorListener {
		JLabel topLabel;
		JLabel progressLabel;
		
		public ProgressDialog(JComponent parent, Build build) { 
			super(SwingUtilities.getWindowAncestor(parent));
			Icon icon = new ImageIcon(Base.getImage("images/slicing-icon.png",this));
			setTitle("Generating toolpath for "+build.getName());
			topLabel = new JLabel("Generating toolpath for "+build.getName(),icon,SwingConstants.LEFT);
			progressLabel = new JLabel("Launching plugin...");
			setLayout(new MigLayout());
			add(topLabel,"wrap");
			add(progressLabel,"wrap");
		}

		boolean done = false;
		
		public boolean isDone() {
			return done;
		}
		
		public void setDone(boolean done) {
			this.done = done;
		}
		
		public void updateGenerator(final String message) {
			System.err.println(message);
			System.err.flush();
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					progressLabel.setText(message);
				}
			});
		}
	}
	
	public ToolpathGeneratorThread(JComponent parent, ToolpathGenerator generator, Build build) {
		this.parent = parent;
		this.generator = generator;
		this.build = build;
	}
	
	public void run() {
		generator.setModel(build.getModel());
		ProgressDialog progressDialog = null;
		if (parent != null) {
			// Configure, if possible
			progressDialog = new ProgressDialog(parent,build);
			generator.visualConfigure(parent);
			generator.setListener(progressDialog);
			// This actually works because it's a modal dialog;
			// a new nested event loop is generated in the event loop
			// that blocks other events.
			final ProgressDialog pdHandle = progressDialog;
			SwingUtilities.invokeLater(new Runnable() { public void run() {
				synchronized (pdHandle) {
					if (!pdHandle.isDone()) {
						System.err.println("showing tp gen dialog");
						pdHandle.pack();
						pdHandle.setVisible(true);
					}
				}
			}});
		}
		System.err.println("begin tp gen ");
		BuildCode code = generator.generateToolpath();
		if (progressDialog != null) {
			synchronized (progressDialog) { 
				progressDialog.setVisible(false);
				progressDialog.setDone(true);
			}
		}
		build.code = code;
	}
}
