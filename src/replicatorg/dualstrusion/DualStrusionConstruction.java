package replicatorg.dualstrusion;

import java.io.File;

public class DualStrusionConstruction implements Runnable {

	private File result;
	private File primary, secondary, dest;
	private boolean replaceStart, replaceEnd;
	public DualStrusionConstruction(File p, File s, File d, boolean rs, boolean re)
	{
		primary = p;
		secondary = s;
		dest = d;
		replaceStart = rs;
		replaceEnd = re;
	}
	public File getCombinedFile()
	{
	return result;	
	}
	@Override
	public void run() {
		result = DualStrusionWorker.shuffle(primary, secondary, dest, replaceStart, replaceEnd);
		
	}
	
}
