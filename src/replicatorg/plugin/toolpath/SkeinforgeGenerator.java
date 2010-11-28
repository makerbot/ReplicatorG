package replicatorg.plugin.toolpath;

import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;

import javax.annotation.Generated;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import net.miginfocom.swing.MigLayout;
import replicatorg.app.Base;
import replicatorg.app.util.PythonUtils;
import replicatorg.app.util.StreamLoggerThread;
import replicatorg.model.BuildCode;

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

	class Profile implements Comparable<Profile> {
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

	abstract File getUserProfilesDir();

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
	protected class SkeinforgeOption {
		final String module;
		final String preference;
		final String value;
		public SkeinforgeOption(String module, String preference, String value) {
			this.module = module; 
			this.preference = preference; 
			this.value = value;
		}
		public String getSpec() {
			return module + ":" + preference + "=" + value;
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
	
	protected class SkeinforgeBooleanPreference implements SkeinforgePreference {
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
	
	class ConfigurationDialog extends JDialog {
		final String manageStr = "Manage profiles...";
		final String profilePref = "replicatorg.skeinforge.profilePref";
		JButton editButton = new JButton("Edit...");
		JButton newButton = new JButton("Clone...");
		JButton deleteButton = new JButton("Delete");
		JButton generate = new JButton("Generate...");

		private void loadList(JList list) {
			list.removeAll();
			List<Profile> profiles = getProfiles();
			DefaultListModel model = new DefaultListModel();
			int i=0;
			int foundLastProfile = -1;
			for (Profile p : profiles) {
				model.addElement(p);
				if(p.toString().equals(Base.preferences.get("lastGeneratorProfileSelected","---")))
				{
					Base.logger.fine("Selecting last used element: " + p);
					foundLastProfile = i;
				}
				i++;
			}
			list.setModel(model);
			list.clearSelection();
			if(foundLastProfile != -1) {
				list.setSelectedIndex(foundLastProfile);	
				generate.setEnabled(true);
				generate.requestFocusInWindow();
				generate.setFocusPainted(true);
			}			
		}

		public ConfigurationDialog(final Frame parent) {
			super(parent, true);
			setTitle("Choose a skeinforge profile");
			setLayout(new MigLayout("aligny top"));

			editButton.setToolTipText("Click to edit this profile's properties.");
			deleteButton.setToolTipText("Click to remove this profile. Note that this can not be undone.");
			newButton.setToolTipText("This will make a copy of the currently selected profile, with a new name that you provide.");
			
			// have to set this. Something wrong with the initial use of the
			// ListSelectionListener
			generate.setEnabled(false);
					
			editButton.setEnabled(false);
			deleteButton.setEnabled(false);
			newButton.setEnabled(false);

			add(new JLabel("Select a printing profile:"), "wrap");

			final JList prefList = new JList();
			prefList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			prefList.addListSelectionListener(new ListSelectionListener() {

				@Override
				public void valueChanged(ListSelectionEvent selectionEvent) {
					boolean selected = !((JList) selectionEvent.getSource())
							.isSelectionEmpty();
					generate.setEnabled(selected);
					editButton.setEnabled(selected);
					deleteButton.setEnabled(selected);
					newButton.setEnabled(selected);
				}
			});
			
			// Add a listener for mouse clicks
			prefList.addMouseListener(new MouseAdapter() {
			    public void mouseClicked(MouseEvent evt) {
			        JList list = (JList)evt.getSource();
			        if (evt.getClickCount() == 2) { // Double-click generates with this profile
			            int idx = list.locationToIndex(evt.getPoint());
						Profile p = (Profile) prefList.getModel().getElementAt(idx);
						Base.preferences.put("lastGeneratorProfileSelected",p.toString());
						configSuccess = true;
						profile = p.getFullPath();
						setVisible(false);
			        }
			    }
			});
			loadList(prefList);
			add(prefList, "growy");

			prefList.addKeyListener( new KeyAdapter() {
				public void keyPressed ( KeyEvent e ) {
					Base.logger.fine("key pressed event: "+e);
					if(e.getKeyCode() == KeyEvent.VK_ENTER)
					{
						int idx = prefList.getSelectedIndex();
						Base.logger.fine("idx="+idx);
						Profile p = (Profile) prefList.getModel().getElementAt(idx);
						Base.preferences.put("lastGeneratorProfileSelected",p.toString());
						configSuccess = true;
						profile = p.getFullPath();
						setVisible(false);
					}
				}
		     }
			);
			
			add(editButton, "split,flowy,growx");
			editButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					int idx = prefList.getSelectedIndex();
					if (idx == -1) {
						JOptionPane.showMessageDialog(parent,
								"Select a profile to edit.");
					} else {
						Profile p = (Profile) prefList.getModel().getElementAt(
								idx);
						editProfile(p);
					}
				}
			});

			add(newButton, "growx,flowy");
			newButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					int idx = prefList.getSelectedIndex();
					String newName = JOptionPane.showInputDialog(parent,
							"Name your new profile:");
					if (newName != null) {
						File newProfDir = new File(getUserProfilesDir(),
								newName);
						Profile p = (Profile) prefList.getModel().getElementAt(
								idx);
						File oldProfDir = new File(p.getFullPath());
						try {
							Base.copyDir(oldProfDir, newProfDir);
							Profile newProf = new Profile(newProfDir
									.getAbsolutePath());
							editProfile(newProf);
							loadList(prefList);
						} catch (IOException ioe) {
							Base.logger.log(Level.SEVERE,
									"Couldn't copy directory", ioe);
						}
					}
				}
			});

			add(deleteButton, "wrap,growx");
			deleteButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					int idx = prefList.getSelectedIndex();
					Profile p = (Profile) prefList.getModel().getElementAt(idx);
					boolean result = new ProfileUtils().delete(p);
					loadList(prefList);
					Base.logger.log(Level.INFO, "Profile " + p.getFullPath()
							+ " deleted: " + result);
				}
			});

			for (SkeinforgePreference preference: preferences) {
				add(preference.getUI(), "wrap");
			}

			add(generate, "tag ok");
			JButton cancel = new JButton("Cancel");
			add(cancel, "tag cancel");
			generate.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent arg0) {
					int idx = prefList.getSelectedIndex();
					Profile p = (Profile) prefList.getModel().getElementAt(idx);
					Base.preferences.put("lastGeneratorProfileSelected",p.toString());
					configSuccess = true;
					profile = p.getFullPath();
					setVisible(false);
					SkeinforgeGenerator.setSelectedProfile(p.toString());
				}
			});
			cancel.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent arg0) {
					configSuccess = false;
					setVisible(false);
				}
			});
		}

	};

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
		ConfigurationDialog cd = new ConfigurationDialog(parent);
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
					arguments.add("--option");
					arguments.add(option.getSpec());
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
