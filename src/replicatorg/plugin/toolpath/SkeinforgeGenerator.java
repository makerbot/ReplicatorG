package replicatorg.plugin.toolpath;

import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
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
	boolean useRaft = false;

	// "skein_engines/skeinforge-0006","sf_profiles");
	public SkeinforgeGenerator() {
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

	class ConfigurationDialog extends JDialog {
		final String manageStr = "Manage profiles...";
		final String profilePref = "replicatorg.skeinforge.profilePref";
		JButton editButton = new JButton("Edit...");
		JButton newButton = new JButton("Create...");
		JButton deleteButton = new JButton("Delete");
		JButton ok = new JButton("Ok");

		private void loadList(JList list) {
			list.removeAll();
			List<Profile> profiles = getProfiles();
			DefaultListModel model = new DefaultListModel();
			String selected = SkeinforgeGenerator.getSelectedProfile();
			Profile selectedprofile = null;
			for (Profile p : profiles) {
				model.addElement(p);
				if (p.toString().equals(selected)) selectedprofile = p;
			}
			list.setModel(model);
			if (selectedprofile != null) {
				list.setSelectedValue(selectedprofile, true);
			}
			else {
				list.clearSelection();
			}
		}

		public ConfigurationDialog(final Frame parent) {
			super(parent, true);
			setTitle("Choose a skeinforge profile");
			setLayout(new MigLayout("aligny top"));

			// have to set this. Something wrong with the initial use of the
			// ListSelectionListener
			ok.setEnabled(false);
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
					ok.setEnabled(selected);
					editButton.setEnabled(selected);
					deleteButton.setEnabled(selected);
					newButton.setEnabled(selected);
				}
			});
			loadList(prefList);
			add(prefList, "growy");

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

			final String useRaftPref = "replicatorg.skeinforge.useRaft";
			useRaft = Base.preferences.getBoolean(useRaftPref, false);
			final JCheckBox raftSelection = new JCheckBox("Use raft", useRaft);
			raftSelection
					.setToolTipText("If this option is checked, skeinforge will lay down a rectangular 'raft' of plastic before starting the build.  "
							+ "Rafts increase the build size slightly, so you should avoid using a raft if your build goes to the edge of the platform.");
			raftSelection.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					useRaft = raftSelection.isSelected();
					Base.preferences.putBoolean(useRaftPref, useRaft);
				}
			});
			add(raftSelection, "wrap");

			add(ok, "tag ok");
			JButton cancel = new JButton("Cancel");
			add(cancel, "tag cancel");
			ok.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent arg0) {
					int idx = prefList.getSelectedIndex();

					Profile p = (Profile) prefList.getModel().getElementAt(idx);
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

	public BuildCode generateToolpath() {
		String path = model.getPath();

		List<String> arguments = new LinkedList<String>();
		// The -u makes python output unbuffered. Oh joyous day.
		String[] baseArguments = { PythonUtils.getPythonPath(), "-u",
				"skeinforge.py", "-p", profile };
		for (String arg : baseArguments) {
			arguments.add(arg);
		}
		if (useRaft) {
			arguments.add("--raft");
		} else {
			arguments.add("--no-raft");
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
