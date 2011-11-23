package replicatorg.plugin.toolpath;

import java.awt.Frame;
import java.util.logging.Level;

import javax.swing.SwingUtilities;

import replicatorg.app.Base;
import replicatorg.model.Build;
import replicatorg.model.BuildCode;
import replicatorg.plugin.toolpath.ToolpathGenerator.GeneratorListener;

public class ToolpathGeneratorThread extends Thread {
	private Frame parent;
	private ToolpathGenerator generator;
	private Build build;
	private boolean supportDualStrusion = false;
	private boolean skipConfig = false;
	int x, y;
	String name;
	
	
	public ToolpathGeneratorThread(Frame parent, ToolpathGenerator generator, Build build) {
		// Naming the thread can ease debugging
		super("ToolpathGeneratorThread");
		this.parent = parent;
		this.generator = generator;
		this.build = build;
	}
	
	public ToolpathGeneratorThread(Frame parent, ToolpathGenerator generator, Build build, boolean skipConfig) {
		this(parent, generator, build);
		this.skipConfig = skipConfig;
	}
	
	
	public void addListener(ToolpathGenerator.GeneratorListener listener) {
		this.generator.addListener(listener);
	}
	
	
	public void setDualStrusionSupportFlag(boolean b, int lox, int loy, String loName)
	{
		supportDualStrusion = b;
		x  = lox;
		y = loy;
		name = loName;
	}
	
	public void run() {
		//System.out.println("alexpong");
		generator.setModel(build.getModel());
		ProgressDialog progressDialog = null;
		if (parent != null) {
			// Configure, if possible
			if(skipConfig)
			{
				if (!generator.nonvisualConfigure()) { return; }
			}
			else
			{
				if(!supportDualStrusion)
				{
					if (!generator.visualConfigure(parent)) { return; }
				}
				if(supportDualStrusion)
				{
					if (!generator.visualConfigure(parent, x, y, name)) { return; }
				}
			}
			
			progressDialog = new ProgressDialog(parent,build,this);
			generator.addListener(progressDialog);
			// This actually works because it's a modal dialog;
			// a new nested event loop is generated in the event loop
			// that blocks other events.
			if(!supportDualStrusion)
			{
				final ProgressDialog pdHandle = progressDialog;
				
				SwingUtilities.invokeLater(new Runnable() 
				{ public void run() 
				{
					if (!pdHandle.isDone()) 
					{
						double xl = parent.getBounds().getCenterX();
						double yl = parent.getBounds().getCenterY();
						pdHandle.pack();
						xl -= pdHandle.getWidth() / 2.0;
						yl -= pdHandle.getHeight() / 2.0;
						pdHandle.setLocation((int)xl,(int)yl);
						pdHandle.setVisible(true);
					}
				}});
			}
			if(supportDualStrusion)
			{
				final ProgressDialog pdHandle = progressDialog;
				System.out.println("pd attempt start");
				SwingUtilities.invokeLater(new Runnable() 
				{ public void run() 
				{
					if (!pdHandle.isDone()) 
					{
						//System.out.println("akexoibg2");
						//pdHandle.setSize(400, 270);
						pdHandle.pack();
						pdHandle.setLocation(x,y);
						pdHandle.setName(name);
						//pdHandle.setTitle(name);
						pdHandle.setVisible(true);
					}
				}});
			}
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
		SkeinStep.totalStepTime += thisStepTime;
		this.stepName = stepName;
		this.thisStepTime = thisStepTime;
		this.incrementalStepTime = SkeinStep.totalStepTime;
	}
	public int getStepPercentage(int layerIndex, int layerTotal)
	{
		int percentage = (int) ((double) (this.incrementalStepTime-this.thisStepTime)/totalStepTime*100);
		if((layerTotal > 0)&&(layerTotal!=layerIndex)){
			//Base.logger.info("layer "+layerIndex+"/"+layerTotal+" step "+this.thisStepTime+"/"+this.totalStepTime);
			percentage += (int) (((double) this.thisStepTime/SkeinStep.totalStepTime)*((double)layerIndex/layerTotal)*100);
		}
		return percentage;
	}
}
