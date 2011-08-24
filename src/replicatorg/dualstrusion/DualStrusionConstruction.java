package replicatorg.dualstrusion;

import java.io.File;

/**
 * This class is designed to give the functionality of DualStrusionWorker's shuffle in its own thread, therefore it basically just calls that and implements runnable
 * @author Noah Levy
 *
 */
public class DualStrusionConstruction implements Runnable {

	private File result;
	private File primary, secondary, dest;
	private boolean replaceStart, replaceEnd;
	/**
	 * This constructor mimics the paramaters of shuffle
	 * @param p primary file
	 * @param s secondary file
	 * @param d destination file
	 * @param rs replace start
	 * @param re replace end
	 */
	public DualStrusionConstruction(File p, File s, File d, boolean rs, boolean re)
	{
		primary = p;
		secondary = s;
		dest = d;
		replaceStart = rs;
		replaceEnd = re;
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
	 * once a DSC is constructed this method calls shuffle
	 */
	public void run() {
		result = DualStrusionWorker.shuffle(primary, secondary, dest, replaceStart, replaceEnd);
		
	}
	
}
