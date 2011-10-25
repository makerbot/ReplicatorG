package replicatorg.plugin.toolpath.skeinforge;

import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.logging.Level;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JComboBox;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

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
			generateButton.setEnabled(true);
			generateButton.requestFocusInWindow();
			generateButton.setFocusPainted(true);
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
		
		// have to set this. Something wrong with the initial use of the
		// ListSelectionListener
		generateButton.setEnabled(false);
				
		add(new JLabel("Base Profile:"), "split 2");
		loadList(prefPulldown);
		prefPulldown.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent arg0) {
				String value = (String)prefPulldown.getSelectedItem().toString();
			}
		});
		add(prefPulldown, "wrap, growx");

		for (SkeinforgePreference preference: parentGenerator.preferences) {
			add(preference.getUI(), "wrap");
		}

		add(generateButton, "tag ok, split 2");
		add(cancelButton, "tag cancel");
		generateButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				if(!parentGenerator.runSanityChecks()) {
					return;
				}
				
				int idx = prefPulldown.getSelectedIndex();
				Profile p = getListedProfile(idx);
				Base.preferences.put("lastGeneratorProfileSelected",p.toString());
				parentGenerator.configSuccess = true;
				parentGenerator.profile = p.getFullPath();
				setVisible(false);
				SkeinforgeGenerator.setSelectedProfile(p.toString());
			}
		});
		cancelButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				parentGenerator.configSuccess = false;
				setVisible(false);
			}
		});
/*
 * This is being removed because the nulling of profiles and 
 * parentGenerator is being moved to setVisible()		
		addWindowListener( new WindowAdapter() {
			@Override
			public void windowClosed(WindowEvent e) {
				profiles = null;
				parentGenerator = null;
				super.windowClosed(e);
			}
		});
*/
	}
};
