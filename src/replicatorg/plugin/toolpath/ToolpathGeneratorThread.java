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
			progressDialog = new ProgressDialog(parent,build,this);
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
