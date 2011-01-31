package replicatorg.plugin.toolpath.skeinforge;

import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.lang.ref.WeakReference;
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
	private WeakReference<SkeinforgeGenerator> parentGenerator;
	private List<Profile> profiles = null; // NB: must explicitly deallocate at close time
	
	private void loadList(JList list) {
		list.removeAll();
		profiles = parentGenerator.get().getProfiles();
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

	final JList prefList = new JList();

	private Profile getListedProfile(int idx) {
		return profiles.get(idx);
	}

	public ConfigurationDialog(final Frame parent, final SkeinforgeGenerator parentGeneratorIn) {
		super(parent, true);
		parentGenerator = new WeakReference<SkeinforgeGenerator>(parentGeneratorIn);
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
		            Profile p = getListedProfile(idx);
					Base.preferences.put("lastGeneratorProfileSelected",p.toString());
					parentGenerator.get().configSuccess = true;
					parentGenerator.get().profile = p.getFullPath();
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
					Profile p = getListedProfile(idx);
					Base.preferences.put("lastGeneratorProfileSelected",p.toString());
					parentGenerator.get().configSuccess = true;
					parentGenerator.get().profile = p.getFullPath();
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
					Profile p = getListedProfile(idx);
					parentGenerator.get().editProfile(p);
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
					Profile p = getListedProfile(idx);
					parentGenerator.get().duplicateProfile(p, newName);
					loadList(prefList);
					pack();
				}
			}
		});

		add(deleteButton, "wrap,growx");
		deleteButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				int idx = prefList.getSelectedIndex();
				Profile p = getListedProfile(idx);
				boolean result = new ProfileUtils().delete(p);
				loadList(prefList);
				pack();
				Base.logger.log(Level.INFO, "Profile " + p.getFullPath()
						+ " deleted: " + result);
			}
		});

		for (SkeinforgePreference preference: parentGenerator.get().preferences) {
			add(preference.getUI(), "wrap");
		}

		add(generate, "tag ok");
		JButton cancel = new JButton("Cancel");
		add(cancel, "tag cancel");
		generate.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				int idx = prefList.getSelectedIndex();
				Profile p = getListedProfile(idx);
				Base.preferences.put("lastGeneratorProfileSelected",p.toString());
				parentGenerator.get().configSuccess = true;
				parentGenerator.get().profile = p.getFullPath();
				setVisible(false);
				SkeinforgeGenerator.setSelectedProfile(p.toString());
			}
		});
		cancel.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				parentGenerator.get().configSuccess = false;
				setVisible(false);
			}
		});
		
		addWindowListener( new WindowAdapter() {
			@Override
			public void windowClosed(WindowEvent e) {
				profiles = null;
				super.windowClosed(e);
			}
		});
	}
};
