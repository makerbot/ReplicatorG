package replicatorg.plugin.toolpath;

import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.logging.Level;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JProgressBar;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import net.miginfocom.swing.MigLayout;
import replicatorg.app.Base;
import replicatorg.model.Build;
import replicatorg.model.BuildCode;
import replicatorg.plugin.toolpath.ToolpathGenerator.GeneratorListener;

// For interpreting a GCode generator's output:
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.text.DecimalFormat;
import java.text.NumberFormat;

public class ToolpathGeneratorThread extends Thread {
	private Frame parent;
	private ToolpathGenerator generator;
	private Build build;

	private void abortGeneration() {
		Base.logger.severe("Aborted toolpath generation!");
		this.interrupt();
	}
	
	private class ProgressDialog extends JDialog implements ToolpathGenerator.GeneratorListener {
		JLabel topLabel;
		JLabel progressLabel;
		JProgressBar subProgressBar;
		JProgressBar totalProgressBar;
		JButton doneButton;
		int layerIndex;
		int layerTotal;
		
		public ProgressDialog(Frame parent, Build build) { 
			super(parent);
			ImageIcon icon = new ImageIcon(Base.getDirectImage("images/slicing-icon.gif",this));
			setTitle("Generating toolpath for "+build.getName());
			topLabel = new JLabel("Generating toolpath for "+build.getName(),icon,SwingConstants.LEFT);
			icon.setImageObserver(topLabel);
			progressLabel = new JLabel("Launching plugin...");
			subProgressBar = new JProgressBar();
			totalProgressBar = new JProgressBar();
			subProgressBar.setValue(0);
			subProgressBar.setStringPainted(true);
			subProgressBar.setValue(0);
			totalProgressBar.setStringPainted(false);
			int layerIndex = 0;
			int layerTotal= 9999;
			setLayout(new MigLayout());
			add(topLabel,"wrap");
			add(new JLabel("Generator: Skeinforge"),"wrap");
			add(progressLabel,"wrap,growx");
			add(subProgressBar,"wrap,wmin 400px");
			add(totalProgressBar,"wrap,wmin 400px");
			doneButton = new JButton("Cancel");
			doneButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					synchronized(this) {
						abortGeneration();
					}
				}
			});
			add(doneButton,"tag cancel");
			this.setModal(false);
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

				    String newMessage = message;
					boolean logIt = true;
				    boolean showProgress = false;
					int sub;
					Pattern r = Pattern.compile("total Layer count is[^0-9]([0-9]+)[^0-9]");
				    Matcher m = r.matcher(message);
				    if (m.find( )) {
			    		layerTotal = Integer.parseInt(m.group(1));
				    }
					r = Pattern.compile("^Filling layer.*[^0-9]([0-9]+)[^0-9]");
				    m = r.matcher(message);
				    if (m.find( ))
				    {
				    	layerIndex = Integer.parseInt(m.group(1));
				    	showProgress = true;
				    	logIt = false;
				    	sub = (int) (55*((double) layerIndex)/ layerTotal);
				    	totalProgressBar.setValue(10 + sub);				    	
				    }

				    // THE ONE BELOW IS FOR THE OLDER SKEINFORGE!
					r = Pattern.compile("Filling layer[^0-9]([0-9]+)/([0-9]+)[^0-9]");
				    m = r.matcher(message);
				    if (m.find( )) {
			    		layerIndex = Integer.parseInt(m.group(1));
			    		layerTotal = Integer.parseInt(m.group(2));
			    		showProgress = true;
			    		logIt = false;
				    	sub = (int) (55*((double) layerIndex)/ layerTotal);
				    	totalProgressBar.setValue(10 + sub);
				    }				    
				    				    
					r = Pattern.compile("Slice to GCode.*layer ([0-9]+)[^0-9]");//Slice to GCode... layer %s.
				    m = r.matcher(message);
				    if (m.find( ))
				    {
				    	layerIndex = Integer.parseInt(m.group(1));
				    	showProgress = true;
				    	logIt = false;
				    	sub = (int) (2*((double) layerIndex)/ layerTotal);
				    	totalProgressBar.setValue(2 + sub);				    	
				    }
				    
					r = Pattern.compile("Insetting.*layer ([0-9]+)[^0-9]");
				    m = r.matcher(message);
				    if (m.find( ))
				    {
				    	layerIndex = Integer.parseInt(m.group(1));
				    	showProgress = true;
				    	logIt = false;
				    	sub = (int) (6*((double) layerIndex)/ layerTotal);
				    	totalProgressBar.setValue(4 + sub);
				    }
				    
				    if(showProgress)
				    {
				    	String i = new Integer(layerIndex).toString();
						String j = new Integer(layerTotal).toString();
						double completion =  ((double) layerIndex/layerTotal);
						NumberFormat nf = NumberFormat.getPercentInstance();
						String perc = nf.format(completion);
					    if(layerIndex>0)
					    	newMessage += " ("+j+" layers)";//
					    subProgressBar.setValue((int) (100*completion));
					}
					r = Pattern.compile("Fill procedure took");
				    m = r.matcher(message);
				    if (m.find( ))
				    {
				    	subProgressBar.setValue(0);
				    	totalProgressBar.setValue(65);
				    }
					r = Pattern.compile("Speed procedure took");
				    m = r.matcher(message);
				    if (m.find( )) totalProgressBar.setValue(70);
					r = Pattern.compile("Temperature procedure took");
				    m = r.matcher(message);
				    if (m.find( )) totalProgressBar.setValue(79);
					r = Pattern.compile("Raft procedure took");
				    m = r.matcher(message);
				    if (m.find( )) totalProgressBar.setValue(85);
					r = Pattern.compile("Jitter procedure took");
				    m = r.matcher(message);
				    if (m.find( )) totalProgressBar.setValue(88);
					r = Pattern.compile("Clip procedure took");
				    m = r.matcher(message);
				    if (m.find( )) totalProgressBar.setValue(99);

				    if(logIt)
				    	Base.logger.info(message);

				    progressLabel.setText(newMessage);
				}
			});
		}

		public void generationComplete(Completion completion, Object details) {
		}
	}
	
	public ToolpathGeneratorThread(Frame parent, ToolpathGenerator generator, Build build) {
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
			if (!generator.visualConfigure(parent)) { return; }
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
				build.reloadCode();
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
