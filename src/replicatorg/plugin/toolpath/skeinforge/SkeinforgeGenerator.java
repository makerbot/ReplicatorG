package replicatorg.plugin.toolpath.skeinforge;

import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;

import net.miginfocom.swing.MigLayout;
import replicatorg.app.Base;
import replicatorg.app.util.PythonUtils;
import replicatorg.app.util.StreamLoggerThread;
import replicatorg.model.BuildCode;
import replicatorg.plugin.toolpath.ToolpathGenerator;

public abstract class SkeinforgeGenerator extends ToolpathGenerator {

	boolean configSuccess = false;
	String profile = null;
	List <SkeinforgePreference> preferences;

	// "skein_engines/skeinforge-0006","sf_profiles");
	public SkeinforgeGenerator() {
		preferences = getPreferences();
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

		public Profile(String fullPath) {
			this.fullPath = fullPath;
			int idx = fullPath.lastIndexOf(File.separatorChar);
			if (idx >= 0) {
				name = fullPath.substring(idx + 1);
			} else {
				name = fullPath;
			}
		}

		public String getFullPath() {
			return fullPath;
		}

		public String toString() {
			return name;
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
	 * @param name The human-readable name of the preference.
	 * @param pereferenceName If you wish to cache the last selected value of this option in
	 * the java application preferences, specify it here.
	 * @param defaultState the default state of this preference, to be used if the
	 * preferenceState is not supplied or not set.
	 * @author phooky
	 *
	 */
	protected interface SkeinforgePreference {
		public JComponent getUI();
		public List<SkeinforgeOption> getOptions();
	}
	
	public static class SkeinforgeChoicePreference implements SkeinforgePreference {
		private Map<String,List<SkeinforgeOption>> optionsMap = new HashMap<String,List<SkeinforgeOption>>();
		private JPanel component;
		private DefaultComboBoxModel model;
		private String chosen;
		
		public SkeinforgeChoicePreference(String name, final String preferenceName, String defaultState, String toolTip) {
			component = new JPanel(new MigLayout());
			chosen = defaultState;
			if (preferenceName != null) {
				chosen = Base.preferences.get(preferenceName, defaultState);
			}
			model = new DefaultComboBoxModel();
			model.setSelectedItem(chosen);
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

		public List<SkeinforgeOption> getOptions() {
			if (optionsMap.containsKey(chosen)) {
				List<SkeinforgeOption> l = optionsMap.get(chosen);
				for (SkeinforgeOption o : l) {
					Base.logger.fine(o.getArgument());
				}
				return optionsMap.get(chosen);
			}
			return new LinkedList<SkeinforgeOption>();
		}

	}
	
	protected static class SkeinforgeBooleanPreference implements SkeinforgePreference {
		private boolean isSet;
		private JCheckBox component;
		private List<SkeinforgeOption> trueOptions = new LinkedList<SkeinforgeOption>();
		private List<SkeinforgeOption> falseOptions = new LinkedList<SkeinforgeOption>();
		
		public SkeinforgeBooleanPreference(String name, final String preferenceName, boolean defaultState, String toolTip) {
			isSet = defaultState;
			if (preferenceName != null) {
				isSet = Base.preferences.getBoolean(preferenceName, defaultState);
			}
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

		public List<SkeinforgeOption> getOptions() {
			return isSet?trueOptions:falseOptions;
		}
	}
	
	
	public static class PrintOMatic implements SkeinforgePreference {
		private JPanel component;
		private JCheckBox enabled;
		private String baseName;
		
		private class StoringActionListener implements ActionListener {
			final String name;
			final JTextField input;
			
			public StoringActionListener(JTextField input, String name) {
				this.input = input;
				this.name = name;
			}

			@Override
			public void actionPerformed(ActionEvent arg0) {
				String value = (String)input.getText();
				
				if (name != null) {
					Base.logger.fine("here: " + name + "=" + value);
					Base.preferences.put(name, value);
				}
			}
		}
		
		private void addParameter(JComponent target, String name, String description, String defaultValue, String toolTip) {
			String fullName = baseName + name;
			String value = null;
			
			if (fullName != null) {
				value = Base.preferences.get(fullName, defaultValue);
				
				// Store it back so that we can be assured that it is set.
				Base.preferences.put(fullName, value);
			}
			target.add(new JLabel(description));
			
			JTextField input = new JTextField(value, 10);
			target.add(input, "wrap");
			
			input.addActionListener(new StoringActionListener(input, fullName));
			if (toolTip != null) {
				// TODO: This is wrong.
				input.setToolTipText(toolTip);
			}
		}
		
		private double getValue(String optionName) {
			// TODO: record the default values somewhere, so that we can retrieve them here!
			String value = Base.preferences.get(baseName + optionName, null);
			
			Base.logger.fine("Getting preference for " + baseName + optionName);
			
			return Double.valueOf(value);
		}
		
		public PrintOMatic() {
			component = new JPanel(new MigLayout());
			
			baseName = "replicatorg.skeinforge.printOMatic.";
			

//			SkeinforgeBooleanPreference printOMaticPref =
//				new SkeinforgeBooleanPreference("Use Print-O-Matic",
//						"replicatorg.skeinforge.printOMaticPref", true,
//						"If this option is checked, skeinforge will use the values below to control the print");
//			printOMaticPref.addNegateableOption(new SkeinforgeOption("raft.csv", "Add Raft, Elevate Nozzle, Orbit and Set Altitude:", "true"));
//			prefs.add(printOMaticPref);

			// Add a checkbox to switch print-o-matic on and off
			final String enabledName = baseName + "enabled";
			enabled = new JCheckBox("Use Print-O-Matic", Base.preferences.getBoolean(enabledName,true));
			enabled.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					if (enabledName != null) {
						Base.preferences.putBoolean(enabledName,enabled.isSelected());
					}
				}
			});
			
			component.add(enabled, "wrap, spanx");
			
			// Make a tabbed pane to sort basic and advanced components 
			JTabbedPane tabbedPane = new JTabbedPane();
			
			JComponent basicPanel = new JPanel(new MigLayout());
			JComponent advancedPanel = new JPanel(new MigLayout());
			 
			addParameter(basicPanel, "desiredFeedrate",
						"Desired Feedrate (mm/s)", "30",
						"slow: 0-20, default: 30, Fast: 40+");
			
			addParameter(basicPanel, "desiredLayerHeight",
						"Desired Layer Height (mm)", "0.35",
						"Set the desired feedrate");

			addParameter(advancedPanel, "filamentDiameter",
					"Filament Diameter (mm)", "2.98",
					"measure feedstock");

			addParameter(advancedPanel, "nozzleDiameter",
					"Nozzle Diameter (mm)", "0.5",
					"exit hole diameter");
			
			addParameter(advancedPanel, "driveGearDiameter",
					"Drive Gear Diameter (mm)", "10.58",
					"measure at teeth");
			
			addParameter(advancedPanel, "driveGearScalingFactor",
					"Gear Diameter Scaling Factor", "0.85",
					"ABS = 0.85, PLA = 1");
			
			addParameter(advancedPanel, "retractedVolumeScalingFactor",
					"Retracted Volume Scaling Factor", "1",
					"Nominally 1");
			
			addParameter(advancedPanel, "modelHasThinFeatures",
					"No thin features", "1",
					"Model does not contain any thin features (<2.5mm) (1=true, 0=false)");
			
			addParameter(advancedPanel, "reversalDistance",
					"Extruder Reversal Distance (mm)", "1.235",
					"input distance");

			addParameter(advancedPanel, "reversalPushBack",
					"Extruder Push Back Distance (mm)", "1.285",
					"input distance (Push back should be slightly longer to overcome nozzle pressure)");

			addParameter(advancedPanel, "reversalSpeed",
					"Reversal Speed (RPM)", "35",
					"35 is default for 3mm, 60 is default for 1.75");

			tabbedPane.addTab("Basic", basicPanel);
			tabbedPane.addTab("Advanced", advancedPanel);
			component.add(tabbedPane);
		}
		
		public JComponent getUI() { return component; }
		
		public List<SkeinforgeOption> getOptions() {
			List<SkeinforgeOption> options = new LinkedList<SkeinforgeOption>();

			if (enabled.isSelected()) {
			
				double flowRate = Math.pow(getValue("nozzleDiameter")/2,2)*Math.PI*getValue("desiredFeedrate")*60/(Math.pow(getValue("filamentDiameter")/2,2)*Math.PI*(getValue("driveGearScalingFactor")*getValue("driveGearDiameter")*Math.PI));
				double perimeterWidthOverThickness =((Math.pow(getValue("nozzleDiameter")/2,2)*Math.PI)/getValue("desiredLayerHeight"))/getValue("desiredLayerHeight");
				double infillWidthOverThickness =((Math.pow(getValue("nozzleDiameter")/2,2)*Math.PI)/getValue("desiredLayerHeight"))/getValue("desiredLayerHeight");
				double feedRate =getValue("desiredFeedrate");
				double layerHeight =getValue("desiredLayerHeight");
				double extraShellsOnAlternatingSolidLayer =getValue("modelHasThinFeatures");
				double extraShellsOnSparseLayer =getValue("modelHasThinFeatures");
				double reversalSpeed =getValue("reversalSpeed");
				double reversalTime =(((getValue("retractedVolumeScalingFactor")*getValue("reversalDistance")*Math.PI*Math.pow(2.88/2,2))/(Math.PI*Math.pow(getValue("filamentDiameter")/2,2)))/(((getValue("driveGearDiameter")*getValue("driveGearScalingFactor")*Math.PI)*getValue("reversalSpeed"))/60))*Math.pow(10,3);
				double pushbackTime =(((getValue("retractedVolumeScalingFactor")*getValue("reversalPushBack")*Math.PI*Math.pow(2.88/2,2))/(Math.PI*Math.pow(getValue("filamentDiameter")/2,2)))/(((getValue("driveGearDiameter")*getValue("driveGearScalingFactor")*Math.PI)*getValue("reversalSpeed"))/60))*Math.pow(10,3);
				
				Base.logger.fine("Print-O-Matic settings:"
						+ "\n flowRate=" + flowRate
						+ "\n perimeterWidthOverThickness=" + perimeterWidthOverThickness
						+ "\n infillWidthOverThickness=" + infillWidthOverThickness
						+ "\n feedRate=" + feedRate
						+ "\n layerHeight=" + layerHeight
						+ "\n extraShellsOnAlternatingSolidLayer=" + extraShellsOnAlternatingSolidLayer
						+ "\n extraShellsOnSparseLayer=" + extraShellsOnSparseLayer
						+ "\n reversalSpeed=" + reversalSpeed
						+ "\n reversalTime=" + reversalTime
						+ "\n pushbackTime=" + pushbackTime
						);
							
				options.add(new SkeinforgeOption("speed.csv", "Flow Rate Setting (float):", Double.toString(flowRate)));
				options.add(new SkeinforgeOption("carve.csv", "Perimeter Width over Thickness (ratio):", Double.toString(perimeterWidthOverThickness)));
				options.add(new SkeinforgeOption("fill.csv", "Infill Width over Thickness (ratio):", Double.toString(infillWidthOverThickness)));
				options.add(new SkeinforgeOption("speed.csv", "Feed Rate (mm/s):", Double.toString(feedRate)));
				options.add(new SkeinforgeOption("carve.csv", "Layer Thickness (mm):", Double.toString(layerHeight)));
				options.add(new SkeinforgeOption("fill.csv", "Extra Shells on Alternating Solid Layer (layers):", Double.toString(extraShellsOnAlternatingSolidLayer)));
				options.add(new SkeinforgeOption("fill.csv", "Extra Shells on Sparse Layer (layers):", Double.toString(extraShellsOnSparseLayer)));
				options.add(new SkeinforgeOption("reversal.csv", "Reversal speed (RPM):", Double.toString(reversalSpeed)));
				options.add(new SkeinforgeOption("reversal.csv", "Reversal time (milliseconds):", Double.toString(reversalTime)));
				options.add(new SkeinforgeOption("reversal.csv", "Push-back time (milliseconds):", Double.toString(pushbackTime)));
			}
				
			return options;
		}
	}

	public static class SkeinforgeValuePreference implements SkeinforgePreference {
		private Map<String,List<SkeinforgeOption>> optionsMap = new HashMap<String,List<SkeinforgeOption>>();
		private JPanel component;
		private JTextField input; 
		private String value;
		
		public SkeinforgeValuePreference(String name, final String preferenceName, String defaultValue, String toolTip) {
			component = new JPanel(new MigLayout());
			if (preferenceName != null) {
				value = Base.preferences.get(preferenceName, defaultValue);
			}
			component.add(new JLabel(name));
			
			input = new JTextField(value, 10);
			component.add(input);
			input.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					value = (String)input.getText();
					if (preferenceName != null) {
						Base.preferences.put(preferenceName,value);
					}
				}
			});
			if (toolTip != null) {
				component.setToolTipText(toolTip);
			}
		}
		public JComponent getUI() { return component; }
	
		public List<SkeinforgeOption> getOptions() {
//			if (optionsMap.containsKey(chosen)) {
//				List<SkeinforgeOption> l = optionsMap.get(chosen);
//				for (SkeinforgeOption o : l) {
//					System.err.println(o.getArgument());
//				}
//				return optionsMap.get(chosen);
//			}
			return new LinkedList<SkeinforgeOption>();
		}
	
	}

	public boolean visualConfigure(Frame parent) {
		// First check for Python.
		boolean hasPython = PythonUtils.interactiveCheckVersion(parent,
				"Generating gcode", new PythonUtils.Version(2, 5, 0),
				new PythonUtils.Version(3, 0, 0));
		if (!hasPython) {
			return false;
		}
		boolean hasTkInter = PythonUtils.interactiveCheckTkInter(parent,
				"Generating gcode");
		if (!hasTkInter) {
			return false;
		}
		ConfigurationDialog cd = new ConfigurationDialog(parent, this);
		double x = parent.getBounds().getCenterX();
		double y = parent.getBounds().getCenterY();
		cd.pack();
		x -= cd.getWidth() / 2.0;
		y -= cd.getHeight() / 2.0;
		cd.setLocation((int) x, (int) y);
		cd.setVisible(true);
		return configSuccess;
	}

	abstract public File getDefaultSkeinforgeDir();

	public File getSkeinforgeDir() {
		String skeinforgePath = System
				.getProperty("replicatorg.skeinforge.path");
		if (skeinforgePath == null || (skeinforgePath.length() == 0)) {
			return getDefaultSkeinforgeDir();
		}
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
		try {
			process = pb.start();
			int value = process.waitFor();
			if (value != 0) {
				Base.logger
						.severe("Unrecognized error code returned by Skeinforge.");
			}
		} catch (IOException ioe) {
			Base.logger.log(Level.SEVERE, "Could not run skeinforge.", ioe);
		} catch (InterruptedException e) {
			// We are most likely shutting down, or the process has been
			// manually aborted.
			// Kill the background process and bail out.
			if (process != null) {
				process.destroy();
			}
		}
	}

	abstract public List<SkeinforgePreference> getPreferences();
	
	public BuildCode generateToolpath() {
		String path = model.getPath();

		List<String> arguments = new LinkedList<String>();
		// The -u makes python output unbuffered. Oh joyous day.
		String[] baseArguments = { PythonUtils.getPythonPath(), "-u",
				"skeinforge.py", "-p", profile };
		for (String arg : baseArguments) {
			arguments.add(arg);
		}
		for (SkeinforgePreference preference : preferences) {
			List<SkeinforgeOption> options = preference.getOptions();
			if (options != null) {
				for (SkeinforgeOption option : options) {
					arguments.add(option.getParameter());
					String arg = option.getArgument();
					if (arg.length() > 0) arguments.add(arg);
				}
			}
		}
		arguments.add(path);

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
		return new BuildCode(root, new File(root + ".gcode"));
	}
}
