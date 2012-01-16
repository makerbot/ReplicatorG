package replicatorg.dualstrusion;

import java.io.File;
import java.util.logging.Level;

import replicatorg.app.Base;


/**
 * This class is designed to give the functionality of DualStrusionWorker's shuffle in its own thread, therefore it basically just calls that and implements runnable
 * @author Noah Levy
 *
 */
public class DualStrusionConstruction implements Runnable {

	private File result;
	private File primary, secondary, dest;
	private boolean replaceStart, replaceEnd, useWipes;
	//private Thread wait1, wait2;
	/**
	 * This constructor mimics the paramaters of shuffle
	 * @param p primary file
	 * @param s secondary file
	 * @param d destination file
	 * @param rs replace start
	 * @param re replace end
	 */
	public DualStrusionConstruction(File p, File s, File d, boolean rs, boolean re, boolean uW)
	{
		primary = p;
		secondary = s;
		dest = d;
		replaceStart = rs;
		replaceEnd = re;
		useWipes = uW;
	//	wait1 = t1;
		//wait2 = t2;
	}
	/**
	 * This method is my attempt at making a thread safe way to get the file made by combining the gcode
	 * @return
	 */
	public synchronized File getCombinedFile()
	{
	return result;	
	}
	@Override
	/**
	 * once a DSC is constructed this method calls combineGcode 
	 */
	public void run() {
		System.out.println("DSW primary " + primary.getName() + " secondary " + secondary.getName());
		try
		{
			result = DualStrusionWorker.combineGcode(primary, secondary, dest, replaceStart, replaceEnd, useWipes);
			Base.getEditor().handleOpenFile(result);
		}
		catch(Exception e)
		{
			Base.logger.log(Level.SEVERE, "Could not finish combining gcodes for dualstrusion, Sorry.\n" +
					"Dualstrusion is still very new functionality and is currently being improved.");
			e.printStackTrace();
		}
	}
	
}
