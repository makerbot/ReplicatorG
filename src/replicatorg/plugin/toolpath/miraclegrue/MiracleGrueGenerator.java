package replicatorg.plugin.toolpath.miraclegrue;

import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;
import replicatorg.app.Base;
import replicatorg.app.util.PythonUtils;
import replicatorg.app.util.StreamLoggerThread;
import replicatorg.machine.model.ToolheadAlias;
import replicatorg.model.BuildCode;
import replicatorg.plugin.toolpath.ToolpathGenerator;

public abstract class MiracleGrueGenerator extends ToolpathGenerator {

	{
		displayName = "MiracleGrue";
	}
	
	public boolean configSuccess = false;
	ConfigurationDialog cd;
	String profile = null;
	List <MiracleGruePreference> preferences;
	
	BuildCode output;
	protected final MiracleGruePostProcessor postprocess;
	
	public MiracleGrueGenerator() {
		postprocess = new MiracleGruePostProcessor(this);
	}

	public boolean runSanityChecks() {
		String errors = "";
		
		for (MiracleGruePreference preference : getPreferences()) {
			String error = preference.valueSanityCheck();
			if( error != null) {
				errors += error;
			}
		}
		
		if (errors.equals("")) {
			return true;
		}
		
		int result = JOptionPane.showConfirmDialog(null,
				"The following non-optimal profile settings were detected:\n\n"
				+ errors + "\n\n"
				+ "Press OK to attempt to generate profile anyway, or Cancel to go back and correct the settings.",
				"Profile warnings", JOptionPane.OK_CANCEL_OPTION);
		
		return (result == JOptionPane.OK_OPTION);
	}
	
	static public String getSelectedProfile() {
		String name = Base.preferences.get("replicatorg.slic3r.profile", "");
		return name;
	}

	static public void setSelectedProfile(String name) {
		Base.preferences.put("replicatorg.slic3r.profile", name);
	}

	static class Profile implements Comparable<Profile> {
		private String fullPath;
		private String name;
		// targetMachines is a filter that will allow this profile to only be show for specified machines
		private Set<String> targetMachines = new TreeSet<String>();

		public Profile(String fullPath) {
			this.fullPath = fullPath;
			int idx = fullPath.lastIndexOf(File.separatorChar);
			if (idx >= 0) {
				name = fullPath.substring(idx + 1);
			} else {
				name = fullPath;
			}
			
			File targetsFile = new File(fullPath+File.separator+"targetMachines.csv");
			if(targetsFile.exists()) {
				try {
					BufferedReader bir = new BufferedReader(new FileReader(targetsFile));
					String curline = bir.readLine();
					while (curline != null) {
						targetMachines.addAll(Arrays.asList(curline.split(",")));
						curline = bir.readLine();
					}
					bir.close();
					
					for(String machine : targetMachines)
						machine = machine.trim();
					
				} catch (FileNotFoundException e) {
					Base.logger.log(Level.FINEST, "Didn't find a targetMachines file in " + fullPath, e);
				} catch (IOException e) {
					Base.logger.log(Level.FINEST, "Didn't find a targetMachines file in " + fullPath, e);
				}
			} else {
				
			}
		}

		public String getFullPath() {
			return fullPath;
		}

		public String toString() {
			return name;
		}
		
		public Set<String> getTargetMachines() {
			return targetMachines;
		}

		public int compareTo(Profile o) {
			return name.compareTo(o.name);
		}
	}

	void getProfilesIn(File dir, List<Profile> profiles) {
		if (dir.exists() && dir.isDirectory()) {
			for (String subpath : dir.list()) {
				File subDir = new File(dir, subpath);
				if (subDir.isDirectory()) {
					profiles.add(new Profile(subDir.getAbsolutePath()));
				}
			}
		}
	}

	abstract public File getUserProfilesDir();

	List<Profile> getProfiles() {
		final List<Profile> profiles = new LinkedList<Profile>(); 
		// Get default installed profiles
		File dir = new File(Base.getApplicationFile("slic3r"),"prefs");
		Base.logger.finest("Looking for profiles in: "+ dir.toString());
		getProfilesIn(dir, profiles);
		dir = getUserProfilesDir();
		Base.logger.finest("Looking for profiles in: "+ dir.toString());
		getProfilesIn(dir, profiles);
		Collections.sort(profiles);
		return profiles;
	}

	
	/**
	 * A MiracleGrueOption instance describes a single preference override to pass to MiracleGrue.
	 * @author mrseeker
	 */
	protected static class MiracleGrueOption {
		final String parameter;
		final String module;
		final String preference;
		final String value;
		/*
		public MiracleGrueOption(String module, String preference, String value) {
			this.parameter = "--option";
			this.module = module; 
			this.preference = preference; 
			this.value = value;
		}*/
		public MiracleGrueOption(String parameter) {
			this.parameter = parameter;
			this.module = null;
			this.preference = null;
			this.value = "";
		}
		
		public MiracleGrueOption(String parameter, String value) {
			this.parameter = parameter;
			this.module = null;
			this.preference = null;
			this.value = value;
		}
		public String getParameter() {
			return this.parameter;
		}
		public String getArgument() {
			return (this.module != null ? this.module + ":" : "") + (this.preference != null ? this.preference + "=" : "") + this.value;
		}
	}
		
	/**
	 * A MiracleGruePreference describes a user-visible preference that appears in the 
	 * configuration dialog.  MiracleGruePreferences should give a list of options
	 * that will be set at runtime.
	 * @param displayName The human-readable name of the preference.
	 * @param pereferenceName If you wish to cache the last selected value of this option in
	 * the java application preferences, specify it here.
	 * @param defaultState the default state of this preference, to be used if the
	 * preferenceState is not supplied or not set.
	 * @author mrseeker
	 *
	 */
	protected interface MiracleGruePreference {
		public JComponent getUI();
		public List<MiracleGrueOption> getOptions();
		public String valueSanityCheck();
		public String getName();
	}
	
	public static class MiracleGrueChoicePreference implements MiracleGruePreference {
		private Map<String,List<MiracleGrueOption>> optionsMap = new HashMap<String,List<MiracleGrueOption>>();
		private JPanel component;
		private DefaultComboBoxModel model;
		private String chosen;
		private String name;
		
		public MiracleGrueChoicePreference(String name, final String preferenceName, String defaultState, String toolTip) {
			component = new JPanel(new MigLayout("ins 5"));
			chosen = defaultState;
			if (preferenceName != null) {
				chosen = Base.preferences.get(preferenceName, defaultState);
			}
			model = new DefaultComboBoxModel();
			model.setSelectedItem(chosen);
			this.name = name;
			component.add(new JLabel(name));
			JComboBox cb = new JComboBox(model);
			component.add(cb);
			cb.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					chosen = (String)model.getSelectedItem();
					if (preferenceName != null) {
						Base.preferences.put(preferenceName,chosen);
					}
				}
			});
			if (toolTip != null) {
				component.setToolTipText(toolTip);
			}
		}
		public JComponent getUI() { return component; }
		
		public void addOption(String name, MiracleGrueOption o) {
			if (!optionsMap.containsKey(name)) {
				model.addElement(name);
				optionsMap.put(name, new LinkedList<MiracleGrueOption>());
				if (name.equals(chosen)) {
					model.setSelectedItem(name);
				}
			}
			List<MiracleGrueOption> list = optionsMap.get(name);
			list.add(o);
		}

		public List<MiracleGrueOption> getOptions() {
			if (optionsMap.containsKey(chosen)) {
				List<MiracleGrueOption> l = optionsMap.get(chosen);
				for (MiracleGrueOption o : l) {
					Base.logger.fine(o.getArgument());
				}
				return optionsMap.get(chosen);
			}
			return new LinkedList<MiracleGrueOption>();
		}
		@Override
		public String valueSanityCheck() {
			return null;
		}

		@Override
		public String getName() {
			return name;
		}
		
	}
	
	protected static class MiracleGrueBooleanPreference implements MiracleGruePreference {
		private boolean isSet;
		private JCheckBox component;
		private List<MiracleGrueOption> trueOptions = new LinkedList<MiracleGrueOption>();
		private List<MiracleGrueOption> falseOptions = new LinkedList<MiracleGrueOption>();
		private String name;
		
		public MiracleGrueBooleanPreference(String name, final String preferenceName, boolean defaultState, String toolTip) {
			isSet = defaultState;
			if (preferenceName != null) {
				isSet = Base.preferences.getBoolean(preferenceName, defaultState);
			}
			this.name = name;
			component = new JCheckBox(name, isSet);
			component.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					isSet = component.isSelected();
					if (preferenceName != null) {
						Base.preferences.putBoolean(preferenceName,isSet);
					}
				}
			});
			if (toolTip != null) {
				component.setToolTipText(toolTip);
			}
		}
		
		public JComponent getUI() { return component; }
		
		public void addTrueOption(MiracleGrueOption o) { trueOptions.add(o); }
		public void addFalseOption(MiracleGrueOption o) { falseOptions.add(o); }
		public void addNegateableOption(MiracleGrueOption o) {
			trueOptions.add(o);
			String negated = o.value.equalsIgnoreCase("1")?"0":"1";
			falseOptions.add(new MiracleGrueOption(o.parameter,negated));
		}

		public List<MiracleGrueOption> getOptions() {
			return isSet?trueOptions:falseOptions;
		}

		@Override
		public String valueSanityCheck() {
			return null;
		}
		
		@Override
		public String getName() {
			return name;
		}
	}

	/**
	 * creates gui for configuration
	 * @param parent parent window
	 * @param name tkinter window name
	 * @return true if a new ConfigurationDialog was created and stored to cd, false otherwise
	 */
	public boolean configure(Frame parent, String name)
	{
		if (name == null)
			name = "Generate GCode";
		
		if(parent != null)
			parent.setName(name);
		cd = new ConfigurationDialog(parent, this);
		cd.setName(name);
		cd.setTitle(name);

		if (Base.preferences.getBoolean("replicatorg.slic3r.printOMatic.enabled", false)) {
			
			//Figure out if we're looking to do a toolhead swap
			String extruderChoice = Base.preferences.get("replicatorg.slic3r.printOMatic.toolheadOrientation", "does not exist");
			
			if(extruderChoice.equalsIgnoreCase("right"))
				postprocess.setToolheadTarget(ToolheadAlias.RIGHT);
			else if(extruderChoice.equalsIgnoreCase("left"))
				postprocess.setToolheadTarget(ToolheadAlias.LEFT);
		}
		return true;
	}
	
	public ConfigurationDialog visualConfiguregetCD(Frame parent, int x, int y, String name) {
		parent.setName(name);
		cd = new ConfigurationDialog(parent, this);
		cd.setName(name);
		cd.setTitle(name);
		//cd.setSize(500, 760);
		cd.pack();
		cd.setLocation(x, y);
		cd.setVisible(true);
		emitUpdate("Config Done");
		return cd;
	}
	
	public boolean visualConfigure(Frame parent, int x, int y, String name) {

		//cd.setSize(500, 760);
		configure(parent, name);
		
		if (x == -1 || y == -1) {
			double x2 = parent.getBounds().getCenterX();
			double y2 = parent.getBounds().getCenterY();
			cd.pack();
			x2 -= cd.getWidth() / 2.0;
			y2 -= cd.getHeight() / 2.0;
			x = (int)x2;
			y = (int)y2;
		} else {
			cd.pack();
		}
		
		cd.setLocation(x, y);
		cd.setVisible(true);
		emitUpdate("Config Done");
		return configSuccess;//configSuccess is updated in the configuration dialog
	}
	
	public boolean visualConfigure(Frame parent) {
		return visualConfigure(parent, -1, -1, null);
	}
	
	public boolean nonvisualConfigure()
	{
		configure(null, "");
		configSuccess = cd.configureGenerator();
		emitUpdate("Config Done");
		return configSuccess;
	}

	public void editProfiles(Frame parent) {
		EditProfileDialog ep = new EditProfileDialog(parent, this);

		double x = parent.getBounds().getCenterX();
		double y = parent.getBounds().getCenterY();
		ep.pack();
		x -= ep.getWidth() / 2.0;
		y -= ep.getHeight() / 2.0;
		
		ep.setLocation((int)x, (int)y);
		ep.setVisible(true);
	}

	abstract public File getDefaultMiracleGrueDir();

	public File getMiracleGrueDir() {
		String slic3rPath = System
				.getProperty("replicatorg.slic3r.path");
		if (slic3rPath == null || (slic3rPath.length() == 0)) {
			return getDefaultMiracleGrueDir();
		}
		return new File(slic3rPath);
	}

	public Profile duplicateProfile(Profile originalProfile, String newName) {
		File newProfDir = new File(getUserProfilesDir(),
				newName);
		File oldProfDir = new File(originalProfile.getFullPath());
		try {
			Base.copyDir(oldProfDir, newProfDir);
			Profile newProf = new Profile(newProfDir.getAbsolutePath());
			editProfile(newProf);
			return newProf;
		} catch (IOException ioe) {
			Base.logger.log(Level.SEVERE,
					"Couldn't copy directory", ioe);
		}
		return null;
	}
	
	public void editProfile(Profile profile) {
		ProcessBuilder pb = null;
		if (Base.isWindows())
		{
			String[] arguments = { getMiracleGrueDir()+"\\slic3r.exe",
					"--ignore-nonexistent-config","--load", profile.getFullPath() + "\\config.ini" };
			pb = new ProcessBuilder(arguments);
		}
		else
		{
			String[] arguments = { getMiracleGrueDir()+"/slic3r",
					"--ignore-nonexistent-config","--load", profile.getFullPath() + "/config.ini"};
				pb = new ProcessBuilder(arguments);
		}
		File slic3rDir = getMiracleGrueDir();
		pb.directory(slic3rDir);
		Process process = null;
		Base.logger.log(Level.FINEST, "Starting MiracleGrue process...");
		
		/**
		 * Run the process and wait for it to return. Because of an issue with process.waitfor() failing to
		 * return, we now also do a busy wait with a timeout. The timeout value is loaded from timeout.txt
		 */
		try {
			// force failure if something goes wrong
			int value = 1;
			
			long timeoutValue = Base.preferences.getInt("replicatorg.slic3r.timeout", -1);
			
			process = pb.start();
			
			//if no timeout set
			if(timeoutValue == -1)
			{
				Base.logger.log(Level.FINEST, "\tRunning MiracleGrue without a timeout");
				value = process.waitFor();
			}
			else // run for timeoutValue cycles trying to get an exit value from the process
			{
				Base.logger.log(Level.FINEST, "\tRunning MiracleGrue with a timeout");
				while(timeoutValue > 0)
				{
					Thread.sleep(1000);
					try
					{
						value = process.exitValue(); 
						break;
					}
					catch (IllegalThreadStateException itse)
					{
						timeoutValue--;
					}
				}
				if(timeoutValue == 0)
				{
					JOptionPane.showConfirmDialog(null, 
							"\tMiracleGrue has not returned, This may be due to a communication error\n" +
							"between MiracleGrue and ReplicatorG. If you are still editing a MiracleGrue\n" +
							"profile, ignore this message; any changes you make in the MiracleGrue window\n" +
							"and save will be used when generating the gcode file.\n\n" +
							"\tAdjusting the \"MiracleGrue timeout\" in the preferences window will affect how\n" +
							"long ReplicatorG waits before assuming that MiracleGrue has failed, if you\n" +
							"frequently encounter this message you may want to increase the timeout.",
							"SF Timeout", JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE);
				}
			}
			Base.logger.log(Level.FINEST, "MiracleGrue process returned");
			if (value != 0) {
				Base.logger.severe("Unrecognized error code returned by MiracleGrue.");
			}
			else
			{
				Base.logger.log(Level.FINEST, "Normal Exit on MiracleGrue close");
			}
		} catch (IOException ioe) {
			Base.logger.log(Level.SEVERE, "Could not run MiracleGrue.", ioe);
		} catch (InterruptedException e) {
			// We are most likely shutting down, or the process has been
			// manually aborted.
			// Kill the background process and bail out.
			System.out.println("MiracleGrueGenerator.editProfile() interrupted: " + e);
			if (process != null) {
				process.destroy();
			}
		}
	}


	// renamed to actually reflect what it does, also making way for an internal getPrefs
	abstract public List<MiracleGruePreference> initPreferences();
	
	public List<MiracleGruePreference> getPreferences()
	{
		if(preferences == null)
			preferences = initPreferences();
		return preferences;
	}

	public MiracleGruePostProcessor getPostProcessor() {
		return postprocess;
	}
	
	public BuildCode generateToolpath() {
		String path = model.getPath();

		List<String> arguments = new LinkedList<String>();
		
		String[] baseArguments = {};
		
		if (Base.isWindows())
		{
			baseArguments = new String[]{ getMiracleGrueDir()+"\\slic3r-console.exe",
				"--debug","--ignore-nonexistent-config","--load", profile + "\\config.ini"};
		}
		else
		{
			baseArguments = new String[]{ getMiracleGrueDir()+"/slic3r",
					"--debug","--ignore-nonexistent-config","--load", profile + "/config.ini" };
		}
		for (String arg : baseArguments) {
			arguments.add(arg);
		}
		for (MiracleGruePreference preference : getPreferences()) {
			List<MiracleGrueOption> options = preference.getOptions();
			if (options != null) {
				for (MiracleGrueOption option : options) {
					arguments.add(option.getParameter());
					String arg = option.getArgument();
					if (arg.length() > 0) arguments.add(arg);
				}
			}
		}
		arguments.add(path);
for(String a : arguments) System.out.println(a);
		ProcessBuilder pb = new ProcessBuilder(arguments);
		pb.directory(getMiracleGrueDir());
		Process process = null;
		try {
			process = pb.start();
			StreamLoggerThread ist = new StreamLoggerThread(
					process.getInputStream()) {
				@Override
				protected void logMessage(String line) {
					emitUpdate(line);
					super.logMessage(line);
				}
			};
			StreamLoggerThread est = new StreamLoggerThread(
					process.getErrorStream());
			est.setDefaultLevel(Level.SEVERE);
			ist.setDefaultLevel(Level.FINE);
			ist.start();
			est.start();
			int value = process.waitFor();
			if (value != 0) {
				Base.logger
						.severe("Unrecognized error code returned by MiracleGrue.");
				// Throw ToolpathGeneratorException
				return null;
			}
		} catch (IOException ioe) {
			Base.logger.log(Level.SEVERE, "Could not run MiracleGrue.", ioe);
			// Throw ToolpathGeneratorException
			return null;
		} catch (InterruptedException e) {
			// We are most likely shutting down, or the process has been
			// manually aborted.
			// Kill the background process and bail out.
			if (process != null) {
				process.destroy();
			}
			return null;
		}
		int lastIdx = path.lastIndexOf('.');
		String root = (lastIdx >= 0) ? path.substring(0, lastIdx) : path;
		output = new BuildCode(root, new File(root + ".gcode"));
		
		if(postprocess != null)
		{
			Base.logger.log(Level.FINER, "pre-post-processor");
			postprocess.runPostProcessing();
			Base.logger.log(Level.FINER, "post-post-processor");
		}
		
		return output;
	}
	
	public BuildCode getGeneratedToolpath()
	{
		return output;
	}
}
