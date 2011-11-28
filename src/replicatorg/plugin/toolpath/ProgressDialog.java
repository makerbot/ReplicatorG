package replicatorg.plugin.toolpath;

import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

class ProgressDialog extends JDialog implements ToolpathGenerator.GeneratorListener {
	Thread parentThread;
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
			new SkeinStep("Carve",13), 
			new SkeinStep("Inset",27),
			new SkeinStep("Fill",12),
			new SkeinStep("Raft",36),
			new SkeinStep("Clip",8),
			new SkeinStep("Comb",4),
			new SkeinStep("Oozebane",5),
	};
	
	public ProgressDialog(Frame parent, Build build, Thread parentThread) { 
		super(parent);
		this.parentThread = parentThread;
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
		setLayout(new MigLayout());
		add(topLabel,"wrap");
		add(new JLabel("Generator: Skeinforge"),"wrap");
		add(progressLabel,"wrap,growx,wmax 400px");
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
		this.setResizable(false);
		this.setModal(false);

		// Escape key to abort generation
		doneButton.addKeyListener( new KeyAdapter()  {
			public void keyPressed ( KeyEvent e ) {
				if(e.getKeyCode() == KeyEvent.VK_ESCAPE) {
					synchronized(this) {
						abortGeneration();
					}
				}
			}
		} );
	}

	boolean done = false;
	
	public boolean isDone() {
		return done;
	}
	
	/**
	 * SetVisible is overridden to ensure that we don't retain dangling references
	 * to the toolpath generation thread after it's done.  This helps reduce the
	 * damage from http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6497929
	 */
	@Override
	public void setVisible(boolean b) {
		if (b == false) {
			// Alleviate memory leak.
			parentThread = null;
		}
		super.setVisible(b);
	}
	
	public void setDone(boolean done) {
		this.done = done;
	}

	private void abortGeneration() {
		Base.logger.severe("Aborted toolpath generation!");
		parentThread.interrupt();
	}

	// Parsing for progress messages.  There is no need to compile all these patterns every single time.
	static private Pattern patOfNum = Pattern.compile(" of ([0-9]+)...");
    static private Pattern patLayerCount = Pattern.compile("([A-Za-z]+) layer count ([0-9]+)");
	static private Pattern patOldLayerTotal = Pattern.compile("total Layer count is[^0-9]([0-9]+)[^0-9]");
	static private Pattern patFillingLayer = Pattern.compile("^Filling layer.*[^0-9]([0-9]+)[^0-9]");
	static private Pattern patOldFillingLayer = Pattern.compile("Filling layer[^0-9]([0-9]+)/([0-9]+)[^0-9]");
	static private Pattern patSliceToGcode = Pattern.compile("Slice to GCode.*layer ([0-9]+)[^0-9]");//Slice to GCode... layer %s.
	static private Pattern patProcedureTook = Pattern.compile("(.*) procedure took");
			
	public void updateGenerator(final String message) {

		SwingUtilities.invokeLater(new Runnable() {

			public void run() {
				synchronized(ProgressDialog.this) {
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
					Matcher m = patOfNum.matcher(newMessage);
					if (m.find( )) {
						logIt = false;
						layerTotal = Integer.parseInt(m.group(1));
					}
					// skeinforge 33 (and up)
					m = patLayerCount.matcher(newMessage);
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
					m = patOldLayerTotal.matcher(newMessage);
					if (m.find( )) {
						layerTotal = Integer.parseInt(m.group(1));
					}
					m = patFillingLayer.matcher(newMessage);
					if (m.find( ))
					{
						layerIndex = Integer.parseInt(m.group(1));
						showProgress = true;
						logIt = false;
						sub = (int) (55*((double) layerIndex)/ layerTotal);
						totalProgressBar.setValue(10 + sub);				    	
					}

					// THE ONE BELOW IS JUST FOR THE OLDER SKEINFORGE < 31!
					m = patOldFillingLayer.matcher(newMessage);
					if (m.find( )) {
						layerIndex = Integer.parseInt(m.group(1));
						layerTotal = Integer.parseInt(m.group(2));
						showProgress = true;
						logIt = false;
						sub = (int) (55*((double) layerIndex)/ layerTotal);
						totalProgressBar.setValue(10 + sub);
					}				    

					m = patSliceToGcode.matcher(newMessage);
					if (m.find( ))
					{
						layerIndex = Integer.parseInt(m.group(1));
						showProgress = true;
						logIt = false;
						sub = (int) (2*((double) layerIndex)/ layerTotal);
						totalProgressBar.setValue(2 + sub);				    	
					}
					if(showProgress)
					{
						String j = new Integer(layerTotal).toString();
						double completion =  ((double) layerIndex/layerTotal);
						if((layerIndex>0) && (processName == ""))
						{
							newMessage += " ("+j+" layers)";//
						}
						subProgressBar.setValue((int) (100*completion));
					}
					m = patProcedureTook.matcher(newMessage);
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
			}
		});
	}

	public void generationComplete(Completion completion, Object details) {
		synchronized(this) {
			setVisible(false);
			setDone(true);
		}
	}
}
