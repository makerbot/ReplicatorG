package replicatorg.plugin.toolpath.skeinforge;

import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;
import replicatorg.app.Base;
import replicatorg.plugin.toolpath.skeinforge.SkeinforgeGenerator.Profile;
import replicatorg.plugin.toolpath.skeinforge.SkeinforgeGenerator.SkeinforgePreference;
import replicatorg.plugin.toolpath.skeinforge.PrintOMatic5D;

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
		int selectedProfile = -1;
		for (Profile p : profiles) {
			///we display all profiles for all machines.
			// at MBI customer support's request.
			model.addElement(p.toString());
			
			if(p.toString().equals(Base.preferences.get("lastGeneratorProfileSelected","---")))
			{
				Base.logger.fine("Selecting last used element: " + p);
				/// default select the last profile that matches 
				// the currently selected machine type
				if(ProfileUtils.shouldDisplay(p)) {
					selectedProfile = i;
				}
			}
			i++;
		}
		comboBox.setModel(model);
		if(selectedProfile != -1) {
			comboBox.setSelectedIndex(selectedProfile);
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

	public ConfigurationDialog(final Frame parent, final SkeinforgeGenerator parentGeneratorIn) {
		super(parent, true);

		parentGenerator = parentGeneratorIn;
		setTitle("GCode Generator");
		setLayout(new MigLayout("aligny, top, ins 5, fill"));
		
		add(new JLabel("Slicing Profile:"), "split 2");
		
		// This is intended to fix a bug where the "Generate Gcode"
		// button doesn't get enabled 
		prefPulldown.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent arg0) {
				generateButton.setEnabled(true);
				generateButton.requestFocusInWindow();
				generateButton.setFocusPainted(true);
			}
		});
		
		/// Fills UI with the list of Skeinforge settings/options
		loadList(prefPulldown); 
		add(prefPulldown, "wrap, growx, gapbottom 10");

		for (SkeinforgePreference preference: parentGenerator.getPreferences()) {
			add(preference.getUI(), "growx, wrap");
		}
		
		generateButton.setToolTipText("Generates GCode instructions for your machine.");
		
		add(generateButton, "tag ok, split 2");
		add(cancelButton, "tag cancel");

		generateButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0)
			{
				String genname = parentGenerator.displayName;
				System.out.println("\n##" + genname + "##");
				if(genname.equals("Skeinforge (35) - Legacy"))
				{

					PrintOMatic pom = new PrintOMatic();

					System.out.println("\n**sf35**\n" + "desired:" + pom.getValue("desiredFeedrate") +
						"travel:" + pom.getValue("travelFeedrate"));

					if((pom.getValue("desiredFeedrate") > 40) || (pom.getValue("travelFeedrate") > 55))
					{
						JOptionPane.showMessageDialog(parent,"You are now slicing with accelerated build speeds.\n" +
              "Do not print files generated at these speeds unless you have acceleration turned on.\n" +
              "Building high speed files with acceleration turned off can harm your Makerbot.\n\n" +
							"You can turn acceleration on in the Onboard Preferences menu or via your Makerbot's onboard menus", "Acceleration Warning", JOptionPane.WARNING_MESSAGE);
					}
				}
				else if(genname.equals("Skeinforge (47) - Legacy") || genname.equals("Skeinforge (50)"))
				{

					PrintOMatic5D pom5d = new PrintOMatic5D();
					System.out.println("\n**sf47/50**\n" + "desired:" + pom5d.getValue("desiredFeedrate") +
						"travel:" + pom5d.getValue("travelFeedrate"));
					if((pom5d.getValue("desiredFeedrate") > 40) || (pom5d.getValue("travelFeedrate") > 55))
					{
						JOptionPane.showMessageDialog(parent,"You are now slicing with accelerated build speeds.\n" +
              "Do not print files generated at these speeds unless you have acceleration turned on.\n" +
              "Building high speed files with acceleration turned off can harm your Makerbot.\n\n" +
							"You can turn acceleration on in the Onboard Preferences menu or via your Makerbot's onboard menus", "Acceleration Warning", JOptionPane.WARNING_MESSAGE);
					}
				}			
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
		
		Profile p = ProfileUtils.getListedProfile(
				prefPulldown.getModel(), profiles, idx);
		Base.preferences.put("lastGeneratorProfileSelected",p.toString());
		parentGenerator.profile = p.getFullPath();
		SkeinforgeGenerator.setSelectedProfile(p.toString());
		return true;
	}
};
