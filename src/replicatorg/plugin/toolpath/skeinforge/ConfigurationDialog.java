package replicatorg.plugin.toolpath.skeinforge;

import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;
import replicatorg.app.Base;
import replicatorg.plugin.toolpath.skeinforge.SkeinforgeGenerator.Profile;
import replicatorg.plugin.toolpath.skeinforge.SkeinforgeGenerator.SkeinforgePreference;

class ConfigurationDialog extends JDialog {
	final boolean postProcessToolheadIndex = true;
	final String profilePref = "replicatorg.skeinforge.profilePref";
	
	JButton generateButton = new JButton("Generate Gcode");
	JButton cancelButton = new JButton("Cancel");
	
	/* these must be explicitly nulled at close because of a java bug:
	 * http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6497929
	 * 
	 * because JDialogs may never be garbage collected, anything they keep reference to 
	 * may never be gc'd. By explicitly nulling these in the setVisible() function
	 * we allow them to be removed.
	 */
	private SkeinforgeGenerator parentGenerator = null;
	private List<Profile> profiles = null;
	
	JPanel profilePanel = new JPanel();
	
	/**
	 * Fills a combo box with a list of skeinforge profiles
	 * @param comboBox to fill with list of skeinforge profiles
	 */
	private void loadList(JComboBox comboBox) {
		comboBox.removeAllItems();
		profiles = parentGenerator.getProfiles();
		DefaultComboBoxModel model = new DefaultComboBoxModel();
		int i=0;
		int foundLastProfile = -1;
		for (Profile p : profiles) {
			model.addElement(p.toString());
			if(p.toString().equals(Base.preferences.get("lastGeneratorProfileSelected","---")))
			{
				Base.logger.fine("Selecting last used element: " + p);
				foundLastProfile = i;
			}
			i++;
		}
		comboBox.setModel(model);
		if(foundLastProfile != -1) {
			comboBox.setSelectedIndex(foundLastProfile);
		}
	}

	/**
	 * Help reduce effects of miserable memory leak.
	 * see declarations above.
	 */
	@Override
	public void setVisible(boolean b) {
		super.setVisible(b);
		if(!b)
		{
			parentGenerator = null;
			profiles = null;
		}
	}

	final JComboBox prefPulldown = new JComboBox();

	private Profile getListedProfile(int idx) {
		return profiles.get(idx);
	}

	public ConfigurationDialog(final Frame parent, final SkeinforgeGenerator parentGeneratorIn) {
		super(parent, true);
		parentGenerator = parentGeneratorIn;
		setTitle("GCode Generator");
		setLayout(new MigLayout("aligny, top, ins 5, fill"));
		
		add(new JLabel("Base Profile:"), "split 2");
		
		// This is intended to fix a bug where the "Generate Gcode" button doesn't get enabled 
		prefPulldown.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent arg0) {
				generateButton.setEnabled(true);
				generateButton.requestFocusInWindow();
				generateButton.setFocusPainted(true);
			}
		});
		loadList(prefPulldown);
		add(prefPulldown, "wrap, growx");

		for (SkeinforgePreference preference: parentGenerator.preferences) {
			add(preference.getUI(), "wrap");
		}

		final JCheckBox autoGen = new JCheckBox("Automatically generate when building.");
		autoGen.setToolTipText("When building from the model view with this checked " +
				"GCode will automatically be generated, bypassing this dialog.");
		add(autoGen, "wrap");
		autoGen.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent arg0) {
				Base.preferences.putBoolean("build.autoGenerateGcode", autoGen.isSelected());
			}
		});
		autoGen.setSelected(Base.preferences.getBoolean("build.autoGenerateGcode", false));
		
		add(generateButton, "tag ok, split 2");
		add(cancelButton, "tag cancel");
		generateButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				parentGenerator.configSuccess = configureGenerator();
				setVisible(!parentGenerator.configSuccess);
			}
		});
		cancelButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				parentGenerator.configSuccess = false;
				setVisible(false);
			}
		});

	}
	
	/**
	 * Does pre-skeinforge generation tasks
	 */
	protected boolean configureGenerator()
	{
		if(!parentGenerator.runSanityChecks()) {
			return false;
		}
		
		int idx = prefPulldown.getSelectedIndex();
		
		if(idx == -1) {
			return false;
		}
		
		Profile p = getListedProfile(idx);
		Base.preferences.put("lastGeneratorProfileSelected",p.toString());
		parentGenerator.profile = p.getFullPath();
		SkeinforgeGenerator.setSelectedProfile(p.toString());
		return true;
	}
};
