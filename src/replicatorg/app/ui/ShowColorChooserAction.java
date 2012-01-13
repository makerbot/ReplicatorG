package replicatorg.app.ui;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.AbstractAction;
import javax.swing.JColorChooser;
import javax.swing.JDialog;
import javax.swing.JFrame;

public class ShowColorChooserAction extends AbstractAction {
    JColorChooser chooser;
    JDialog dialog;
    

    public ShowColorChooserAction(JFrame frame, JColorChooser chooser, ActionListener okListener, ActionListener cancelListener, Color inColor) {
        super(buttonStringFromColor(inColor) );
        this.chooser = chooser;

        // Choose whether dialog is modal or modeless
        boolean modal = true;

        // Create the dialog that contains the chooser
        dialog = JColorChooser.createDialog(frame, "Choose an LED Strip color", modal,
            chooser, okListener, cancelListener);
    }

    public void actionPerformed(ActionEvent evt) {
        // Show dialog
        dialog.setVisible(true);
        // Disable the action; to enable the action when the dialog is closed, see
        // Listening for OK and Cancel Events in a JColorChooser Dialog
        setEnabled(true);
    }

    public static String buttonStringFromColor(Color c)
	{
		String baseString = "LED String: ";
		if(c == Color.BLACK) {
			return (baseString + "Off");
		}
		return( baseString + c.toString());
	}
}