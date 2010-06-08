package replicatorg.app.util;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JComponent;
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
	/**
	 * Check for the existence of a working python. 
	 * @return null if python is not installed, or the version of python found. 
	 */
	public static Version checkVersion() {
		ProcessBuilder pb = new ProcessBuilder("python","-V");
		pb.redirectErrorStream(true);
		try {
			Process p = pb.start();
			BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
			int returnCode = p.waitFor();
			if (returnCode != 0) { return null; }
	
			Pattern pattern = Pattern.compile("Python ([0-9]+)\\.([0-9]+)\\.([0-9]+)");
			String line = reader.readLine();
			while (line != null) {
				Matcher m = pattern.matcher(line);
				if (m.find()) {
				return new Version(Integer.parseInt(m.group(1)),
						Integer.parseInt(m.group(2)),
						Integer.parseInt(m.group(3)));
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
	public static boolean interactiveCheckVersion(JComponent parent, String procedureName, Version min, Version max) {
		Version v = checkVersion();
		if (procedureName == null) { procedureName = "This operation"; }
		if (v != null) {
			if (min != null && min.compareTo(v) == 1) {
				notifyUser(parent,procedureName+" requires Python version "+min.toString()+
						" or later.  Python version "+v.toString()+" was detected.");
				return false;
			}
			if (max != null && max.compareTo(v) != -1) {
				notifyUser(parent,procedureName+" requires a version of Python earlier than version "+max.toString()+
						".  Python version "+v.toString()+" was detected.");
				return false;
			}
			return true;
		}
		notifyUser(parent,procedureName+" requires the Python interpreter to be installed.");
		return false;
	}
	
	private static void notifyUser(final JComponent parent, final String message) {
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
