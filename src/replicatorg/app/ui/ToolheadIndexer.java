package replicatorg.app.ui;

import java.awt.Container;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.NumberFormat;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;

import net.miginfocom.swing.MigLayout;
import replicatorg.app.Base;
import replicatorg.drivers.Driver;
import replicatorg.drivers.MultiTool;

/**
 * The toolhead indexer tool allows the user to explicitly set the index of an attached tool. 
 * @author phooky
 *
 */
public class ToolheadIndexer extends JDialog {

	final static String instructions = "<html>" +
			"Set the Toolhead Index of *all attached extruders*.\n"+  
			"If you have multiple extruder boards attached, this will case an error.\n" +
			"See documentaiton at: http://www.makerbot.com/docs/dualstrusion for details. </html>";
	public ToolheadIndexer(Frame parent, final Driver d) {
		super(parent,"Set Toolhead Index",true);
		Container c = getContentPane();
		c.setLayout(new MigLayout("fillx,pack pref pref"));
		c.add(new JLabel(instructions),"wrap,wmax 500px");
		c.add(new JLabel("Tool index:"),"split");
		NumberFormat.getNumberInstance();

		final JFormattedTextField toolIndexField = new JFormattedTextField(NumberFormat.getNumberInstance());
		toolIndexField.setColumns(4);
		toolIndexField.setValue(new Integer(0));
		c.add(toolIndexField);
		JButton cancelButton = new JButton("Cancel");
		cancelButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				setVisible(false);
			}
		});
		JButton indexButton = new JButton("Set Index");
		indexButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				int value = ((Number)toolIndexField.getValue()).intValue();
				Base.logger.info("Setting toolhead index to "+Integer.toString(value));
				((MultiTool)d).setConnectedToolIndex(value);
				setVisible(false);
			}
		});
		c.add(indexButton);
		c.add(cancelButton);
		pack();
	}

}
