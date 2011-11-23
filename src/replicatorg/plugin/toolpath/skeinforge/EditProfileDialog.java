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
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import net.miginfocom.swing.MigLayout;
import replicatorg.app.Base;
import replicatorg.plugin.toolpath.skeinforge.SkeinforgeGenerator.Profile;
import replicatorg.plugin.toolpath.skeinforge.SkeinforgeGenerator.SkeinforgePreference;

class EditProfileDialog extends JDialog {
	final boolean postProcessToolheadIndex = true;
	final String manageStr = "Manage profiles...";
	final String profilePref = "replicatorg.skeinforge.profilePref";
	JButton editButton = new JButton("Edit...");
	JButton duplicateButton = new JButton("Duplicate...");
	JButton locateButton = new JButton("Locate...");
	JButton deleteButton = new JButton("Delete");
	
	JButton doneButton = new JButton("Done");
	
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
	JPanel buttonPanel = new JPanel();
	
	private void loadList(JList list) {
		list.removeAll();
		profiles = parentGenerator.getProfiles();
		DefaultListModel model = new DefaultListModel();
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
		list.setModel(model);
		list.clearSelection();
		if(foundLastProfile != -1) {
			list.setSelectedIndex(foundLastProfile);	
			doneButton.setEnabled(true);
			doneButton.requestFocusInWindow();
			doneButton.setFocusPainted(true);
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

	final JList prefList = new JList();

	private Profile getListedProfile(int idx) {
		return profiles.get(idx);
	}

	public EditProfileDialog(final Frame parent, final SkeinforgeGenerator parentGeneratorIn) {
		super(parent, true);
		parentGenerator = parentGeneratorIn;
		setTitle("Edit Profiles");
		setLayout(new MigLayout("aligny, top, ins 5, fill"));

		editButton.setToolTipText("Click to edit this profile's properties.");
		deleteButton.setToolTipText("Click to remove this profile. Note that this can not be undone.");
		locateButton.setToolTipText("Click to find the folder for this profile, e.g. to make backups or to share your settings.");
		duplicateButton.setToolTipText("This will make a copy of the currently selected profile, with a new name that you provide.");
		
		// have to set this. Something wrong with the initial use of the
		// ListSelectionListener
		doneButton.setEnabled(false);
				
		editButton.setEnabled(false);
		locateButton.setEnabled(false);
		deleteButton.setEnabled(false);
		duplicateButton.setEnabled(false);

		profilePanel.setLayout(new MigLayout("top, ins 0, fillx"));
		profilePanel.add(new JLabel("Select a Skeinforge profile:"), "wrap");

		prefList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		prefList.addListSelectionListener(new ListSelectionListener() {

			public void valueChanged(ListSelectionEvent selectionEvent) {
				boolean selected = !((JList) selectionEvent.getSource())
						.isSelectionEmpty();
				doneButton.setEnabled(selected);
				editButton.setEnabled(selected);
				locateButton.setEnabled(selected);
				deleteButton.setEnabled(selected);
				duplicateButton.setEnabled(selected);
			}
		});
		
		// Add a listener for mouse clicks
		prefList.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent evt) {
		        JList list = (JList)evt.getSource();
		        if (evt.getClickCount() == 2) { // Double-click generates with this profile
		            int idx = list.locationToIndex(evt.getPoint());
		            Profile p = getListedProfile(idx);
					Base.preferences.put("lastGeneratorProfileSelected",p.toString());
					parentGenerator.configSuccess = true;
					parentGenerator.profile = p.getFullPath();
					setVisible(false);
		        }
		    }
		});
		loadList(prefList);
		profilePanel.add(new JScrollPane(prefList), "growx, growy");

		prefList.addKeyListener( new KeyAdapter() {
			public void keyPressed ( KeyEvent e ) {
				Base.logger.fine("key pressed event: "+e);
				if(e.getKeyCode() == KeyEvent.VK_ENTER)
				{
					int idx = prefList.getSelectedIndex();
					Base.logger.fine("idx="+idx);
					Profile p = getListedProfile(idx);
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
		profilePanel.add(editButton, "split,flowy,growx");
		editButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				int idx = prefList.getSelectedIndex();
				if (idx == -1) {
					JOptionPane.showMessageDialog(parent,
							"Select a profile to edit.");
				} else {
					Profile p = getListedProfile(idx);
					parentGenerator.editProfile(p);
				}
			}
		});

		profilePanel.add(duplicateButton, "growx,flowy");
		duplicateButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				int idx = prefList.getSelectedIndex();
				String newName = JOptionPane.showInputDialog(parent,
						"Name your new profile:");
				if (newName != null) {
					Profile p = getListedProfile(idx);
					Profile newp = parentGenerator.duplicateProfile(p, newName);
					loadList(prefList);
					// Select new profile
					if (newp != null) prefList.setSelectedValue(newp.toString(), true);
					pack();
				}
			}
		});
		
		profilePanel.add(locateButton, "split,flowy,growx");
		locateButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				int idx = prefList.getSelectedIndex();
				if (idx == -1) {
				} else {
					Profile p = getListedProfile(idx);
					boolean result = new ProfileUtils().openFolder(p);
					Base.logger.log(Level.FINEST,
							"Opening directory for profile: "+ result);
				}
			}
		});


		profilePanel.add(deleteButton, "wrap,growx");
		deleteButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				int idx = prefList.getSelectedIndex();
				Profile p = getListedProfile(idx);
				
				int userResponse = JOptionPane.showConfirmDialog(null,
						"Are you sure you want to delete profile " 
						+ p.toString() + "? This cannot be undone.",
						"Delete Profile", JOptionPane.YES_NO_OPTION);
				if (userResponse == JOptionPane.YES_OPTION) {

					boolean result = new ProfileUtils().delete(p);
					loadList(prefList);
					pack();
					Base.logger.log(Level.INFO, "Profile " + p.getFullPath()
							+ " deleted: " + result);
				}
			}
		});
		
		add(profilePanel, "wrap, growx");

		buttonPanel.setLayout(new MigLayout("aligny, top, ins 0"));
		
		add(doneButton, "tag ok, split 2");
		doneButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				parentGenerator.configSuccess = false;
				setVisible(false);
			}
		});
	}
};
