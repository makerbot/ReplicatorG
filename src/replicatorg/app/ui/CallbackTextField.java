package replicatorg.app.ui;

import java.awt.Dimension;
import java.awt.event.ActionEvent;

import replicatorg.app.ui.controlpanel.ExtruderPanel;

public class CallbackTextField extends ActionTextField {
	ExtruderPanel panel;
	String actionCommand;
	
	public CallbackTextField(ExtruderPanel panel, String actionCommand, String name, int columns) {
		super("", columns);
		
//		setMaximumSize(new Dimension(textBoxWidth, 25));
//		setMinimumSize(new Dimension(textBoxWidth, 25));
//		setPreferredSize(new Dimension(textBoxWidth, 25));
		setName(name);
		this.panel = panel;
		this.actionCommand = actionCommand;
	}

	@Override
	public void doSaveEvent() {
		// TODO Auto-generated method stub
		panel.actionPerformed(new ActionEvent(this, 0, actionCommand));
	}

}
