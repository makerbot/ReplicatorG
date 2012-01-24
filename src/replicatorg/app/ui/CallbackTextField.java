package replicatorg.app.ui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.Format;

import replicatorg.app.Base;
import replicatorg.app.ui.controlpanel.ExtruderPanel;

public class CallbackTextField extends ActionTextField {
	ActionListener panel;
	String actionCommand;
	
	public CallbackTextField(ActionListener panel, String actionCommand, String name, int columns, Format format) {
		super(null, columns, format);	
//		setMaximumSize(new Dimension(textBoxWidth, 25));
//		setMinimumSize(new Dimension(textBoxWidth, 25));
//		setPreferredSize(new Dimension(textBoxWidth, 25));
		setName(name);
		this.panel = panel;
		this.actionCommand = actionCommand;
	}

	@Override
	public void doSaveEvent() {
		panel.actionPerformed(new ActionEvent(this, 0, actionCommand));
	}

}
