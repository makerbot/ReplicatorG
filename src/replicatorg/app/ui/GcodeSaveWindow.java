package replicatorg.app.ui;

import java.io.File;
import java.io.FileFilter;

import javax.swing.JFileChooser;
import javax.swing.JFrame;

public class GcodeSaveWindow  
{
	public static File go(File f)
	{
		JFileChooser chooser = new JFileChooser();
		chooser.setSelectedFile(f);
		System.out.println("here");
		JFrame chooseFramer = new JFrame("Save Gcode...");
		// Note: source for ExampleFileFilter can be found in FileChooserDemo,
		// under the demo/jfc directory in the Java 2 SDK, Standard Edition.
		myFileFilter filter = new myFileFilter();
		chooser.setFileFilter(filter);
		chooser.showSaveDialog(chooseFramer);
		File fr = chooser.getSelectedFile();
		GcodeSelectWindow.lastdir = fr.getParentFile();
		return fr;
	}
	public static File go()
	{
		JFileChooser chooser = new JFileChooser();
		if(GcodeSelectWindow.lastdir != null)
		{
		chooser.setCurrentDirectory(GcodeSelectWindow.lastdir);
		}
		JFrame chooseFramer = new JFrame("Save Gcode...");
		// Note: source for ExampleFileFilter can be found in FileChooserDemo,
		// under the demo/jfc directory in the Java 2 SDK, Standard Edition.
		myFileFilter filter = new myFileFilter();
		chooser.setFileFilter(filter);
		chooser.showSaveDialog(chooseFramer);
		File f = chooser.getSelectedFile();
		GcodeSelectWindow.lastdir = f.getParentFile();
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
