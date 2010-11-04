package replicatorg.app.ui;

import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JDialog;

import replicatorg.drivers.Driver;

/**
 * The toolhead indexer tool allows the user to explicitly set the index of an attached tool. 
 * @author phooky
 *
 */
public class ToolheadIndexer extends JDialog implements ActionListener {

	final static String instructions = "Instructions";
	public ToolheadIndexer(Frame parent, Driver d) {
		super(parent,"Set toolhead index",true);
		
	}
	public void actionPerformed(ActionEvent actionEvent) {
	}

}
