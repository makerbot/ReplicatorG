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


/**
 * This is the slicing Progress Dialog used to keep the user updated on slice 
 * progress for all slice engines. 
 * 
 * @author farmckon
 *
 */
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
    String currentSlicer;

    SkeinStep slicer[] = {
    		new SkeinStep("Processing triangulated mesh",12),
    		new SkeinStep("Generating perimeters",8),
    		new SkeinStep("Detecting solid surfaces",8),
    		new SkeinStep("Preparing infill surfaces",8),
    		new SkeinStep("Cleaning up",8),
    		new SkeinStep("Detect bridges",8),
    		new SkeinStep("Cleaning up the perimeters",8),
    		new SkeinStep("Generating horizontal shells",8),
    		new SkeinStep("DISCOVERING HORIZONTAL SHELLS",8),
    		new SkeinStep("Combining infill",8),
    		new SkeinStep("Infilling layers",8),
    		new SkeinStep("Generating skirt",8),
    };
	SkeinStep steps[] = {
			new SkeinStep("Carve",13), 
			new SkeinStep("Inset",27),
			new SkeinStep("Fill",12),
			new SkeinStep("Raft",36),
			new SkeinStep("Clip",8),
			new SkeinStep("Comb",4),
			new SkeinStep("Oozebane",5),
	};

	public ProgressDialog(Frame parent, Build build, Thread parentThread, String generator) { 
		super(parent);
		currentSlicer = generator;
		this.parentThread = parentThread;
		//Base.logger.severe("creating ProgressDialog. ParentThread" + parentThread.toString());
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
		add(new JLabel("Generator: "+generator),"wrap");
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
		//Base.logger.severe("Aborted toolpath generation!");
		if(parentThread != null) 
			parentThread.interrupt();
		synchronized(this) {
			setVisible(false);
			setDone(true);
		}
		
	}

	// Parsing for progress messages.  Precompile patterns to save runtime.
	static private Pattern patOfNum = Pattern.compile(" of ([0-9]+)...");
    static private Pattern patLayerCount = Pattern.compile("([A-Za-z]+) layer count ([0-9]+)");
	static private Pattern patOldLayerTotal = Pattern.compile("total Layer count is[^0-9]([0-9]+)[^0-9]");
	static private Pattern patFillingLayer = Pattern.compile("^Filling layer.*[^0-9]([0-9]+)[^0-9]");
	static private Pattern patOldFillingLayer = Pattern.compile("Filling layer[^0-9]([0-9]+)/([0-9]+)[^0-9]");
	static private Pattern patSliceToGcode = Pattern.compile("Slice to GCode.*layer ([0-9]+)[^0-9]");//Slice to GCode... layer %s.
	static private Pattern patProcedureTook = Pattern.compile("(.*) procedure took");
	
	// Parsing for slicer progress messages.
	static private Pattern patOfReverseBridge = Pattern.compile("Found reverse bridge");
	static private Pattern patOfDetection = Pattern.compile("Detecting solid surfaces...");
	static private Pattern patOfMembrane = Pattern.compile("layer ([0-9]+) contains ([0-9]+) membrane");
	static private Pattern patOfInternalSurface = Pattern.compile("internal surfaces found");
	static private Pattern patOfOverhang = Pattern.compile("overhang on layer ([0-9]+) with ([0-9]+) support");
	static private Pattern patOfBridgeSupport = Pattern.compile("Bridge is supported on ([0-9]+) edge");
	static private Pattern patOfBridgeInfill = Pattern.compile("Optimal infill angle of bridge on layer ([0-9]+) is ([0-9]+) degrees");
	static private Pattern patOfFillBridge = Pattern.compile("Filling bridge with angle ([0-9]+)");
	static private Pattern patOfSmallSurface = Pattern.compile("small surfaces at layer ([0-9]+)");
	static private Pattern patOfHorizontal = Pattern.compile("Facet is horizontal; ([A-Za-z]+)");
	static private Pattern patOfZLevel = Pattern.compile("z: min = ([0-9]+), max = ([0-9]+)");
	static private Pattern patOfFacets = Pattern.compile("==> FACET ([0-9]+)" );
	static private Pattern patOfPoly = Pattern.compile ("poly([A-Za-z]+)");
	static private Pattern patOfLayers = Pattern.compile("layers: min = ([0-9]+), max = ([0-9]+)");
	static private Pattern patOfPower = Pattern.compile("fan = ([0-9]+)%, speed = ([0-9]+)%");
	static private Pattern patOfSurfaces = Pattern.compile("Making surfaces for layer ([0-9]+)");
	static private Pattern patOfPerimeters = Pattern.compile("Making perimeters for layer ([0-9]+)");
	static private Pattern patOfLines = Pattern.compile("([0-9]+) lines start at point ([0-9]+)");
	static private Pattern patOfWalls = Pattern.compile("([0-9]+) thin walls detected");
	static private Pattern patOfSolids = Pattern.compile("Layer ([0-9]+)");
	static private Pattern patOfRemoveWall = Pattern.compile("removed ([0-9]+) unprintable perimeters at layer ([0-9]+)");
	static private Pattern patOfSolidShells = Pattern.compile("looking for neighbors on layer ([0-9]+)...");
	static private Pattern patOfFillLayer = Pattern.compile("Filling layer ([0-9]+):");
	static private Pattern patProcedureTakes = Pattern.compile("Done. Process took ([0-9]+) minutes and ([0-9]+.[0-9]+) seconds");
	static private Pattern patTotalFilament = Pattern.compile("Filament required: ([0-9]+.[0-9])mm *([0-9].[0-9])cm3");
	static private Pattern patNewProcedure = Pattern.compile("=> (.+)");
	
	@Override
	public void updateGenerator(final ToolpathGenerator.GeneratorEvent evt) {

		/// internal runnable class for setting updates
		SwingUtilities.invokeLater(new Runnable() {

			String processName = "";
			boolean logIt = true;
			boolean showIt = true;
			boolean showProgress = false;
			int sub;
			Matcher m = null;

			/// Parse newMessage and select slic3r specific data
			/// to display as part of the update.
			/// @param: line of text sent by slicing program
			/// @returns: updated tex to display display data 
			String doSlic3rUpdate(String newMessage)
			{
				m = patOfRemoveWall.matcher(newMessage);
				if (m.find())
				{
					logIt = false;
					subProgressBar.setIndeterminate(true);
					layerIndex = Integer.parseInt(m.group(2));
					subProgressBar.setIndeterminate(false);
					newMessage = "Removing unprintable perimeter at layer " + m.group(2);
				}
				
				m = patOfBridgeSupport.matcher(newMessage);
				if (m.find()) { logIt = false; showIt = false; }
				
				m = patOfWalls.matcher(newMessage);
				if (m.find()) { logIt = false; showIt = false; }
				
				m = patOfPower.matcher(newMessage);
				if (m.find()) { logIt = false; showIt = false; }
				
				m = patOfLines.matcher(newMessage);
				if (m.find()) { logIt = false; showIt = false; }
				
				m = patOfFillBridge.matcher(newMessage);
				if (m.find()) { logIt = false; showIt = false; }
				
				m = patOfBridgeInfill.matcher(newMessage);
				if (m.find()) { logIt = false; showIt = false; }
				
				m = patOfOverhang.matcher(newMessage);
				if (m.find()) { logIt = false; showIt = false; }
				
				m = patOfSmallSurface.matcher(newMessage);
				if (m.find()) { logIt = false; showIt = false; }
				
				m = patOfMembrane.matcher(newMessage);
				if (m.find()) { logIt = false; showIt = false; }
				
				m = patOfDetection.matcher(newMessage);
				if (m.find()) { logIt = false; showIt = false; }
				
				m = patOfInternalSurface.matcher(newMessage);
					if (m.find()) { logIt = false; showIt = false; }
				
				m = patOfHorizontal.matcher(newMessage);
				if (m.find()) { logIt = false; showIt = false; }

				m = patOfReverseBridge.matcher(newMessage);
				if (m.find()) { logIt = false; showIt = false; }
				
				m = patOfZLevel.matcher(newMessage);
				if (m.find()) { logIt = false; showIt = false; }
				
				m = patOfPoly.matcher(newMessage);
				if (m.find()) { logIt = false; showIt = false; }
				
				m = patOfFacets.matcher(newMessage);
				if (m.find())
				{
					logIt = false;
					subProgressBar.setIndeterminate(true);
					newMessage = "Processing facet " + m.group(1);
				}
				
				m = patOfLayers.matcher(newMessage);
				if (m.find())
				{
					logIt = false;
					showIt = false;
					int temp = Integer.parseInt(m.group(2));
					if (temp > layerTotal)
					{
						layerTotal = temp;
					}
				}
				
				m = patOfSurfaces.matcher(newMessage);
				if (m.find())
				{
					logIt = false;
					layerIndex = Integer.parseInt(m.group(1));
					subProgressBar.setIndeterminate(false);
					showProgress = true;
					processName = "Making surfaces";
					newMessage = processName + " (layer " + layerIndex +" of "+ layerTotal +")";
				}
				
				m = patOfPerimeters.matcher(newMessage);
				if (m.find())
				{
					logIt = false;
					layerIndex = Integer.parseInt(m.group(1));
					subProgressBar.setIndeterminate(false);
					showProgress = true;
					processName = "Making perimeters";
					newMessage = processName + " (layer " + layerIndex +" of "+ layerTotal +")";
				}
				
				m = patOfSolids.matcher(newMessage);
				if (m.find())
				{
					logIt = false;
					layerIndex = Integer.parseInt(m.group(1));
					subProgressBar.setIndeterminate(false);
					showProgress = true;
					processName = "Detecting solid surfaces...";
					newMessage = processName + " (layer " + layerIndex +" of "+ layerTotal +")";
				}
				m = patOfSolidShells.matcher(newMessage);
				if (m.find())
				{
					logIt = false;
					layerIndex = Integer.parseInt(m.group(1));
					subProgressBar.setIndeterminate(false);
					showProgress = true;
					processName = "looking for neighbors";
					newMessage = processName + " (layer " + layerIndex +" of "+ layerTotal +")";
				}
				m = patOfFillLayer.matcher(newMessage);
				if (m.find())
				{
					logIt = false;
					layerIndex = Integer.parseInt(m.group(1));
					subProgressBar.setIndeterminate(false);
					showProgress = true;
					processName = "Filling";
					newMessage = processName + " (layer " + layerIndex +" of "+ layerTotal +")";
				}	
				return newMessage;
			}

			/// @param: line of text sent by slicing program
			/// @returns: updated tex to display display data 
			String doSkeinforge33Update(String newMessage)
			{
				
				if(newMessage.startsWith(""+'\033'))
				{
					newMessage = newMessage.substring(4);
				}
				// skeinforBase.logger.info("DEBUG: " + slicer[currentProcessI].getStepPercentage(layerIndex,layerTotal));ge 33 (and up) format: \033[1AFill layer count 28 of 35...
				m = patOfNum.matcher(newMessage);
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
				return newMessage;
			}
			
			/// may also set % complete info via class member
			/// @param: line of text sent by slicing program
			/// @returns: updated tex to display display data 
			String doMiracleGrueUpdate(String newMessage)
			{
				
				//Base.logger.severe("doMiracleGrueUpdate: " + newMessage);
				int split = newMessage.indexOf("[");
				String base = (split >= 0) ? newMessage.substring(0, split) : newMessage;
				//Base.logger.severe("doMiracleGrueUpdate: split " + split);
				newMessage = base.trim();
				logIt = true;
				showIt = true;
				//Base.logger.severe("doMiracleGrueUpdate: newMessage " + newMessage);				
				return newMessage;
					
			}

			
			/// run event to post slice progress updates to GUI,
			/// each time this is called the new event message is checked, 
			/// and an update message/progress is created
			public void run() {

				String newMessage = evt.getMessage();

				synchronized(ProgressDialog.this) {

				
					if (currentSlicer.startsWith("Slic3r"))
						newMessage = doSlic3rUpdate(newMessage);
					else if( currentSlicer.startsWith("Miracle"))
						newMessage = doMiracleGrueUpdate(newMessage);
					else
						newMessage = doSkeinforge33Update(newMessage);
					
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
					
					m = patNewProcedure.matcher(newMessage);
					if (m.find( ))
					{
						if(m.group(0).contains("FACET"))
						{
						 //Do nothing...
						}
						else
						{
							for(int i=0;i<slicer.length;i++)
							{
								if(slicer[i].stepName.equals(m.group(1)))
								{
									newMessage = slicer[i].stepName;
									logIt = false;
									currentProcessI = i;
									subProgressBar.setIndeterminate(true);
									//Base.logger.info("Step: "+steps[i].stepName+" = "+ steps[i].thisStepTime+" of "+steps[i].totalStepTime + " = "+steps[i].getStepPercentage(layerIndex,layerTotal)+"%");

								}
							}
						}
					}
					
					if(currentProcessI >= 0)
					{
						if (currentSlicer.startsWith("Slic3r"))
							totalProgressBar.setValue(2 * ((int) slicer[currentProcessI].getStepPercentage(layerIndex,layerTotal)));
						else
							totalProgressBar.setValue((int) steps[currentProcessI].getStepPercentage(layerIndex,layerTotal));
					}else {
						if(layerTotal > 0)
						{
							subProgressBar.setValue((int) (((double)layerIndex/layerTotal)*100));
							subProgressBar.setIndeterminate(false);
						}
					}
					if(logIt==true)
						Base.logger.info(newMessage);
					
					if (showIt ==true)
						progressLabel.setText(newMessage);
						
					
				}
			}
		});
}

	@Override
	public void generationComplete(ToolpathGenerator.GeneratorEvent evt) {
		synchronized(this) {
			setVisible(false);
			setDone(true);
		}
	}
}
