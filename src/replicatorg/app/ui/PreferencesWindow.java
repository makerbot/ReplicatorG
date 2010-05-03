/**
 * 
 */
package replicatorg.app.ui;

import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.AbstractAction;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.KeyStroke;

import net.miginfocom.swing.MigLayout;
import replicatorg.app.Base;
import replicatorg.app.Base.InitialOpenBehavior;
import replicatorg.uploader.FirmwareUploader;

/**
 * Edit the major preference settings.
 * @author phooky
 *
 */
public class PreferencesWindow extends JFrame implements GuiConstants {
	// gui elements

	// the calling editor, so updates can be applied
	MainWindow editor;

	JTextField fontSizeField;
	JTextField firmwareUpdateUrlField;
	
	private void showCurrentSettings() {		
		Font editorFont = Base.getFontPref("editor.font","Monospaced,plain,12");
		fontSizeField.setText(String.valueOf(editorFont.getSize()));
		String firmwareUrl = Base.preferences.get("replicatorg.updates.url", FirmwareUploader.DEFAULT_UPDATES_URL);
		firmwareUpdateUrlField.setText(firmwareUrl);
	}
	
	private JCheckBox addCheckboxForPref(Container c, String text, final String pref, boolean defaultVal) {
		JCheckBox cb = new JCheckBox(text);
		cb.setSelected(Base.preferences.getBoolean(pref,defaultVal));
		c.add(cb,"wrap");
		cb.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				JCheckBox box = (JCheckBox)e.getSource();
				Base.preferences.putBoolean(pref,box.isSelected());
			}
		});
		return cb;
	}

	private void addInitialFilePrefs(Container c) {
		final String prefName = "replicatorg.initialopenbehavior";
		int defaultBehavior = InitialOpenBehavior.OPEN_LAST.ordinal();
		int ordinal = Base.preferences.getInt(prefName, defaultBehavior);
		if (ordinal >= InitialOpenBehavior.values().length) {
			ordinal = defaultBehavior;
		}
		final InitialOpenBehavior openBehavior = InitialOpenBehavior.values()[ordinal];
		ButtonGroup bg = new ButtonGroup();
		class RadioAction extends AbstractAction {
			private InitialOpenBehavior behavior;
		    public RadioAction(String text, InitialOpenBehavior behavior) {
		    	super(text);
		    	this.behavior = behavior;
		    	if (behavior == openBehavior) {
		    		putValue(SELECTED_KEY, new Boolean(true));
		    	}
		    }
		    public void actionPerformed(ActionEvent e) {
		    	Base.preferences.putInt(prefName,behavior.ordinal());
		    }
		}
		c.add(new JLabel("On ReplicatorG launch:"),"wrap");
		JRadioButton b;
		b = new JRadioButton(new RadioAction("Open last opened or save file",InitialOpenBehavior.OPEN_LAST));
		bg.add(b);
		c.add(b,"wrap");
		b = new JRadioButton(new RadioAction("Open new file",InitialOpenBehavior.OPEN_NEW));
		bg.add(b);
		c.add(b,"wrap");
	}
	
	public PreferencesWindow() {
		super("Preferences");
		setResizable(false);

		JComponent content = new JPanel(new MigLayout());

		// MainWindow font size [ ]

		content.add(new JLabel("MainWindow font size: "), "split");
		fontSizeField = new JTextField(4);
		content.add(fontSizeField);
		content.add(new JLabel("  (requires restart of ReplicatorG)"), "wrap");

		addCheckboxForPref(content,"Monitor temperature during builds","build.monitor_temp",false);
		addCheckboxForPref(content,"Honor serial port selection in machines.xml","serial.use_machines",true);
		addCheckboxForPref(content,"Show experimental machine profiles","machine.showExperimental",false);
		addCheckboxForPref(content,"Show simulator during builds","build.showSimulator",true);

		content.add(new JLabel("Firmware update URL: "),"split");
		firmwareUpdateUrlField = new JTextField(34);
		content.add(firmwareUpdateUrlField,"wrap");

		addInitialFilePrefs(content);

		JButton delPrefs = new JButton("Restore all defaults (includes driver choice, etc.)");
		content.add(delPrefs,"wrap");
		delPrefs.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				Base.resetPreferences();
				showCurrentSettings();
			}
			
			
		});

		// [ OK ] [ Cancel ] maybe these should be next to the message?

		JButton button;
		
		button = new JButton("Close");
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				applyFrame();
				dispose();
			}
		});
		content.add(button, "tag ok");

		showCurrentSettings();

		// closing the window is same as hitting cancel button

		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				dispose();
			}
		});

		ActionListener disposer = new ActionListener() {
			public void actionPerformed(ActionEvent actionEvent) {
				dispose();
			}
		};
		Base.registerWindowCloseKeys(getRootPane(), disposer);

		add(content);
		pack();
		Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
		setLocation((screen.width - getWidth()) / 2,
				(screen.height - getHeight()) / 2);

		// handle window closing commands for ctrl/cmd-W or hitting ESC.

		content.addKeyListener(new KeyAdapter() {
			public void keyPressed(KeyEvent e) {
				KeyStroke wc = MainWindow.WINDOW_CLOSE_KEYSTROKE;
				if ((e.getKeyCode() == KeyEvent.VK_ESCAPE)
						|| (KeyStroke.getKeyStrokeForEvent(e).equals(wc))) {
					dispose();
				}
			}
		});
	}

	/**
	 * Change internal settings based on what was chosen in the prefs, then send
	 * a message to the editor saying that it's time to do the same.
	 */
	public void applyFrame() {
		// put each of the settings into the table
		String newSizeText = fontSizeField.getText();
		try {
			int newSize = Integer.parseInt(newSizeText.trim());
			String fontName = Base.preferences.get("editor.font","Monospaced,plain,12");
			if (fontName != null) {
				String pieces[] = fontName.split(",");
				pieces[2] = String.valueOf(newSize);
				StringBuffer buf = new StringBuffer();
				for (String piece : pieces) {
					if (buf.length() > 0) buf.append(",");
					buf.append(piece);
				}
				Base.preferences.put("editor.font", buf.toString());
			}

		} catch (Exception e) {
			Base.logger.warning("ignoring invalid font size " + newSizeText);
		}
		Base.preferences.put("replicatorg.updates.url",firmwareUpdateUrlField.getText());
		editor.applyPreferences();
	}

	public void showFrame(MainWindow editor) {
		this.editor = editor;

		// set all settings entry boxes to their actual status
		setVisible(true);
	}

}
