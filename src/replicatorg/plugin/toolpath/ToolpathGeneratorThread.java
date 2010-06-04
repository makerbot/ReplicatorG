package replicatorg.plugin.toolpath;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.logging.Level;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import net.miginfocom.swing.MigLayout;
import replicatorg.app.Base;
import replicatorg.model.Build;
import replicatorg.model.BuildCode;
import replicatorg.plugin.toolpath.ToolpathGenerator.GeneratorListener;

public class ToolpathGeneratorThread extends Thread {
	private JComponent parent;
	private ToolpathGenerator generator;
	private Build build;

	private void abortGeneration() {
		Base.logger.severe("Aborted toolpath generation!");
		this.interrupt();
	}
	
	private class ProgressDialog extends JDialog implements ToolpathGenerator.GeneratorListener {
		JLabel topLabel;
		JLabel progressLabel;
		JButton doneButton;
		
		public ProgressDialog(JComponent parent, Build build) { 
			super(SwingUtilities.getWindowAncestor(parent));
			ImageIcon icon = new ImageIcon(Base.getDirectImage("images/slicing-icon.gif",this));
			setTitle("Generating toolpath for "+build.getName());
			topLabel = new JLabel("Generating toolpath for "+build.getName(),icon,SwingConstants.LEFT);
			icon.setImageObserver(topLabel);
			progressLabel = new JLabel("Launching plugin...");
			setLayout(new MigLayout());
			add(topLabel,"wrap");
			add(progressLabel,"wrap,wmin 400px");
			doneButton = new JButton("Cancel");
			doneButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					synchronized(this) {
						abortGeneration();
					}
				}
			});
			add(doneButton,"tag cancel");
			this.setModal(true);
		}

		boolean done = false;
		
		public boolean isDone() {
			return done;
		}
		
		public void setDone(boolean done) {
			this.done = done;
		}
		
		public void updateGenerator(final String message) {
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					progressLabel.setText(message);
				}
			});
		}

		public void generationComplete(Completion completion, Object details) {
		}
	}
	
	public ToolpathGeneratorThread(JComponent parent, ToolpathGenerator generator, Build build) {
		this.parent = parent;
		this.generator = generator;
		this.build = build;
	}
	
	public void addListener(ToolpathGenerator.GeneratorListener listener) {
		this.generator.addListener(listener);
	}
	
	public void run() {
		generator.setModel(build.getModel());
		ProgressDialog progressDialog = null;
		if (parent != null) {
			// Configure, if possible
			progressDialog = new ProgressDialog(parent,build);
			generator.visualConfigure(parent);
			generator.addListener(progressDialog);
			// This actually works because it's a modal dialog;
			// a new nested event loop is generated in the event loop
			// that blocks other events.
			final ProgressDialog pdHandle = progressDialog;
			SwingUtilities.invokeLater(new Runnable() { public void run() {
				if (!pdHandle.isDone()) {
					double x = parent.getBounds().getCenterX();
					double y = parent.getBounds().getCenterY();
					pdHandle.pack();
					x -= pdHandle.getWidth() / 2.0;
					y -= pdHandle.getHeight() / 2.0;
					pdHandle.setLocation((int)x,(int)y);
					pdHandle.setVisible(true);
				}
			}});
		}
		Base.logger.info("Beginning toolpath generation.");
		try {
			BuildCode code = generator.generateToolpath();
			if (code != null) {
				build.code = code;
				build.loadCode();
				generator.emitCompletion(GeneratorListener.Completion.SUCCESS, null);
				Base.logger.info("Toolpath generation complete!");
			} else {
				generator.emitCompletion(GeneratorListener.Completion.FAILURE, null);
				Base.logger.severe("Toolpath generation failed!");
			}
		} catch (Exception e) {
			Base.logger.log(Level.SEVERE,"Toolpath generation failed!",e);
			generator.emitCompletion(GeneratorListener.Completion.FAILURE, e);
		} finally {
			if (progressDialog != null) {
				synchronized (progressDialog) {
					progressDialog.setVisible(false);
					progressDialog.setDone(true);
				}
			}
		}
	}
}
