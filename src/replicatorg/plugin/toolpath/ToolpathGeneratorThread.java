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
		JLabel totalProgressLabel;
		JProgressBar totalProgressBar;
		JButton doneButton;
		int layerIndex;
		int layerTotal;
	    int currentProcessI = -1;
		SkeinStep steps[] = {
				new SkeinStep("Carve",6), 
				new SkeinStep("Preface",1),
				new SkeinStep("Inset",8),
				new SkeinStep("Fill",18),
				new SkeinStep("Raft",8),
				new SkeinStep("Oozebane",5),
		};
		
		public ProgressDialog(Frame parent, Build build) { 
			super(parent);
			ImageIcon icon = new ImageIcon(Base.getDirectImage("images/slicing-icon.gif",this));
			setTitle("Generating toolpath for "+build.getName());
			topLabel = new JLabel("Generating toolpath for "+build.getName(),icon,SwingConstants.LEFT);
			icon.setImageObserver(topLabel);
			progressLabel = new JLabel("Launching plugin...");
			subProgressBar = new JProgressBar();
			totalProgressLabel = new JLabel("Total progress:");
			totalProgressBar = new JProgressBar();
			subProgressBar.setValue(0);
			subProgressBar.setStringPainted(false);
			subProgressBar.setValue(0);
			totalProgressBar.setStringPainted(false);
			int layerIndex = 0;
			int layerTotal= 9999;
			setLayout(new MigLayout());
			add(topLabel,"wrap");
			add(new JLabel("Generator: Skeinforge"),"wrap");
			add(progressLabel,"wrap,growx");
			add(subProgressBar,"wrap,wmin 400px");
			add(totalProgressLabel,"wrap,growx");
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
				    String processName = "";
					boolean logIt = true;
				    boolean showProgress = false;
					int sub;

					if(newMessage.startsWith(""+'\033'))
					{
				    	newMessage = newMessage.substring(4);
				    }
				    // skeinforge 33 (and up) format: \033[1AFill layer count 28 of 35...
				    Pattern r = Pattern.compile(" of ([0-9]+)...");
				    Matcher m = r.matcher(newMessage);
				    if (m.find( )) {
		    			logIt = false;
			    		layerTotal = Integer.parseInt(m.group(1));
				    }
				    // skeinforge 33 (and up)
				    r = Pattern.compile("([A-Za-z]+) layer count ([0-9]+)");
				    m = r.matcher(newMessage);
				    if (m.find( )) {
				    	processName = m.group(1);
			    		layerIndex = Integer.parseInt(m.group(2));
		    			logIt = false;
			    		if(layerTotal > 0) {
			    			subProgressBar.setIndeterminate(false);
			    			showProgress = true;
					    	newMessage = processName + " (layer " + layerIndex +" of "+ layerTotal +")";
			    		} else {
					    	newMessage = processName + " (layer " + layerIndex +")";
			    			subProgressBar.setIndeterminate(true);
			    		}
				    }
				    
				    // Older skeinforge's
					r = Pattern.compile("total Layer count is[^0-9]([0-9]+)[^0-9]");
				    m = r.matcher(newMessage);
				    if (m.find( )) {
			    		layerTotal = Integer.parseInt(m.group(1));
				    }
					r = Pattern.compile("^Filling layer.*[^0-9]([0-9]+)[^0-9]");
				    m = r.matcher(newMessage);
				    if (m.find( ))
				    {
				    	layerIndex = Integer.parseInt(m.group(1));
				    	showProgress = true;
				    	logIt = false;
				    	sub = (int) (55*((double) layerIndex)/ layerTotal);
				    	totalProgressBar.setValue(10 + sub);				    	
				    }

				    // THE ONE BELOW IS JUST FOR THE OLDER SKEINFORGE < 31!
					r = Pattern.compile("Filling layer[^0-9]([0-9]+)/([0-9]+)[^0-9]");
				    m = r.matcher(newMessage);
				    if (m.find( )) {
			    		layerIndex = Integer.parseInt(m.group(1));
			    		layerTotal = Integer.parseInt(m.group(2));
			    		showProgress = true;
			    		logIt = false;
				    	sub = (int) (55*((double) layerIndex)/ layerTotal);
				    	totalProgressBar.setValue(10 + sub);
				    }				    
				    				    
					r = Pattern.compile("Slice to GCode.*layer ([0-9]+)[^0-9]");//Slice to GCode... layer %s.
				    m = r.matcher(newMessage);
				    if (m.find( ))
				    {
				    	layerIndex = Integer.parseInt(m.group(1));
				    	showProgress = true;
				    	logIt = false;
				    	sub = (int) (2*((double) layerIndex)/ layerTotal);
				    	totalProgressBar.setValue(2 + sub);				    	
				    }
				    /*
					r = Pattern.compile("Insetting.*layer ([0-9]+)[^0-9]");
				    m = r.matcher(newMessage);
				    if (m.find( ))
				    {
				    	layerIndex = Integer.parseInt(m.group(1));
				    	showProgress = true;
				    	logIt = false;
				    	sub = (int) (6*((double) layerIndex)/ layerTotal);
				    	totalProgressBar.setValue(4 + sub);
				    }
				    */
				    if(showProgress)
				    {
				    	String i = new Integer(layerIndex).toString();
						String j = new Integer(layerTotal).toString();
						double completion =  ((double) layerIndex/layerTotal);
						NumberFormat nf = NumberFormat.getPercentInstance();
						String perc = nf.format(completion);
					    if((layerIndex>0) && (processName == ""))
					    {
					    	newMessage += " ("+j+" layers)";//
					    }
					    subProgressBar.setValue((int) (100*completion));
					}
				    /*
					r = Pattern.compile("Fill procedure took");
				    m = r.matcher(newMessage);
				    if (m.find( ))
				    {
				    	subProgressBar.setIndeterminate(true);
				    	// http://download.oracle.com/javase/tutorial/uiswing/components/progress.html#indeterminate
				    	totalProgressBar.setValue(65);
				    }

					r = Pattern.compile("Speed procedure took");
				    m = r.matcher(newMessage);
				    if (m.find( )) totalProgressBar.setValue(70);
					r = Pattern.compile("Temperature procedure took");
				    m = r.matcher(newMessage);
				    if (m.find( )) totalProgressBar.setValue(79);
					r = Pattern.compile("Raft procedure took");
				    m = r.matcher(newMessage);
				    if (m.find( )) totalProgressBar.setValue(85);
					r = Pattern.compile("Jitter procedure took");
				    m = r.matcher(newMessage);
				    if (m.find( )) totalProgressBar.setValue(88);
					r = Pattern.compile("Clip procedure took");
				    m = r.matcher(newMessage);
				    if (m.find( )) totalProgressBar.setValue(99);
					r = Pattern.compile("The extrusion fill density ratio");
				    m = r.matcher(newMessage);
				    if (m.find( )) totalProgressBar.setValue(99);
					*/
					r = Pattern.compile("(.*) procedure took");
				    m = r.matcher(newMessage);
				    if (m.find( ))
				    {	
					    for(int i=0;i<5;i++)
					    {
					    	if(steps[i].stepName.equals(m.group(1)))
					    	{
					    		currentProcessI = i;
					    		subProgressBar.setIndeterminate(true);
					    		//Base.logger.info("Step: "+steps[i].stepName+" = "+ steps[i].thisStepTime+" of "+steps[i].totalStepTime + " = "+steps[i].getStepPercentage(layerIndex,layerTotal)+"%");
					    		
					    	}
					    }
				    }
				    if(currentProcessI >= 0)
				    {
				    	totalProgressBar.setValue((int) steps[currentProcessI].getStepPercentage(layerIndex,layerTotal));
				    } else {
				    	if(layerTotal > 0)
				    	{
				    	subProgressBar.setValue((int) (((double)layerIndex/layerTotal)*100));
				    	subProgressBar.setIndeterminate(false);
				    	}
				    }
				    if(logIt==true)
				    	Base.logger.info(newMessage);
				    	
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


class SkeinStep {
	public static int totalStepTime;
	public String stepName;
	public int thisStepTime; 
	public int incrementalStepTime; 
	
	public SkeinStep(String stepName, int thisStepTime){
		this.totalStepTime += thisStepTime;
		this.stepName = stepName;
		this.thisStepTime = thisStepTime;
		this.incrementalStepTime = this.totalStepTime;
	}
	public int getStepPercentage(int layerIndex, int layerTotal)
	{
		int percentage = (int) ((double) (this.incrementalStepTime-this.thisStepTime)/totalStepTime*100);
		if((layerTotal > 0)&&(layerTotal!=layerIndex)){
			//Base.logger.info("layer "+layerIndex+"/"+layerTotal+" step "+this.thisStepTime+"/"+this.totalStepTime);
			percentage += (int) (((double) this.thisStepTime/this.totalStepTime)*((double)layerIndex/layerTotal)*100);
		}
		return percentage;
	}
}
