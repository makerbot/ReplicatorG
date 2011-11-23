/*
 Base.java

 Main class for the app.

 Part of the ReplicatorG project - http://www.replicat.org
 Copyright (c) 2008 Zach Smith

 Forked from Arduino: http://www.arduino.cc
 
 Based on Processing http://www.processing.org
 Copyright (c) 2004-05 Ben Fry and Casey Reas
 Copyright (c) 2001-04 Massachusetts Institute of Technology

 This program is free software; you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation; either version 2 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program; if not, write to the Free Software Foundation,
 Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package replicatorg.app;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.Toolkit;
//import java.awt.TrayIcon;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.URL;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.HashSet;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import javax.imageio.ImageIO;
import javax.swing.JComponent;
import javax.swing.JPopupMenu;
import javax.swing.JRootPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import replicatorg.app.ui.MainWindow;
import replicatorg.app.ui.NotificationHandler;
import replicatorg.machine.MachineLoader;
import replicatorg.uploader.FirmwareUploader;
import ch.randelshofer.quaqua.QuaquaManager;

import com.apple.mrj.MRJApplicationUtils;
import com.apple.mrj.MRJOpenDocumentHandler;

/**
 * Primary role of this class is for platform identification and general
 * interaction with the system (launching URLs, loading files and images, etc)
 * that comes from that.
 */
public class Base {
	public enum InitialOpenBehavior {
		OPEN_LAST,
		OPEN_NEW,
		OPEN_SPECIFIC_FILE
	};
	
	/**
	 * The version number of this edition of replicatorG.
	 */
	public static final int VERSION = 28;
	
	/**
	 * The textual representation of this version (4 digits, zero padded).
	 */
	public static final String VERSION_NAME = String.format("%04d",VERSION);

	/**
	 * The machine controller in use.
	 */
	private static MachineLoader machineLoader;
	
	/**
	 * The user preferences store.
	 */
	static public Preferences preferences = Preferences.userNodeForPackage(Base.class);

	/**
	*  Simple base data capture logger. So simple, but useful.
	*/
	static public DataCapture capture;
	
	/**
	 * The general-purpose logging object.
	 */
	public static Logger logger = Logger.getLogger("replicatorg.log");
	public static FileHandler logFileHandler = null;
	public static String logFilePath = null;
	
	/**
	 * Start logging on the given path. If the path is null, stop file logging.
	 * @param path The path to log messages to
	 */
	public static void setLogFile(String path) {
		boolean useLogFile = Base.preferences.getBoolean("replicatorg.useLogFile",false);

		if (useLogFile && path.equals(logFilePath)) { return; }
		
		if (logFileHandler != null) {
			logger.removeHandler(logFileHandler);
			logFileHandler = null;
		}
		
		logFilePath = path;
		
		if (useLogFile && logFilePath != null && logFilePath.length() > 0) {
			boolean append = true;
			try {
				FileHandler fh = new FileHandler(logFilePath, append);
				fh.setFormatter(new SimpleFormatter());
				fh.setLevel(Level.ALL);
				logFileHandler = fh;
				logger.addHandler(fh);
			} catch (IOException ioe) {
				String msg = "LOG INIT ERROR: Could not open file.\n"+ioe.getMessage();
				System.err.println(msg); // In case logging is not yet enabled
				logger.log(Level.SEVERE,msg);
			}
		}
	}
	
	{	
		String levelName = Base.preferences.get("replicatorg.debuglevel", Level.INFO.getName());
		Level l = Level.parse(levelName);
		logger.setLevel(l);

		String logPath = Base.preferences.get("replicatorg.logpath", "");
		setLogFile(logPath);
	}
	/**
	 * Path of filename opened on the command line, or via the MRJ open document
	 * handler.
	 */
	static public String openedAtStartup;

	static public void resetPreferences() {
		try {
			Base.preferences.removeNode();
			Base.preferences.flush();
			preferences = Preferences.userNodeForPackage(Base.class);
		} catch (BackingStoreException bse) {
			bse.printStackTrace();
		}
	}
	
	static public String getToolsPath() {
	    String toolsDir = System.getProperty("replicatorg.toolpath");
	    if (toolsDir == null || (toolsDir.length() == 0)) {
		    String path = System.getProperty("user.dir");
	    	toolsDir = path + File.separator + "tools";
	    }
	    return toolsDir;
	}
	
	static public File getUserDirectory() {
		File dir = new File(System.getProperty("user.home")+File.separator+".replicatorg");
		if (!dir.exists()) {
			dir.mkdirs();
		}
		return dir;
	}
	
	static public File getApplicationDirectory() {
		return new File(System.getProperty("user.dir"));
	}
	
	static public File getApplicationFile(String path) {
		return new File(getApplicationDirectory(),path);
	}

	static public File getUserFile(String path) {
		return getUserFile(path,true);
	}

	/** Local storage for localized NumberFormat. */
	static private NumberFormat localNF = NumberFormat.getInstance();
	{
		localNF.setMinimumFractionDigits(2);
	}
	
	/**
	 * Get the NumberFormat object used for parsing and displaying numbers in the localized
	 * format. This should be used for all non-GCode input and output.
	 */
	static public NumberFormat getLocalFormat() {
		return localNF;
	}
	
	/** Local storage for gcode NumberFormat. */
	static private NumberFormat gcodeNF;
	{
		// We don't use DFS.getInstance here to maintain compatibility with Java 5
        DecimalFormatSymbols dfs;
 	 	gcodeNF = new DecimalFormat("#0.0");
 	 	dfs = ((DecimalFormat)gcodeNF).getDecimalFormatSymbols();
 	 	dfs.setDecimalSeparator('.');
	}
	
	/**
	 * Get the NumberFormat object used for GCode generation and parsing. All gcode should be
	 * interpreted and generated by using this format object.
	 */
	static public NumberFormat getGcodeFormat() {
		return gcodeNF;
	}
	
	/**
	 * 
	 * @param path The relative path to the file in the .replicatorG directory
	 * @param autoCopy If true, copy over the file of the same name in the application directory if none is found in the prefs directory.
	 * @return
	 */
	static public File getUserFile(String path, boolean autoCopy) {
		if (path.contains("..")) {
			Base.logger.info("Attempted to access parent directory in "+path+", skipping");
			return null;
		}
		// First look in the user's local .replicatorG directory for the path.
		File f = new File(getUserDirectory(),path);
		// Make the parent file if not already there
		File dir = f.getParentFile();
		if (!dir.exists()) { dir.mkdirs(); }
		if (autoCopy && !f.exists()) {
			// Check if there's an application-level version
			File original = getApplicationFile(path);
			// If so, copy it over
			if (original.exists()) {
				try {
					Base.copyFile(original,f);
				} catch (IOException ioe) {
					Base.logger.log(Level.SEVERE,"Couldn't copy "+path+" to your local .replicatorG directory",f);
				}
			}
		}
		return f;
	}

	static public Font getFontPref(String name, String defaultValue) {
		String s = preferences.get(name,defaultValue);
		StringTokenizer st = new StringTokenizer(s, ",");
		String fontname = st.nextToken();
		String fontstyle = st.nextToken();
		return new Font(fontname,
				((fontstyle.indexOf("bold") != -1) ? Font.BOLD : 0)
						| ((fontstyle.indexOf("italic") != -1) ? Font.ITALIC
								: 0), Integer.parseInt(st.nextToken()));
	}
	
	static public Color getColorPref(String name,String defaultValue) {
		String s = preferences.get(name, defaultValue);
		Color parsed = null;
		if ((s != null) && (s.indexOf("#") == 0)) {
			try {
				int v = Integer.parseInt(s.substring(1), 16);
				parsed = new Color(v);
			} catch (Exception e) {
			}
		}
		// if (parsed == null) return otherwise;
		return parsed;
	}

	/**
	 * The main UI window.
	 */
	static MainWindow editor = null;
	
	public static MainWindow getEditor() {
		return editor;
	}
	private static NotificationHandler notificationHandler;

	private static final String[] supportedExtensions = {
			"gcode", "ngc",
			"stl", "dae", "obj"
	};
	
	/**
	 * Return the extension of a path, converted to lowercase.
	 * @param path The path to check.
	 * @return The extension suffix, sans ".".
	 */
	public static String getExtension(String path) {
		String[] split = path.split("\\.");
		return split[split.length-1];
	}
	
	public static boolean supportedExtension(String path) {
		String suffix = getExtension(path);
		for (final String s : supportedExtensions) {
			if (s.equals(suffix)) { return true; }
		}
		return false;
	}
	

	static public void main(String args[]) {

		// make sure that this is running on java 1.5 or better.
		if (Base.javaVersion < 1.5f) {
			Base.quitWithError("Need to install Java 1.5",
					"This version of ReplicatorG requires\n"
							+ "Java 1.5 or later to run properly.\n"
							+ "Please visit java.com to upgrade.", null);
		}

		if (Base.isMacOS()) {
	         // Default to sun's XML parser, PLEASE.  Some apps are installing some janky-ass xerces.
	         System.setProperty("javax.xml.parsers.DocumentBuilderFactory",
	        		 "com.sun.org.apache.xerces.internal.jaxp.DocumentBuilderFactoryImpl");
		 System.setProperty("com.apple.mrj.application.apple.menu.about.name",
				    "ReplicatorG");
		}
		
		// parse command line input
		for (int i=0;i<args.length;i++) {
			// grab any opened file from the command line
			if (supportedExtension(args[i])) {
				Base.openedAtStartup = args[i];
			}
			
			// Allow for [--debug] [DEBUGLEVEL]
			if(args[i].equals("--debug")) {
				int debugLevelArg = 2;
				if((i+1) < args.length) {
					try {
						debugLevelArg = Integer.parseInt(args[i+1]);
					} catch (NumberFormatException e) {};
				}
				if(debugLevelArg == 0) {
					logger.setLevel(Level.INFO);
					logger.info("Debug level is 'INFO'");
				} else if(debugLevelArg == 1) {
					logger.setLevel(Level.FINE);
					logger.info("Debug level is 'FINE'");
				} else if(debugLevelArg == 2) {
					logger.setLevel(Level.FINER);
					logger.info("Debug level is 'FINER'");
				} else if(debugLevelArg == 3) {
					logger.setLevel(Level.FINEST);
					logger.info("Debug level is 'FINEST'");
				} else if(debugLevelArg >= 4) {
					logger.setLevel(Level.ALL);
					logger.info("Debug level is 'ALL'");
				}
			} else if(args[i].startsWith("-")){
				System.out.println("Usage: ./replicatorg [[--debug] [DEBUGLEVEL]] [filename.stl]");
				System.exit(1);
			}
		}
		
		// Warn about read-only directories
    	{
    		File userDir = getUserDirectory();
    		String header = null;
    		if (!userDir.exists()) header = new String("Unable to create user directory");
    		else if (!userDir.canWrite()) header = new String("Unable to write to user directory");
    		else if (!userDir.isDirectory()) header = new String("User directory must be a directory");
    		if (header != null) {
    			Base.showMessage(header, 
    					"<html><body>ReplicatorG can not write to the directory "+userDir.getAbsolutePath()+".<br>" +
    					"Some functions of ReplicatorG, like toolpath generation and firmware updates,<br>" +
    					"require ReplicatorG to write data to this directory.  You should end this<br>"+
    					"session, change the permissions on this directory, and start again."
    					);
    		}
    	}

		// Use the default system proxy settings
		System.setProperty("java.net.useSystemProxies", "true");
    	// Use antialiasing implicitly
		System.setProperty("j3d.implicitAntialiasing", "true");
		
		// Start the firmware check thread.
		FirmwareUploader.checkFirmware();
		
		// MAC OS X ONLY:
		// register a temporary/early version of the mrj open document handler,
		// because the event may be lost (sometimes, not always) by the time
		// that MainWindow is properly constructed.
		MRJOpenDocumentHandler startupOpen = new MRJOpenDocumentHandler() {
			public void handleOpenFile(File file) {
				// this will only get set once.. later will be handled
				// by the MainWindow version of this fella
				if (Base.openedAtStartup == null) {
					Base.openedAtStartup = file.getAbsolutePath();
				}
			}
		};
		MRJApplicationUtils.registerOpenDocumentHandler(startupOpen);

		// Create the new application "Base" class.
		new Base();
	}

	public Base() {
		// set the look and feel before opening the window
		try {
			if (Base.isMacOS()) {
		         // Only override the UI's necessary for ColorChooser and
		         // FileChooser:
		         Set<Object> includes = new HashSet<Object>();
		         includes.add("ColorChooser");
		         includes.add("FileChooser");
		         includes.add("Component");
		         includes.add("Browser");
		         includes.add("Tree");
		         includes.add("SplitPane");
		         QuaquaManager.setIncludedUIs(includes);

		         // set the Quaqua Look and Feel in the UIManager
		         UIManager.setLookAndFeel("ch.randelshofer.quaqua.QuaquaLookAndFeel");
		         System.setProperty("apple.laf.useScreenMenuBar", "true");

			} else if (Base.isLinux()) {
				// For 0120, trying out the gtk+ look and feel as the default.
				// This is available in Java 1.4.2 and later, and it can't
				// possibly
				// be any worse than Metal. (Ocean might also work, but that's
				// for
				// Java 1.5, and we aren't going there yet)
				UIManager
						.setLookAndFeel("com.sun.java.swing.plaf.gtk.GTKLookAndFeel");

			} else {
				UIManager.setLookAndFeel(UIManager
						.getSystemLookAndFeelClassName());
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		// use native popups so they don't look so crappy on osx
		JPopupMenu.setDefaultLightWeightPopupEnabled(false);
		
		SwingUtilities.invokeLater(new Runnable() {
//		    private TrayIcon trayIcon;

			public void run() {
				// build the editor object
				editor = new MainWindow();
				
				notificationHandler = NotificationHandler.Factory.getHandler(editor, Base.preferences.getBoolean("ui.preferSystemTrayNotifications", false));

				// Get sizing preferences. This is an issue of contention; let's look at how
				// other programs decide how to size themselves.
				editor.restorePreferences();
				// add shutdown hook to store preferences
				Runtime.getRuntime().addShutdownHook(new Thread("Shutdown Hook") {
					final private MainWindow w = editor; 
					public void run() {
						w.onShutdown();
					}
				});
				
				boolean autoconnect = Base.preferences.getBoolean("replicatorg.autoconnect",true);
				String machineName = preferences.get("machine.name",null);
				
				editor.loadMachine(machineName, autoconnect);
				
				// show the window
				editor.setVisible(true);
				UpdateChecker.checkLatestVersion(editor);
		    }
		});

		if (logger.isLoggable(Level.FINE)) {
			logger.fine("OS: " + System.getProperty("os.name") + " " + System.getProperty("os.version") + " (" + System.getProperty("os.arch") + ")");
			logger.fine("JVM: " + System.getProperty("java.version") + " " + System.getProperty("java.vm.name") + " (" + System.getProperty("java.vm.version") + " " + System.getProperty("java.vendor") + ")");
		}
	}

	public enum Platform {
		WINDOWS, MACOS9, MACOSX, LINUX, OTHER
	}

	/**
	 * Full name of the Java version (i.e. 1.5.0_11). Prior to 0125, this was
	 * only the first three digits.
	 */
	public static final String javaVersionName = System.getProperty("java.version");

	/**
	 * Version of Java that's in use, whether 1.1 or 1.3 or whatever, stored as
	 * a float.
	 * <P>
	 * Note that because this is stored as a float, the values may not be <EM>exactly</EM>
	 * 1.3 or 1.4. Instead, make sure you're comparing against 1.3f or 1.4f,
	 * which will have the same amount of error (i.e. 1.40000001). This could
	 * just be a double, but since Processing only uses floats, it's safer for
	 * this to be a float because there's no good way to specify a double with
	 * the preproc.
	 */
	public static final float javaVersion = new Float(javaVersionName.substring(0, 3)).floatValue();
	/**
	 * Current platform in use
	 */
	static public Platform platform;

	/**
	 * Current platform in use.
	 * <P>
	 * Equivalent to System.getProperty("os.name"), just used internally.
	 */
	static public String platformName = System.getProperty("os.name");

	static {
		// figure out which operating system
		// this has to be first, since editor needs to know

		if (platformName.toLowerCase().indexOf("mac") != -1) {
			// can only check this property if running on a mac
			// on a pc it throws a security exception and kills the applet
			// (but on the mac it does just fine)
			if (System.getProperty("mrj.version") != null) { // running on a
																// mac
				platform = (platformName.equals("Mac OS X")) ? Platform.MACOSX : Platform.MACOS9;
			}

		} else {
			String osname = System.getProperty("os.name");

			if (osname.indexOf("Windows") != -1) {
				platform = Platform.WINDOWS;

			} else if (osname.equals("Linux")) { // true for the ibm vm
				platform = Platform.LINUX;

			} else {
				platform = Platform.OTHER;
			}
		}
	}

	// .................................................................

	/**
	 * returns true if the ReplicatorG is running on a Mac OS machine,
	 * specifically a Mac OS X machine because it doesn't run on OS 9 anymore.
	 */
	static public boolean isMacOS() {
		return platform == Platform.MACOSX;
	}

	/**
	 * returns true if running on windows.
	 */
	static public boolean isWindows() {
		return platform == Platform.WINDOWS;
	}

	/**
	 * true if running on linux.
	 */
	static public boolean isLinux() {
		return platform == Platform.LINUX;
	}

	/**
	 * Registers key events for a Ctrl-W and ESC with an ActionListener that
	 * will take care of disposing the window.
	 */
	static public void registerWindowCloseKeys(JRootPane root, // Window
																// window,
			ActionListener disposer) {
		/*
		 * JRootPane root = null; if (window instanceof JFrame) { root =
		 * ((JFrame)window).getRootPane(); } else if (window instanceof JDialog) {
		 * root = ((JDialog)window).getRootPane(); }
		 */

		KeyStroke stroke = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
		root.registerKeyboardAction(disposer, stroke,
				JComponent.WHEN_IN_FOCUSED_WINDOW);

		int modifiers = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();
		stroke = KeyStroke.getKeyStroke('W', modifiers);
		root.registerKeyboardAction(disposer, stroke,
				JComponent.WHEN_IN_FOCUSED_WINDOW);
	}


	/**
	 * Implements the cross-platform headache of opening URLs TODO This code
	 * should be replaced by PApplet.link(), however that's not a static method
	 * (because it requires an AppletContext when used as an applet), so it's
	 * mildly trickier than just removing this method.
	 */
	static public void openURL(String url) {
		// System.out.println("opening url " + url);
		try {
			if (Base.isWindows()) {
				// this is not guaranteed to work, because who knows if the
				// path will always be c:\progra~1 et al. also if the user has
				// a different browser set as their default (which would
				// include me) it'd be annoying to be dropped into ie.
				// Runtime.getRuntime().exec("c:\\progra~1\\intern~1\\iexplore "
				// + currentDir

				// the following uses a shell execute to launch the .html file
				// note that under cygwin, the .html files have to be chmodded
				// +x
				// after they're unpacked from the zip file. i don't know why,
				// and don't understand what this does in terms of windows
				// permissions. without the chmod, the command prompt says
				// "Access is denied" in both cygwin and the "dos" prompt.
				// Runtime.getRuntime().exec("cmd /c " + currentDir +
				// "\\reference\\" +
				// referenceFile + ".html");
				if (url.startsWith("http://")) {
					// open dos prompt, give it 'start' command, which will
					// open the url properly. start by itself won't work since
					// it appears to need cmd
					Runtime.getRuntime().exec("cmd /c start " + url);
				} else {
					// just launching the .html file via the shell works
					// but make sure to chmod +x the .html files first
					// also place quotes around it in case there's a space
					// in the user.dir part of the url
					Runtime.getRuntime().exec("cmd /c \"" + url + "\"");
				}

			} else if (Base.isMacOS()) {
				// com.apple.eio.FileManager.openURL(url);

				if (!url.startsWith("http://")) {
					// prepend file:// on this guy since it's a file
					url = "file://" + url;

					// replace spaces with %20 for the file url
					// otherwise the mac doesn't like to open it
					// can't just use URLEncoder, since that makes slashes into
					// %2F characters, which is no good. some might say
					// "useless"
					if (url.indexOf(' ') != -1) {
						StringBuffer sb = new StringBuffer();
						char c[] = url.toCharArray();
						for (int i = 0; i < c.length; i++) {
							if (c[i] == ' ') {
								sb.append("%20");
							} else {
								sb.append(c[i]);
							}
						}
						url = sb.toString();
					}
				}
				com.apple.mrj.MRJFileUtils.openURL(url);

			} else if (Base.isLinux()) {
				String launcher = preferences.get("launcher.linux","gnome-open");
				if (launcher != null) {
					Runtime.getRuntime().exec(new String[] { launcher, url });
				}
			} else {
				String launcher = preferences.get("launcher",null);
				if (launcher != null) {
					Runtime.getRuntime().exec(new String[] { launcher, url });
				} else {
					Base.logger.warning("Unspecified platform, no launcher available.");
				}
			}

		} catch (IOException e) {
			Base.showWarning("Could not open URL",
					"An error occurred while trying to open\n" + url, e);
		}
	}

	static public boolean openFolderAvailable() {
		if (Base.isWindows() || Base.isMacOS())
			return true;

		if (Base.isLinux()) {
			// Assume that this is set to something valid
			if (preferences.get("launcher.linux",null) != null) {
				return true;
			}

			// Attempt to use gnome-open
			try {
				Process p = Runtime.getRuntime().exec(
						new String[] { "gnome-open" });
				p.waitFor();
				// Not installed will throw an IOException (JDK 1.4.2, Ubuntu
				// 7.04)
				preferences.put("launcher.linux", "gnome-open");
				return true;
			} catch (Exception e) {
			}

			// Attempt with kde-open
			try {
				Process p = Runtime.getRuntime().exec(
						new String[] { "kde-open" });
				p.waitFor();
				preferences.put("launcher.linux", "kde-open");
				return true;
			} catch (Exception e) {
			}
		}
		return false;
	}

	/**
	 * Implements the other cross-platform headache of opening a folder in the
	 * machine's native file browser.
	 */
	static public void openFolder(File file) {
		try {
			String folder = file.getAbsolutePath();

			if (Base.isWindows()) {
				// doesn't work
				// Runtime.getRuntime().exec("cmd /c \"" + folder + "\"");

				// works fine on winxp, prolly win2k as well
				Runtime.getRuntime().exec("explorer \"" + folder + "\"");

				// not tested
				// Runtime.getRuntime().exec("start explorer \"" + folder +
				// "\"");

			} else if (Base.isMacOS()) {
				openURL(folder); // handles char replacement, etc

			} else if (Base.isLinux()) {
				String launcher = preferences.get("launcher.linux",null);
				if (launcher != null) {
					Runtime.getRuntime()
							.exec(new String[] { launcher, folder });
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * "No cookie for you" type messages. Nothing fatal or all that much of a
	 * bummer, but something to notify the user about.
	 */
	static public void showMessage(String title, String message) {
		notificationHandler.showMessage(title,message);
	}

	/**
	 * Non-fatal error message with optional stack trace side dish.
	 */
	static public void showWarning(String title, String message, Exception e) {

		notificationHandler.showWarning(title, message, e);
		
		if (e != null)
			e.printStackTrace();
	}

	/**
	 * Show an error message that's actually fatal to the program. This is an
	 * error that can't be recovered. Use showWarning() for errors that allow 
	 * ReplicatorG to continue running.
	 */
	static public void quitWithError(String title, String message, Throwable e) {

		notificationHandler.showError(title, message, e);
		
		if (e != null)
			e.printStackTrace();
		System.exit(1);
	}

	static public String getContents(String what) {
		String basePath = System.getProperty("user.dir");
		return basePath + File.separator + what;
	}

	static public String getLibContents(String what) {
		/*
		 * On MacOSX, the replicatorg.app-resources property points to the
		 * resources directory inside the app bundle. On other platforms it's
		 * not set.
		 */
		String appResources = System.getProperty("replicatorg.app-resources");
		if (appResources != null) {
			return appResources + File.separator + what;
		} else {
			return getContents("lib" + File.separator + what);
		}
	}

	/**
	 * We need to load animated .gifs through this mechanism vs. getImage due to
	 * a number of bugs in Java's image loading routines.
	 * @param name The path of the image
	 * @param who The component that will use the image
	 * @return the loaded image object
	 */
	static public Image getDirectImage(String name, Component who)  {
		Image image = null;

		// try to get the URL as a system resource
	    URL url = ClassLoader.getSystemResource(name);
	    try {
	    	image = Toolkit.getDefaultToolkit().createImage(url);
	    	MediaTracker tracker = new MediaTracker(who);
	    	tracker.addImage(image, 0);
			tracker.waitForAll();
		} catch (InterruptedException e) {
		}
		return image;
	}

	static public BufferedImage getImage(String name, Component who) {
		BufferedImage image = null;

		// try to get the URL as a system resource
	    URL url = ClassLoader.getSystemResource(name);
	    try {
	    	image = ImageIO.read(url);
	    	MediaTracker tracker = new MediaTracker(who);
	    	tracker.addImage(image, 0);
			tracker.waitForAll();
			BufferedImage img2 = new BufferedImage(image.getWidth(null),
					image.getHeight(null),
					BufferedImage.TYPE_INT_ARGB);
			img2.getGraphics().drawImage(image,0,0,null);
			image = img2;
		} catch (InterruptedException e) {
		} catch (IOException ioe) {
		} catch (IllegalArgumentException iae) {
		}
		return image;
	}

	static public InputStream getStream(String filename) throws IOException {
		return new FileInputStream(getLibContents(filename));
	}

	// ...................................................................

	static public void copyFile(File afile, File bfile) throws IOException {
		InputStream from = new BufferedInputStream(new FileInputStream(afile));
		OutputStream to = new BufferedOutputStream(new FileOutputStream(bfile));
		byte[] buffer = new byte[16 * 1024];
		int bytesRead;
		while ((bytesRead = from.read(buffer)) != -1) {
			to.write(buffer, 0, bytesRead);
		}
		to.flush();
		from.close(); // ??
		from = null;
		to.close(); // ??
		to = null;

		bfile.setLastModified(afile.lastModified()); // jdk13+ required
		// } catch (IOException e) {
		// e.printStackTrace();
		// }
	}

	/**
	 * Grab the contents of a file as a string.
	 */
	static public String loadFile(File file) throws IOException {
		Base.logger.info("Load file : "+file.getAbsolutePath());
		// empty code file.. no worries, might be getting filled up later
		if (file.length() == 0)
			return "";

		InputStreamReader isr = new InputStreamReader(new FileInputStream(file));
		BufferedReader reader = new BufferedReader(isr);

		StringBuffer buffer = new StringBuffer();
		String line = null;
		while ((line = reader.readLine()) != null) {
			buffer.append(line);
			buffer.append('\n');
		}
		reader.close();
		return buffer.toString();
	}

	/**
	 * Spew the contents of a String object out to a file.
	 */
	static public void saveFile(String str, File file) throws IOException {
		Base.logger.info("Saving as "+file.getCanonicalPath());

		ByteArrayInputStream bis = new ByteArrayInputStream(str.getBytes());
		InputStreamReader isr = new InputStreamReader(bis);
		BufferedReader reader = new BufferedReader(isr);

		FileWriter fw = new FileWriter(file);
		PrintWriter writer = new PrintWriter(new BufferedWriter(fw));

		String line = null;
		while ((line = reader.readLine()) != null) {
			writer.println(line);
		}
		writer.flush();
		writer.close();
	}

	static public void copyDir(File sourceDir, File targetDir)
			throws IOException {
		targetDir.mkdirs();
		String files[] = sourceDir.list();
		for (int i = 0; i < files.length; i++) {
			if (files[i].equals(".") || files[i].equals(".."))
				continue;
			File source = new File(sourceDir, files[i]);
			File target = new File(targetDir, files[i]);
			if (source.isDirectory()) {
				// target.mkdirs();
				copyDir(source, target);
				target.setLastModified(source.lastModified());
			} else {
				copyFile(source, target);
			}
		}
	}

	/**
	 * Gets a list of all files within the specified folder, and returns a list
	 * of their relative paths. Ignores any files/folders prefixed with a dot.
	 */
	static public String[] listFiles(String path, boolean relative) {
		return listFiles(new File(path), relative);
	}

	static public String[] listFiles(File folder, boolean relative) {
		String path = folder.getAbsolutePath();
		Vector<String> vector = new Vector<String>();
		addToFileList(relative ? (path + File.separator) : "", path, vector);
		String outgoing[] = new String[vector.size()];
		vector.copyInto(outgoing);
		return outgoing;
	}

	static protected void addToFileList(String basePath, String path,
			Vector<String> fileList) {
		File folder = new File(path);
		String list[] = folder.list();
		if (list == null)
			return;

		for (int i = 0; i < list.length; i++) {
			if (list[i].charAt(0) == '.')
				continue;

			File file = new File(path, list[i]);
			String newPath = file.getAbsolutePath();
			if (newPath.startsWith(basePath)) {
				newPath = newPath.substring(basePath.length());
			}
			fileList.add(newPath);
			if (file.isDirectory()) {
				addToFileList(basePath, newPath, fileList);
			}
		}
	}
	
	/** Get a reference to the currently selected machine **/
	static public MachineLoader getMachineLoader() {
		if (machineLoader == null) {
			machineLoader = new MachineLoader();
		}
		return machineLoader;
	}
}
