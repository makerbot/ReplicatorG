package replicatorg.plugin.toolpath.skeinforge;

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

public abstract class SkeinforgeGenerator extends ToolpathGenerator {

	{
		displayName = "Skeinforge";
	}
	
	public boolean configSuccess = false;
	ConfigurationDialog cd;
	String profile = null;
	List <SkeinforgePreference> preferences;
	
	BuildCode output;
	protected final SkeinforgePostProcessor postprocess;
	
	public SkeinforgeGenerator() {
		postprocess = new SkeinforgePostProcessor(this);
	}

	public boolean runSanityChecks() {
		String errors = "";
		
		for (SkeinforgePreference preference : getPreferences()) {
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
		String name = Base.preferences.get("replicatorg.skeinforge.profile", "");
		return name;
	}

	static public void setSelectedProfile(String name) {
		Base.preferences.put("replicatorg.skeinforge.profile", name);
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
		File dir = new File(getSkeinforgeDir(), "prefs");
		getProfilesIn(dir, profiles);
		dir = getUserProfilesDir();
		getProfilesIn(dir, profiles);
		Collections.sort(profiles);
		return profiles;
	}

	
	/**
	 * A SkeinforgeOption instance describes a single preference override to pass to skeinforge.
	 * @author phooky
	 */
	protected static class SkeinforgeOption {
		final String parameter;
		final String module;
		final String preference;
		final String value;
		public SkeinforgeOption(String module, String preference, String value) {
			this.parameter = "--option";
			this.module = module; 
			this.preference = preference; 
			this.value = value;
		}
		public SkeinforgeOption(String parameter) {
			this.parameter = parameter;
			this.module = null;
			this.preference = null;
			this.value = "";
		}
		public String getParameter() {
			return this.parameter;
		}
		public String getArgument() {
			return (this.module != null ? this.module + ":" : "") + (this.preference != null ? this.preference + "=" : "") + this.value;
		}
	}
		
	/**
	 * A SkeinforgePreference describes a user-visible preference that appears in the 
	 * configuration dialog.  SkeinforgePreferences should give a list of options
	 * that will be set at runtime.
	 * @param displayName The human-readable name of the preference.
	 * @param pereferenceName If you wish to cache the last selected value of this option in
	 * the java application preferences, specify it here.
	 * @param defaultState the default state of this preference, to be used if the
	 * preferenceState is not supplied or not set.
	 * @author phooky
	 *
	 */
	protected interface SkeinforgePreference {
		public JComponent getUI();
		public List<SkeinforgeOption> getOptions(String displayName);
		public String valueSanityCheck();
		public String getName();
	}
	
	public static class SkeinforgeChoicePreference implements SkeinforgePreference {
		private Map<String,List<SkeinforgeOption>> optionsMap = new HashMap<String,List<SkeinforgeOption>>();
		private JPanel component;
		private DefaultComboBoxModel model;
		private String chosen;
		private String name;
		
		public SkeinforgeChoicePreference(String name, final String preferenceName, String defaultState, String toolTip) {
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
		
		public void addOption(String name, SkeinforgeOption o) {
			if (!optionsMap.containsKey(name)) {
				model.addElement(name);
				optionsMap.put(name, new LinkedList<SkeinforgeOption>());
				if (name.equals(chosen)) {
					model.setSelectedItem(name);
				}
			}
			List<SkeinforgeOption> list = optionsMap.get(name);
			list.add(o);
		}

		public List<SkeinforgeOption> getOptions(String displayName) {
			if (optionsMap.containsKey(chosen)) {
				List<SkeinforgeOption> l = optionsMap.get(chosen);
				for (SkeinforgeOption o : l) {
					Base.logger.fine(o.getArgument());
				}
				return optionsMap.get(chosen);
			}
			return new LinkedList<SkeinforgeOption>();
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
	
	protected static class SkeinforgeBooleanPreference implements SkeinforgePreference {
		private boolean isSet;
		private JCheckBox component;
		private List<SkeinforgeOption> trueOptions = new LinkedList<SkeinforgeOption>();
		private List<SkeinforgeOption> falseOptions = new LinkedList<SkeinforgeOption>();
		private String name;
		
		public SkeinforgeBooleanPreference(String name, final String preferenceName, boolean defaultState, String toolTip) {
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
		
		public void addTrueOption(SkeinforgeOption o) { trueOptions.add(o); }
		public void addFalseOption(SkeinforgeOption o) { falseOptions.add(o); }
		public void addNegateableOption(SkeinforgeOption o) {
			trueOptions.add(o);
			String negated = o.value.equalsIgnoreCase("true")?"false":"true";
			falseOptions.add(new SkeinforgeOption(o.module,o.preference,negated));
		}

		public List<SkeinforgeOption> getOptions(String displayName) {
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
		
		// First check for Python.
		boolean hasPython = PythonUtils.interactiveCheckVersion(parent,
				name, new PythonUtils.Version(2, 5, 0),
				new PythonUtils.Version(3, 0, 0));
		if (!hasPython) {
			return false;
		}
		boolean hasTkInter = PythonUtils.interactiveCheckTkInter(parent,
				name);
		if (!hasTkInter) {
			return false;
		}
		if(parent != null)
			parent.setName(name);
		cd = new ConfigurationDialog(parent, this);
		cd.setName(name);
		cd.setTitle(name);

		if (Base.preferences.getBoolean("replicatorg.skeinforge.printOMatic.enabled", false)) {
			
			//Figure out if we're looking to do a toolhead swap
			String extruderChoice = Base.preferences.get("replicatorg.skeinforge.printOMatic.toolheadOrientation", "does not exist");
			
			if(extruderChoice.equalsIgnoreCase("right"))
				postprocess.setToolheadTarget(ToolheadAlias.RIGHT);
			else if(extruderChoice.equalsIgnoreCase("left"))
				postprocess.setToolheadTarget(ToolheadAlias.LEFT);
		}
		return true;
	}
	
	public ConfigurationDialog visualConfiguregetCD(Frame parent, int x, int y, String name) {
		// First check for Python.
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
		// First check for Python.
		boolean hasPython = PythonUtils.interactiveCheckVersion(parent,
				"Editing Profiles", new PythonUtils.Version(2, 5, 0),
				new PythonUtils.Version(3, 0, 0));
		if (!hasPython) {
			return;
		}
		boolean hasTkInter = PythonUtils.interactiveCheckTkInter(parent,
				"Editing Profiles");
		if (!hasTkInter) {
			return;
		}
		EditProfileDialog ep = new EditProfileDialog(parent, this);

		double x = parent.getBounds().getCenterX();
		double y = parent.getBounds().getCenterY();
		ep.pack();
		x -= ep.getWidth() / 2.0;
		y -= ep.getHeight() / 2.0;
		
		ep.setLocation((int)x, (int)y);
		ep.setVisible(true);
	}

	abstract public File getDefaultSkeinforgeDir();

	public File getSkeinforgeDir() {
		String skeinforgePath = System
				.getProperty("replicatorg.skeinforge.path");
		if (skeinforgePath == null || (skeinforgePath.length() == 0)) 
			return getDefaultSkeinforgeDir();
		return new File(skeinforgePath);
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
		String[] arguments = { PythonUtils.getPythonPath(), "skeinforge.py",
				"-p", profile.getFullPath() };
		ProcessBuilder pb = new ProcessBuilder(arguments);
		File skeinforgeDir = getSkeinforgeDir();
		pb.directory(skeinforgeDir);
		Process process = null;
		Base.logger.log(Level.FINEST, "Starting Skeinforge process...");
		
		/**
		 * Run the process and wait for it to return. Because of an issue with process.waitfor() failing to
		 * return, we now also do a busy wait with a timeout. The timeout value is loaded from timeout.txt
		 */
		try {
			// force failure if something goes wrong
			int value = 1;
			
			long timeoutValue = Base.preferences.getInt("replicatorg.skeinforge.timeout", -1);
			
			process = pb.start();
			
			//if no timeout set
			if(timeoutValue == -1)
			{
				Base.logger.log(Level.FINEST, "\tRunning SF without a timeout");
				value = process.waitFor();
			}
			else // run for timeoutValue cycles trying to get an exit value from the process
			{
				Base.logger.log(Level.FINEST, "\tRunning SF with a timeout");
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
							"\tSkeinforge has not returned, This may be due to a communication error\n" +
							"between Skeinforge and ReplicatorG. If you are still editing a Skeinforge\n" +
							"profile, ignore this message; any changes you make in the skeinforge window\n" +
							"and save will be used when generating the gcode file.\n\n" +
							"\tAdjusting the \"Skeinforge timeout\" in the preferences window will affect how\n" +
							"long ReplicatorG waits before assuming that Skeinforge has failed, if you\n" +
							"frequently encounter this message you may want to increase the timeout.",
							"SF Timeout", JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE);
				}
			}
			Base.logger.log(Level.FINEST, "Skeinforge process returned");
			if (value != 0) {
				Base.logger.severe("Unrecognized error code returned by Skeinforge.");
			}
			else
			{
				Base.logger.log(Level.FINEST, "Normal Exit on Skeinforge close");
			}
		} catch (IOException ioe) {
			Base.logger.log(Level.SEVERE, "Could not run skeinforge.", ioe);
		} catch (InterruptedException e) {
			// We are most likely shutting down, or the process has been
			// manually aborted.
			// Kill the background process and bail out.
			System.out.println("SkeinforgeGenerator.editProfile() interrupted: " + e);
			if (process != null) {
				process.destroy();
			}
		}
	}


	// renamed to actually reflect what it does, also making way for an internal getPrefs
	abstract public List<SkeinforgePreference> initPreferences();
	
	public List<SkeinforgePreference> getPreferences()
	{
		if(preferences == null)
			preferences = initPreferences();
		return preferences;
	}

	public SkeinforgePostProcessor getPostProcessor() {
		return postprocess;
	}
	
	public BuildCode generateToolpath() {
		String path = model.getPath();

		List<String> arguments = new LinkedList<String>();
		// The -u makes python output unbuffered. Oh joyous day.
		String[] baseArguments = { PythonUtils.getPythonPath(), "-u",
				"skeinforge.py", "-p", profile };
		for (String arg : baseArguments) {
			arguments.add(arg);
		}

		for (SkeinforgePreference preference : getPreferences()) {
			List<SkeinforgeOption> options = preference.getOptions(displayName);
			if (options != null) {
				for (SkeinforgeOption option : options) {
					arguments.add(option.getParameter());
					String arg = option.getArgument();
					if (arg.length() > 0) arguments.add(arg);
				}
			}
		}
		
		arguments.add(path);
		for(String a : arguments) System.out.println(a);
		ProcessBuilder pb = new ProcessBuilder(arguments);
		pb.directory(getSkeinforgeDir());
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
						.severe("Unrecognized error code returned by Skeinforge.");
				// Throw ToolpathGeneratorException
				return null;
			}
		} catch (IOException ioe) {
			Base.logger.log(Level.SEVERE, "Could not run skeinforge.", ioe);
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
			postprocess.runPostProcessing();
		
		return output;
	}
	
	public BuildCode getGeneratedToolpath()
	{
		return output;
	}
}
