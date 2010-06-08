package replicatorg.app.util;

import java.awt.Desktop;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

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
				String s = "<html><p>"+message+"</p><p>Visit the <a href=\"http://python.org/download/\">Python download page</a> to download and install "+
				"an appropriate version.</p></html>";
				JEditorPane j = new JEditorPane("text/html",s);
				j.setOpaque(false);
				j.setEditable(false);
				j.setBorder(null);
				j.addHyperlinkListener(new HyperlinkListener() {
					public void hyperlinkUpdate(HyperlinkEvent e) {
						try {
							Desktop.getDesktop().browse(e.getURL().toURI());
						} catch (IOException ioe) {
							Base.logger.log(Level.SEVERE,"Couldn't open link to python dowload site",ioe);
						} catch (URISyntaxException use) {
							Base.logger.log(Level.SEVERE,"Couldn't create link to python dowload site",use);
						}
					}
				});
				JOptionPane.showMessageDialog(parent, j, "Missing or incorrect Python interpreter detected", JOptionPane.ERROR_MESSAGE, null);
			}
		});
	}
}
