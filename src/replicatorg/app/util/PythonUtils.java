package replicatorg.app.util;

import java.awt.Frame;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import replicatorg.app.Base;

/**
 * Utilities for detecting python versions and running python apps or scripts from
 * ReplicatorG.
 * @author phooky
 *
 */
public class PythonUtils {
	public static class Version implements Comparable<Version> {
		public int major;
		public int minor;
		public int revision;
		public Version(int major, int minor, int revision) {
			this.major = major;
			this.minor = minor;
			this.revision = revision;
		}
		public int compareTo(Version other) {
			if (major < other.major) return -1;
			if (major > other.major) return 1;
			if (minor < other.minor) return -1;
			if (minor > other.minor) return 1;
			if (revision < other.revision) return -1;
			if (revision > other.revision) return 1;
			return 0;
		}
		public String toString() {
			return Integer.toString(major)+"."+Integer.toString(minor)+"."+Integer.toString(revision);
		}
	}
	
	static String pythonPath = null;
	public static String getPythonPath() {
		if (pythonPath == null) {
			// First, look in the system path.  This is the default solution for
			// all platforms.
			String paths[] = System.getenv("PATH").split(File.pathSeparator);
			for (String path : paths) {
				File candidate = new File(path,"python");
				if (candidate.exists() && candidate.canExecute()) {
					try {
						pythonPath = candidate.getCanonicalPath();
						return pythonPath;
					} catch (IOException ioe) {
						pythonPath = null;
					}
				}
			}
			// We've exhausted the system path; let's try platform-specific solutions.
			if (Base.isWindows()) {
				// The Windows python install does not add Python to the path by default.
				// We look for the install in the standard locations (C:\Python26, etc.)
				Pattern pythonPat = Pattern.compile("Python([0-9]+)");
				File driveDir = new File("C:/");
				if (driveDir.exists() && driveDir.isDirectory()) {
					for (String path : driveDir.list()) {
						Matcher match = pythonPat.matcher(path);
						if (match.matches()) {
							try {
								File python = new File(new File(driveDir,path),"python.exe");
								pythonPath = python.getCanonicalPath();
								return pythonPath;
							} catch (IOException ioe) {
								pythonPath = null;
							}
						}
					}
				}
			}
		}
		return pythonPath;
	}
	
	/**
	 * Check for the existence of a working python. 
	 * @return null if python is not installed, or the version of python found. 
	 */
	public static Version checkVersion() {
		if (getPythonPath() == null) { return null; }
		ProcessBuilder pb = new ProcessBuilder(getPythonPath(),"-V");
		pb.redirectErrorStream(true);
		try {
			Process p = pb.start();
			BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
			int returnCode = p.waitFor();
			if (returnCode != 0) { return null; }
	
			String line = reader.readLine();
			Pattern pattern = Pattern.compile("Python ([0-9]+)\\.([0-9]+)(?:\\.([0-9]+))?");
			while (line != null) {
				Matcher m = pattern.matcher(line);
				if (m.find()) {
					int major = Integer.parseInt(m.group(1));
					int minor = Integer.parseInt(m.group(2));
					int revision = m.group(3)!=null?Integer.parseInt(m.group(3)):0;
					return new Version(major,minor,revision);
				}
				line = reader.readLine();
			}
		} catch (Exception e) {
			Base.logger.log(Level.SEVERE,"Error attempting to detect python",e);
		}
		return null;
	}

	/**
	 * Check for the existence and proper version of python.  Pops up an error dialog if
	 * python is missing or the wrong version was detected.  The detected python version
	 * must fall into the range <code>min &lt;= detected &lt; max</code> (if specified).
	 * @param parent A component to parent the warning dialog
	 * @param procedureName A string describing the procedure that requires python
	 * @param min The minimum acceptable version of python, null if we don't care.
	 * @param max The minimum <i>unacceptable</i> version of python, null if we don't care.
	 * @return true if python was found and falls within acceptable boundries
	 */
	public static boolean interactiveCheckVersion(Frame parent, String procedureName, Version min, Version max) {
		Version v = checkVersion();
		if (procedureName == null) { procedureName = "This operation"; }
		if (v != null) {
			if (min != null && min.compareTo(v) == 1) {
				notifyUser(parent,procedureName+" requires Python version "+min.toString()+
						" or later.  Python version "+v.toString()+" was detected.");
				return false;
			}
			if (max != null && max.compareTo(v) != 1) {
				System.err.println("Comparing "+max.toString()+" to "+v.toString());
				System.err.println("Returned "+Integer.toString(max.compareTo(v)));
				notifyUser(parent,procedureName+" requires a version of Python earlier than version "+max.toString()+
						".  Python version "+v.toString()+" was detected.");
				return false;
			}
			return true;
		}
		notifyUser(parent,procedureName+" requires the Python interpreter to be installed.");
		return false;
	}
	
	private static void notifyUser(final Frame parent, final String message) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				String s = "<html><p>"+message+"</p><p>Would you like to visit the Python download page now?</p></html>";
				int rsp = JOptionPane.showConfirmDialog(parent, s, "Missing or incorrect Python interpreter detected", JOptionPane.YES_NO_OPTION, JOptionPane.ERROR_MESSAGE);
				if (rsp == JOptionPane.YES_OPTION) {
					Base.openURL("http://python.org/download");
				}
			}
		});
	}
}
