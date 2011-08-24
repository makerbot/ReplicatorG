package replicatorg.app.ui;

import java.io.File;
import java.io.FileFilter;

import javax.swing.JFileChooser;
import javax.swing.JFrame;

public class GcodeSelectWindow  
{
	public static File lastdir;
	public static File go(File f)
	{
		JFileChooser chooser = new JFileChooser();
		chooser.setCurrentDirectory(f);
		JFrame chooseFramer = new JFrame("Find Gcode...");
		// Note: source for ExampleFileFilter can be found in FileChooserDemo,
		// under the demo/jfc directory in the Java 2 SDK, Standard Edition.
		myFileFilter filter = new myFileFilter();
		chooser.setFileFilter(filter);
		chooser.showOpenDialog(chooseFramer);
		File fr = chooser.getSelectedFile();
		lastdir = fr.getParentFile();
		return fr;
	}
	public static File go()
	{
		JFileChooser chooser = new JFileChooser();
		if(lastdir != null)
		{
		chooser.setCurrentDirectory(lastdir);
		}
		JFrame chooseFramer = new JFrame("Find Gcode...");
		// Note: source for ExampleFileFilter can be found in FileChooserDemo,
		// under the demo/jfc directory in the Java 2 SDK, Standard Edition.
		myFileFilter filter = new myFileFilter();
		chooser.setFileFilter(filter);
		chooser.showOpenDialog(chooseFramer);
		File f = chooser.getSelectedFile();
		lastdir = f.getParentFile();
		return f;
	}
	public static String goString()
	{
		return go().getAbsolutePath();
	}
	public static String goString(File f)
	{
		return go(f).getAbsolutePath();
	}
}

class myFileFilter extends javax.swing.filechooser.FileFilter {

	private static final String gcode = "gcode";
	private static final String stl = "stl";
	public boolean accept(File f)
	{
		//alexpong
		String extension = "";
		String s = f.getName();
		if(s.contains("."))
		{
			int i = s.lastIndexOf('.');
			if (i > 0 &&  i < s.length() - 1) {
				extension = s.substring(i+1).toLowerCase();
			}
			if(extension.equals(gcode) || extension.equals(stl))
			{
				return true;
			}
			return false;
		}
		return true;

	}
	@Override
	public String getDescription() {
		return "gcode";
	}

}