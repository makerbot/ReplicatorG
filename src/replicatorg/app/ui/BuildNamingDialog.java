package replicatorg.app.ui;

import java.awt.Container;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JTextField;

import net.miginfocom.swing.MigLayout;

public class BuildNamingDialog extends JDialog {

	private JButton okButton;
	private JButton cancelButton;
	private String path = null;
	private JTextField text;
	
	/// Returns null if no path is selected or the cancel button was clicked.
	public String getPath() { return path; }

	public BuildNamingDialog(Frame parent, String sourceName) {
		super(parent,"Name the captured build",true);
		Container c = getContentPane();
		c.setLayout(new MigLayout("fill"));
		c.add(new JLabel("Name the uploaded filename (example:'teapot.s3g')"),"wrap");
		okButton = new JButton("OK");
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
		// truncate at 8 chars
		if (sourceName.length() > 8) {
			sourceName = sourceName.substring(0, 8);
		}
		sourceName = sourceName + ".s3g";
		text = new JTextField(sourceName,12);
//		text.getDocument().addDocumentListener(new DocumentListener() {
//			public void changedUpdate(DocumentEvent arg0) {
//			}
//
//			public void insertUpdate(DocumentEvent arg0) {
//			}
//
//			public void removeUpdate(DocumentEvent arg0) {
//			}
//		});
		c.add(text,"wrap");
		c.add(cancelButton, "tag cancel");
		c.add(okButton,"tag ok");
		pack();
		setLocationRelativeTo(parent);
	}
	
	private void doOk() {
		path = text.getText();
		dispose();
	}
	
	private void doCancel() {
		path = null;
		dispose();
	}

}
