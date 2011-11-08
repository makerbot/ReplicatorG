package replicatorg.app.util;

import java.awt.Frame;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.Set;
import java.util.TreeSet;
import java.util.Vector;
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
	/**
	 * Preference name for preferred Python path.
	 */
	final static String PYTON_PATH_PREF = "python.default_path";
	
	/**
	 * Callback for Python selector method.
	 */
	public interface Selector {
		/**
		 * Select the path to the desired Python implementation.
		 * @param candidates a list of paths to candidate implementations 
		 * @return the selected path (which need not be a member of the candidate list)
		 */
		String selectPythonPath(Vector<String> candidates);
	}
	
	private static Selector selector = null;
	
	
	/**
	 * Set the callback that allows a user to select the Python path.
	 */
	public static void setSelector(Selector selector) {
		PythonUtils.selector = selector;
	}
	
	/**
	 * Class representing a Python version.  The members are directly accessible. 
	 * @author phooky
	 */
	public static class Version implements Comparable<Version> {
		public int major;
		public int minor;
		public int revision;
		public Version(int major, int minor, int revision) {
			this.major = major;
			this.minor = minor;
			this.revision = revision;
		}
		/// returns 0 on match, 1 if this version is newer than other, -1 if this version is older than other
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
	static Version pythonVersion = null;
	
	/**
	 * Calculate the expected path to the Python installation.  The result is cached.
	 * @return the path as a string
	 */
	public static String getPythonPath() {
		return getPythonPath(null, null);
	}
	
	/**
	 * Calculate the expected path to the Python installation.  The result is cached.
	 * @return the path as a string
	 */
	public static String getPythonPath(Version minVersion, Version maxVersion) {
		// First, check if our cached python path, if any, meets the version requirements.
		boolean versionOk = false;
		if (pythonVersion != null) {
			versionOk = true;
			if (minVersion != null && pythonVersion.compareTo(minVersion) < 0) {
				versionOk = false;
			}
			if (maxVersion != null && maxVersion.compareTo(pythonVersion) < 0) {
				versionOk = false;
			}
		}
		if (versionOk && pythonPath != null) {
			return pythonPath;
		} else {
			pythonPath = null;
			pythonVersion = null;
		}

		// The candidate paths to python to check. 
		Set<String> candidates = new TreeSet<String>();
		
		// Assemble a list of candidate paths.
		// First, check if the user has explicitly set the Python path.
		{
			String path = Base.preferences.get(PYTON_PATH_PREF, null);
			if (path != null) {
				File candidate = new File(path);
				if (candidate.exists()) {
					candidates.add(candidate.getAbsolutePath());
				}
			}
		}
		// Second, look in the system path.  This is the default solution for
		// all platforms.
		String paths[] = System.getenv("PATH").split(File.pathSeparator);
		// On platforms with python 3.x installed, 2.x is sometimes named "python2".
		String pythonNames[] = { "python", "python2" };
		for (String path : paths) {
			for (String name : pythonNames) {
				File candidate = new File(path,name);
				if (candidate.exists()) {
					candidates.add(candidate.getAbsolutePath());
				}
			}
		}
		// Add platform-specific candidate paths.
		if (Base.isWindows()) {
			// The Windows python install does not add Python to the path by default.
			// We look for the install in the standard locations (C:\Python26, etc.)
			Pattern pythonPat = Pattern.compile("Python([0-9]+)");
			File driveDir = new File("C:/");
			if (driveDir.exists() && driveDir.isDirectory()) {
				for (String path : driveDir.list()) {
					Matcher match = pythonPat.matcher(path);
					if (match.matches()) {
						File python = new File(new File(driveDir,path),"python.exe");
						if (python.exists()) {
							candidates.add(python.getAbsolutePath());
						}
					}
				}
			}
		}
		
		for (String c : candidates) {
			@SuppressWarnings("unused")
			Version v = checkVersion(c,minVersion,maxVersion);
		}
		
		// Filter candidates by version
		Vector<String> viableCandidates = new Vector<String>();
		for (String candidate : candidates) {
			Version v = checkVersion(candidate, minVersion, maxVersion);
			if (v != null) {
				viableCandidates.add(candidate);
			}
		}
		
		if (selector != null && viableCandidates.size() > 1) {
			String path = selector.selectPythonPath(viableCandidates);
			if (path != null) {
				Base.preferences.put(PYTON_PATH_PREF, path);
				pythonPath = path;
				pythonVersion = checkVersion(pythonPath);
			}
		} else if (viableCandidates.size() > 0) {
			pythonPath = viableCandidates.firstElement();
			pythonVersion = checkVersion(pythonPath);
		}
		return pythonPath;
	}

	/**
	 * Check the version of the given python installation and return the version of python detected
	 * if it's within the minimum and maximum version limitations, or null if it doesn't meet the requirements.
	 * If either the maximum or minimum version is null, that constraint is ignored.
	 * @return null if python is not installed or an inappropriate version, or the acceptable
	 * version of python found.
	 */
	public static Version checkVersion(String path, Version minVersion, Version maxVersion) {
		Version v = checkVersion(path);
		if (v != null) {
			if (minVersion != null && v.compareTo(minVersion) < 0) {
				return null;
			}
			if (maxVersion != null && v.compareTo(maxVersion) > 0) {
				return null;
			}
		}
		return v;
	}
	
	/**
	 * Check for the existence of a working python at the standard location.
	 * @return null if python is not installed, or the version of python found. 
	 */
	public static Version checkVersion() {
		if (getPythonPath() == null) { return null; }
		return checkVersion(getPythonPath());
	}
	
	/**
	 * Check for the version of a working python binary at the given path. 
	 * @return null if python is not installed, or the version of python found. 
	 */
	public static Version checkVersion(String path) {
		ProcessBuilder pb = new ProcessBuilder(path,"-V");
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
	 * Check for a successful TkInter installation.  This should be installed by default on Windows
	 * and OS X, but needs to be explicitly installed on many Linux distributions.
	 * @return true if TkInter is successfully installed, false otherwise
	 */
	public static boolean checkTkInter() {
		if (getPythonPath() == null) { return false; }
		ProcessBuilder pb = new ProcessBuilder(getPythonPath(),"-c","import Tkinter");
		try {
			Process p = pb.start();
			int returnCode = p.waitFor();
			if (returnCode != 0) { return false; }
		} catch (Exception e) {
			Base.logger.log(Level.SEVERE,"Error attempting to detect TkInter",e);
		}
		return true;
	}

	/**
	 * Check for the existence of a running TkInter package.  Pops up an error dialog
	 * if TkInter was not found or incorrectly installed.
	 * @param parent A frame to parent the warning dialog 
	 * @param procedureName A string describing the procedure that requires python
	 * @return true if TkInter is successfully installed; false otherwise
	 */
	public static boolean interactiveCheckTkInter(final Frame parent, String procedureName) {
		boolean hasTkInter = checkTkInter();
		if (procedureName == null) { procedureName = "This operation"; }
		if (!hasTkInter) {
			String s = procedureName+" requires TkInter to be installed.  No valid TkInter install was found.";
			displayPythonErrorDialog(parent,s);
		}
		return hasTkInter;
	}

	/**
	 * Check for the existence and proper version of python.  Pops up an error dialog if
	 * python is missing or the wrong version was detected.  The detected python version
	 * must fall into the range <code>min &lt;= detected &lt; max</code> (if specified).
	 * @param parent A component to parent the warning dialog
	 * @param procedureName A string describing the procedure that requires python
	 * @param min The minimum acceptable version of python, null if we don't care.
	 * @param max The minimum <i>unacceptable</i> version of python, null if we don't care.
	 * @return true if python was found and falls within acceptable boundaries
	 */
	public static boolean interactiveCheckVersion(Frame parent, String procedureName, Version min, Version max) {
		getPythonPath(min,max);
		Version v = pythonVersion;
		if (procedureName == null) { procedureName = "This operation"; }
		if (v != null) {
			if (min != null && min.compareTo(v) == 1) {
				displayPythonErrorDialog(parent,procedureName+" requires Python version "+min.toString()+
						" or later.  Python version "+v.toString()+" was detected.");
				return false;
			}
			if (max != null && max.compareTo(v) != 1) {
				System.err.println("Comparing "+max.toString()+" to "+v.toString());
				System.err.println("Returned "+Integer.toString(max.compareTo(v)));
				displayPythonErrorDialog(parent,procedureName+" requires a version of Python earlier than version "+max.toString()+
						".  Python version "+v.toString()+" was detected.");
				return false;
			}
			return true;
		}
		displayPythonErrorDialog(parent,procedureName+" requires that a Python interpreter be installed.");
		return false;
	}

	/**
	 * Notify the user that there is a problem with their python install, and give them the option of visiting
	 * the Python site for installation instructions.  On Linux, the user instead is given a notice to install
	 * the "python" and "python-tk" packages.
	 * @param parent The frame to parent the warning dialog
	 * @param message A simple description of the problem with their install
	 */
	private static void displayPythonErrorDialog(final Frame parent, final String message) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				if (Base.isLinux()) {
					String s = "<html><p>"+message+"</p><p>Make sure your system has the 'python' and 'python-tk' packages installed.</p></html>";
					JOptionPane.showMessageDialog(parent, s, "Missing or incorrect Python interpreter detected", JOptionPane.ERROR_MESSAGE);
				} else {
					String s = "<html><p>"+message+"</p><p>Would you like to visit the Python download page now?</p></html>";
					int rsp = JOptionPane.showConfirmDialog(parent, s, "Missing or incorrect Python interpreter detected", JOptionPane.YES_NO_OPTION, JOptionPane.ERROR_MESSAGE);
					if (rsp == JOptionPane.YES_OPTION) {
						Base.openURL("http://python.org/download");
					}
				}
			}
		});
	}
	
	/**
	 * Sets the new preferred place to find python
	 */
	public static void setPythonPath(String path) {
		Base.preferences.put(PythonUtils.PYTON_PATH_PREF, path);
		pythonPath = path;
	}
}
