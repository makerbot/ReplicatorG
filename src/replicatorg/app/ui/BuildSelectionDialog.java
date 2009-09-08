package replicatorg.app.ui;

import java.awt.Container;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import net.miginfocom.swing.MigLayout;

public class BuildSelectionDialog extends JDialog {

	private JButton okButton;
	private JButton cancelButton;
	private String selectedPath = null;
	
	/// Returns null if no path is selected or the cancel button was clicked.
	public String getSelectedPath() { return selectedPath; }

	public BuildSelectionDialog(Frame parent, List<String> paths) {
		super(parent,"Select a file to build",true);
		Container c = getContentPane();
		c.setLayout(new MigLayout("fill"));
		c.add(new JLabel("Select the .s3g file to build:"),"wrap");
		okButton = new JButton("OK");
		okButton.setEnabled(false);
		okButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				doOk();
			}
		});
		cancelButton = new JButton("Cancel");
		cancelButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				doCancel();
			}
		});
		final JList list = new JList(paths.toArray());
		list.addListSelectionListener(new ListSelectionListener() {
			public void valueChanged(ListSelectionEvent lse) {
				if (list.getSelectedIndex() != -1) {
					selectedPath = (String)list.getModel().getElementAt(list.getSelectedIndex());
					okButton.setEnabled(selectedPath != null);
				}
			}
		});
		c.add(list,"wrap");
		c.add(cancelButton, "tag cancel");
		c.add(okButton,"tag ok");
		pack();
		setLocationRelativeTo(parent);
	}
	
	private void doOk() {
		dispose();
	}
	
	private void doCancel() {
		selectedPath = null;
		dispose();
	}
}
