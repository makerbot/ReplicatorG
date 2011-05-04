package replicatorg.app.ui;

import java.awt.Color;
import java.awt.Toolkit;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import javax.swing.JTextField;

import replicatorg.app.Base;

// Text field that keeps track of whether it's data has been modified, and calls a function
// when it loses focus or gets an ENTER key to allow the subclasser to handle the event.
public abstract class ActionTextField extends JTextField {
	Color defaultColor;
	Color modifiedColor;
	
	boolean valueModified;
	String originalValue;
	
	private class StoringFocusListener implements FocusListener {
		final ActionTextField textField;
		
		public StoringFocusListener(ActionTextField textField) {
			this.textField = textField;
		}
		
		@Override
		public void focusGained(FocusEvent arg0) {
			// TODO Auto-generated method stub
			Base.logger.fine("Got focus!");
		}

		@Override
		public void focusLost(FocusEvent arg0) {
			// TODO Auto-generated method stub
			Base.logger.fine("Lost focus!");
			textField.notifyDoneModifying();
		}
	}
	
	private class NotifyingKeyListener implements KeyListener {
		final ActionTextField textField;
		
		public NotifyingKeyListener(ActionTextField textField) {
			this.textField = textField;
		}
		
		@Override
		public void keyPressed(KeyEvent arg0) {
		}

		@Override
		public void keyReleased(KeyEvent arg0) {
		}

		@Override
		public void keyTyped(KeyEvent arg0) {
			if (arg0.getKeyChar() == KeyEvent.VK_ENTER) {
				Base.logger.fine("save key, time to save!");
				textField.notifyDoneModifying();
			}
			else if (arg0.getKeyChar() == KeyEvent.VK_ESCAPE) {
				Base.logger.fine("escape key, abort changes!");
				textField.notifyRestoreOriginalValue();
			}
			else {
				Base.logger.fine("new key, exciting");
				textField.notifyValueModified();
			}
		}
	}
	
	public void notifyRestoreOriginalValue() {
		if (valueModified) {
			Base.logger.fine("Resetting to orignal value");
			
			valueModified = false;
			setText(originalValue);
			originalValue = null;
			setBackground(defaultColor);
		}
	}
	
	public void notifyValueModified() {
		if (!valueModified) {
			Base.logger.fine("New data, needs to be saved");
			
			valueModified = true;
			originalValue = getText();
			setBackground(modifiedColor);
		}
	}
	
	public void notifyDoneModifying() {
		if (valueModified) {
			Base.logger.fine("New data being saved");
			
			valueModified = false;
			originalValue = null;
			setBackground(defaultColor);
			
			doSaveEvent();
		}
	}
	
	public abstract void doSaveEvent();
	
	public ActionTextField(String text, int columns) {
		super(text, columns);
		
		Toolkit tk = Toolkit.getDefaultToolkit();
		defaultColor = (Color)tk.getDesktopProperty("text");
		modifiedColor = new Color(128, 128, 255);
		
		valueModified = false;
		
		addFocusListener(new StoringFocusListener(this));
		
		addKeyListener(new NotifyingKeyListener(this));
	}
	
}
