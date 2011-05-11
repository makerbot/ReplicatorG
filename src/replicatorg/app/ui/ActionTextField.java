package replicatorg.app.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import javax.swing.JTextField;


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
		}

		@Override
		public void focusLost(FocusEvent arg0) {
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
				textField.notifyDoneModifying();
			}
			else if (arg0.getKeyChar() == KeyEvent.VK_ESCAPE) {
				textField.notifyRestoreOriginalValue();
			}
			else {
				textField.notifyValueModified();
			}
		}
	}
	
	public void notifyRestoreOriginalValue() {
		if (valueModified) {
			valueModified = false;
			setText(originalValue);
			originalValue = null;
			setBackground(defaultColor);
		}
	}
	
	public void notifyValueModified() {
		if (!valueModified) {
			valueModified = true;
			originalValue = getText();
			setBackground(modifiedColor);
		}
	}
	
	public void notifyDoneModifying() {
		if (valueModified) {
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
		defaultColor = this.getBackground();
		modifiedColor = new Color(128, 128, 255);
		
		valueModified = false;
		
		addFocusListener(new StoringFocusListener(this));
		
		addKeyListener(new NotifyingKeyListener(this));
	}
	
}
