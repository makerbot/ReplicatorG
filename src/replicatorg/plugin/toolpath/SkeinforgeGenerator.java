package replicatorg.plugin.toolpath;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;

import replicatorg.app.Base;
import replicatorg.app.util.StreamLoggerThread;
import replicatorg.model.BuildCode;

public class SkeinforgeGenerator extends ToolpathGenerator {

	public BuildCode generateToolpath() {
		String path = model.getSTLPath();
		// The -u makes python output unbuffered.  Oh joyous day.
		ProcessBuilder pb = new ProcessBuilder("python","-u","skeinforge.py",path);
	    String skeinforgeDir = System.getProperty("replicatorg.skeinforge.path");
	    if (skeinforgeDir == null || (skeinforgeDir.length() == 0)) {
	    	skeinforgeDir = System.getProperty("user.dir") + File.separator + "skeinforge";
	    }
		pb.directory(new File(skeinforgeDir));
		try {
			Process process = pb.start();
			StreamLoggerThread ist = new StreamLoggerThread(process.getInputStream()) {
				@Override
				protected void logMessage(String line) {
					listener.updateGenerator(line);
					super.logMessage(line);
				}
			};
			StreamLoggerThread est = new StreamLoggerThread(process.getErrorStream());
			est.setDefaultLevel(Level.SEVERE);
			ist.start();
			est.start();
			int value = process.waitFor();
			if (value != 0) {
				Base.logger.severe("Unrecognized error code returned by Skeinforge.");
				// Throw ToolpathGeneratorException
				return null;
			}
		} catch (IOException ioe) {
			Base.logger.log(Level.SEVERE, "Could not run skeinforge.", ioe);
			// Throw ToolpathGeneratorException
			return null;
		} catch (InterruptedException e) {
			// We are most likely shutting down.  Exit gracefully.
			return null;
		}
		int lastIdx = path.lastIndexOf('.'); 
		String root = (lastIdx >= 0)?path.substring(0,lastIdx):path;
		return new BuildCode(root,new File(root+".gcode"));
	}

}
