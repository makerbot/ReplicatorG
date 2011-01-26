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

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import net.miginfocom.swing.MigLayout;
import replicatorg.app.Base;
import replicatorg.plugin.toolpath.skeinforge.SkeinforgeGenerator.Profile;
import replicatorg.plugin.toolpath.skeinforge.SkeinforgeGenerator.SkeinforgePreference;

class ConfigurationDialog extends JDialog {
	final String manageStr = "Manage profiles...";
	final String profilePref = "replicatorg.skeinforge.profilePref";
	JButton editButton = new JButton("Edit...");
	JButton newButton = new JButton("Duplicate...");
	JButton deleteButton = new JButton("Delete");
	JButton generate = new JButton("Generate...");
	private SkeinforgeGenerator parentGenerator;
	
	private void loadList(JList list) {
		list.removeAll();
		List<Profile> profiles = parentGenerator.getProfiles();
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

	/**
	 * Help reduce effects of miserable memory leak.
	 * http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6497929
	 */
	@Override
	public void setVisible(boolean b) {
		super.setVisible(b);
		parentGenerator = null;
	}
	
	public ConfigurationDialog(final Frame parent, final SkeinforgeGenerator parentGenerator) {
		super(parent, true);
		this.parentGenerator = parentGenerator;
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
					parentGenerator.configSuccess = true;
					parentGenerator.profile = p.getFullPath();
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
					parentGenerator.configSuccess = true;
					parentGenerator.profile = p.getFullPath();
					setVisible(false);
				} else if(e.getKeyCode() == KeyEvent.VK_ESCAPE) {
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
					parentGenerator.editProfile(p);
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
					Profile p = (Profile) prefList.getModel().getElementAt(
							idx);
					parentGenerator.duplicateProfile(p, newName);
					loadList(prefList);
					pack();
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
				pack();
				Base.logger.log(Level.INFO, "Profile " + p.getFullPath()
						+ " deleted: " + result);
			}
		});

		for (SkeinforgePreference preference: parentGenerator.preferences) {
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
				parentGenerator.configSuccess = true;
				parentGenerator.profile = p.getFullPath();
				setVisible(false);
				SkeinforgeGenerator.setSelectedProfile(p.toString());
			}
		});
		cancel.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				parentGenerator.configSuccess = false;
				setVisible(false);
			}
		});
	}

};
